package com.mhd.push.infra.service;

import cn.hutool.core.util.ReflectUtil;
import com.alibaba.fastjson2.JSON;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mhd.push.common.constant.CommonConstant;
import com.mhd.push.common.constant.RedisConstant;
import com.mhd.push.common.dto.model.ContentModel;
import com.mhd.push.common.enums.ChannelType;
import com.mhd.push.domain.model.template.TemplateContentDefinition;
import com.mhd.push.domain.model.template.TemplateVariableDefinition;
import com.mhd.push.domain.utils.TemplateContentDefinitionUtils;
import com.mhd.push.infra.persistence.entity.MessageTemplate;
import com.mhd.push.infra.persistence.mapper.MessageTemplateMapper;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 消息模板多级缓存服务
 * <p>
 * 架构策略：
 * 1. 本地缓存 (Guava)：存储完整模板数据 + 解析后的元数据，抗住高并发读取。
 * 2. Redis 缓存：存储模板原文 + 版本号，用于多实例间数据同步。
 * 3. 一致性策略：本地缓存定期（如5秒）异步/懒加载校验 Redis 版本号，避免频繁反序列化。
 * </p>
 */
@Service
@Slf4j
public class MessageTemplateCacheService {
    /**
     * 渠道模型类缓存
     */
    private static final Map<Integer, Class<? extends ContentModel>> CHANNEL_MODEL_CLASS_CACHE = new ConcurrentHashMap<>();
    /**
     * 反射字段缓存
     */
    private static final Map<Class<? extends ContentModel>, Map<String, Field>> CONTENT_FIELD_CACHE = new ConcurrentHashMap<>();
    private static final String CONTENT_FIELD_NAME = "content";
    private static final String OFFICIAL_ACCOUNT_PARAM_FIELD_NAME = "officialAccountParam";
    private static final String MINI_PROGRAM_PARAM_FIELD_NAME = "miniProgramParam";
    private final ConcurrentHashMap<Long, Object> lockPool = new ConcurrentHashMap<>();
    private final Cache<Long, LocalTemplateSnapshot> localCache;

    @Resource
    private MessageTemplateMapper messageTemplateMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 本地缓存版本检查间隔（纳秒）
     */
    private final long localVersionCheckNanos;
    private final long redisTtlMinutes;

    /**
     * 初始化模板缓存参数。
     */
    public MessageTemplateCacheService(
            @Value("${mhd.template.cache.local-max-size:1000}") long localMaxSize,
            @Value("${mhd.template.cache.local-expire-minutes:10}") long localExpireMinutes,
            @Value("${mhd.template.cache.version-check-seconds:5}") long versionCheckSeconds,
            @Value("${mhd.template.cache.redis-ttl-minutes:30}") long redisTtlMinutes) {
        this.localCache = CacheBuilder.newBuilder()
                .maximumSize(localMaxSize)
                .expireAfterAccess(localExpireMinutes, TimeUnit.MINUTES)
                .build();
        this.localVersionCheckNanos = TimeUnit.SECONDS.toNanos(versionCheckSeconds);
        this.redisTtlMinutes = redisTtlMinutes;
    }

    /**
     * 只获取模板实体本身。
     */
    public MessageTemplate getTemplate(Long templateId) {
        MessageTemplateSnapshot snapshot = getTemplateSnapshot(templateId);
        return snapshot == null ? null : snapshot.getTemplate();
    }

    /**
     * 刷新指定模板的本地缓存和 Redis 缓存。
     */
    public void refresh(MessageTemplate template) {
        if (template == null || template.getId() == null) {
            return;
        }
        if (Objects.equals(template.getIsDeleted(), CommonConstant.TRUE)) {
            evict(template.getId());
            return;
        }
        writeRedisCache(template);
        localCache.put(template.getId(), LocalTemplateSnapshot.from(template));
    }

    /**
     * 删除模板缓存。
     */
    public void evict(Long templateId) {
        if (templateId == null) {
            return;
        }
        localCache.invalidate(templateId);
        stringRedisTemplate.delete(RedisConstant.buildMessageTemplateCacheKey(templateId));
        stringRedisTemplate.delete(RedisConstant.buildMessageTemplateVersionKey(templateId));
    }

    /**
     * 获取可直接用于发送组装的有效模板快照。
     */
    public MessageTemplateSnapshot getActiveTemplateSnapshot(Long templateId) {
        MessageTemplateSnapshot snapshot = getTemplateSnapshot(templateId);
        if (snapshot == null || Objects.equals(snapshot.getTemplate().getIsDeleted(), CommonConstant.TRUE)) {
            return null;
        }
        return snapshot;
    }

    /**
     * 获取模板快照。
     * <p>
     * 快照里不仅有模板实体，还包含：
     * 1. 已解析的结构化模板定义。
     * 2. 对应的 ContentModel 类型。
     * 3. 已缓存的字段反射元数据。
     * 4. 正文参数定义、额外参数定义和正文占位符集合。
     */
    public MessageTemplateSnapshot getTemplateSnapshot(Long templateId) {
        if (templateId == null) {
            return null;
        }

        // 1. 尝试从本地缓存获取
        LocalTemplateSnapshot localSnapshot = localCache.getIfPresent(templateId);
        // 2. 校验本地缓存是否可用（包含版本比对逻辑）
        if (isLocalSnapshotUsable(templateId, localSnapshot)) {
            return localSnapshot.toSnapshot();
        }

        // 3. 本地缓存失效或未命中，回源加载
        MessageTemplate template = loadTemplate(templateId);
        if (template == null) {
            localCache.invalidate(templateId);
            return null;
        }

        // 4. 更新本地缓存
        LocalTemplateSnapshot refreshedSnapshot = LocalTemplateSnapshot.from(template);
        localCache.put(templateId, refreshedSnapshot);
        return refreshedSnapshot.toSnapshot();
    }

    /**
     * 加载模板。
     * <p>
     * 顺序为：Redis -> MySQL。
     */
    private MessageTemplate loadTemplate(Long templateId) {
        // 1. 先查 Redis 缓存数据
        MessageTemplate template = getTemplateFromRedis(templateId);
        if (template != null) {
            return template;
        }
        // 2. Redis 未命中，查数据库
        try {
            template = messageTemplateMapper.selectById(templateId);
            if (template != null && !Objects.equals(template.getIsDeleted(), CommonConstant.TRUE)) {
                writeRedisCache(template);
            }
        } catch (Exception e) {
            log.error("Database query failed for templateId: {}", templateId, e);
            // 数据库异常处理策略视业务而定，这里返回 null 让上层处理
        }
        return template;
    }

    /**
     * 从 Redis 读取模板原文。
     */
    private MessageTemplate getTemplateFromRedis(Long templateId) {
        try {
            String cachedTemplate = stringRedisTemplate.opsForValue().get(RedisConstant.buildMessageTemplateCacheKey(templateId));
            if (StringUtils.hasText(cachedTemplate)) {
                return JSON.parseObject(cachedTemplate, MessageTemplate.class);
            }
        } catch (Exception e) {
            log.warn("Failed to parse template from Redis for id: {}", templateId, e);
        }
        return null;
    }

    /**
     * 将模板写入 Redis，并同步版本号。
     */
    private void writeRedisCache(MessageTemplate template) {
        try {
            String dataKey = RedisConstant.buildMessageTemplateCacheKey(template.getId());
            String versionKey = RedisConstant.buildMessageTemplateVersionKey(template.getId());
            String version = buildVersion(template);

            // 使用管道或事务可以进一步优化，但这里保持简单
            stringRedisTemplate.opsForValue().set(dataKey, JSON.toJSONString(template), redisTtlMinutes, TimeUnit.MINUTES);
            stringRedisTemplate.opsForValue().set(versionKey, version, redisTtlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Failed to write Redis cache for templateId: {}", template.getId(), e);
        }
    }

    /**
     * 判断本地快照是否仍然可用
     * <p>
     * 优化点：
     * 1. 使用 synchronized 锁住 templateId，防止并发请求同时击穿到 Redis。
     * 2. 增加 try-catch，Redis 故障时降级使用本地缓存（牺牲一致性保可用性）。
     */
    private boolean isLocalSnapshotUsable(Long templateId, LocalTemplateSnapshot localSnapshot) {
        if (localSnapshot == null) {
            return false;
        }

        long nowNanos = System.nanoTime();

        // 快速路径：如果在信任期内，直接返回 true
        if (nowNanos - localSnapshot.getLastVersionCheckAtNanos() < localVersionCheckNanos) {
            return true;
        }

        // 慢路径：需要校验版本
        // 使用 synchronized 保证同一模板ID在同一时刻只有一个线程去查 Redis
        // 注意：这里使用 localSnapshot 作为锁对象，或者使用 templateId.intern()，防止锁竞争过大
        Object lock = lockPool.computeIfAbsent(templateId, k -> new Object());

        synchronized (lock) {
            // 双重检查：防止等待锁期间，其他线程已经更新过了
            if (nowNanos - localSnapshot.getLastVersionCheckAtNanos() >= localVersionCheckNanos) {
                try {
                    String remoteVersion = stringRedisTemplate.opsForValue().get(RedisConstant.buildMessageTemplateVersionKey(templateId));

                    // 场景1：Redis 中没有数据（可能被删除或过期）
                    if (!StringUtils.hasText(remoteVersion)) {
                        // 保守策略：认为数据已删除，清除本地缓存
                        localCache.invalidate(templateId);
                        return false;
                    }

                    // 场景2：版本一致，更新检查时间
                    if (Objects.equals(remoteVersion, localSnapshot.getVersion())) {
                        localSnapshot.touchVersionCheck(nowNanos);
                        return true;
                    }

                    // 场景3：版本不一致，清除本地缓存，触发 reload
                    localCache.invalidate(templateId);
                    return false;

                } catch (Exception e) {
                    // 【关键容错】Redis 挂了或网络超时
                    log.error("Redis connection failed during version check for templateId: {}. Keeping local cache alive.", templateId, e);

                    // 降级策略：Redis 异常时，不清除本地缓存，而是延长下一次检查的时间（例如延长 30秒）
                    // 这样可以避免 Redis 故障期间，所有请求都打到数据库
                    localSnapshot.touchVersionCheck(nowNanos + TimeUnit.SECONDS.toNanos(30));
                    return true;
                }
            }
            return true;
        }
    }

    /**
     * 对外暴露的只读模板快照。
     */
    @Getter
    public static final class MessageTemplateSnapshot {
        /**
         *  返回模板实体。
         */
        private final MessageTemplate template;
        /**
         *  返回已解析的结构化模板定义。
         */
        private final TemplateContentDefinition templateContentDefinition;
        /**
         *  返回当前模板对应的 ContentModel 类型。
         */
        private final Class<? extends ContentModel> contentModelClass;
        /**
         *  返回 ContentModel 的字段映射。
         */
        private final Map<String, Field> contentFieldMap;
        /**
         *  返回承载正文的字段名。
         */
        private final String contentCarrierFieldName;
        /**
         *  返回正文变量定义映射。
         */
        private final Map<String, TemplateVariableDefinition> contentParamDefinitionMap;
        /**
         *  返回额外参数定义映射。
         */
        private final Map<String, TemplateVariableDefinition> extraParamDefinitionMap;
        /**
         *  返回正文中实际使用到的占位符集合。
         */
        private final Set<String> contentPlaceholders;

        private MessageTemplateSnapshot(MessageTemplate template, TemplateContentDefinition templateContentDefinition,
                                        Class<? extends ContentModel> contentModelClass, Map<String, Field> contentFieldMap,
                                        String contentCarrierFieldName,
                                        Map<String, TemplateVariableDefinition> contentParamDefinitionMap,
                                        Map<String, TemplateVariableDefinition> extraParamDefinitionMap,
                                        Set<String> contentPlaceholders) {
            this.template = template;
            this.templateContentDefinition = templateContentDefinition;
            this.contentModelClass = contentModelClass;
            this.contentFieldMap = contentFieldMap;
            this.contentCarrierFieldName = contentCarrierFieldName;
            this.contentParamDefinitionMap = contentParamDefinitionMap;
            this.extraParamDefinitionMap = extraParamDefinitionMap;
            this.contentPlaceholders = contentPlaceholders;
        }
    }

    /**
     * 本地缓存中的模板快照。
     */
    private static final class LocalTemplateSnapshot {
        private final MessageTemplateSnapshot snapshot;
        private final String version;
        private volatile long lastVersionCheckAtNanos;

        private LocalTemplateSnapshot(MessageTemplateSnapshot snapshot, String version, long lastVersionCheckAtNanos) {
            this.snapshot = snapshot;
            this.version = version;
            this.lastVersionCheckAtNanos = lastVersionCheckAtNanos;
        }

        /**
         * 根据模板实体构建本地快照。
         */
        private static LocalTemplateSnapshot from(MessageTemplate template) {
            try {
                Integer sendChannel = template.getSendChannel();
                Class<? extends ContentModel> contentModelClass = resolveContentModelClass(sendChannel);
                TemplateContentDefinition templateContentDefinition = TemplateContentDefinitionUtils.parse(template.getMsgContent());
                Map<String, Field> fieldMap = resolveContentFields(contentModelClass);
                String contentCarrierFieldName = resolveContentCarrierFieldName(fieldMap);
                Map<String, TemplateVariableDefinition> contentParamDefinitionMap = TemplateContentDefinitionUtils.indexDefinitions(templateContentDefinition.getContentParamsSchema());
                Map<String, TemplateVariableDefinition> extraParamDefinitionMap = TemplateContentDefinitionUtils.indexDefinitions(templateContentDefinition.getExtraParamsSchema());
                validateTemplateDefinition(templateContentDefinition, contentModelClass, fieldMap, contentCarrierFieldName);
                Set<String> contentPlaceholders = TemplateContentDefinitionUtils.extractContentPlaceholders(templateContentDefinition);
                String version = buildVersion(template);
                long nowNanos = System.nanoTime();
                return new LocalTemplateSnapshot(new MessageTemplateSnapshot(template, templateContentDefinition, contentModelClass, fieldMap,
                        contentCarrierFieldName,
                        contentParamDefinitionMap, extraParamDefinitionMap, contentPlaceholders), version, nowNanos);
            } catch (Exception e) {
                log.error("Failed to build local template snapshot for templateId: {}", template.getId(), e);
                throw new RuntimeException("Invalid template format", e);
            }
        }

        /**
         * 返回对外可读的模板快照。
         */
        private MessageTemplateSnapshot toSnapshot() {
            return snapshot;
        }

        /**
         * 返回本地快照的版本号。
         */
        private String getVersion() {
            return version;
        }

        /**
         * 返回上次版本校验时间。
         */
        private long getLastVersionCheckAtNanos() {
            return lastVersionCheckAtNanos;
        }

        /**
         * 更新上次版本校验时间。
         */
        private void touchVersionCheck(long now) {
            this.lastVersionCheckAtNanos = now;
        }


    }

    /**
     * 根据渠道编码获取对应的 ContentModel 类型，并在进程内复用结果。
     */
    private static Class<? extends ContentModel> resolveContentModelClass(Integer sendChannel) {
        return CHANNEL_MODEL_CLASS_CACHE.computeIfAbsent(sendChannel, ChannelType::getChanelModelClassByCode);
    }

    /**
     * 获取 ContentModel 的字段元数据，并在进程内缓存。
     */
    private static Map<String, Field> resolveContentFields(Class<? extends ContentModel> contentModelClass) {
        return CONTENT_FIELD_CACHE.computeIfAbsent(contentModelClass, clazz -> {
            Map<String, Field> fieldMap = new LinkedHashMap<>();
            for (Field field : ReflectUtil.getFields(clazz)) {
                fieldMap.put(field.getName(), field);
            }
            return fieldMap;
        });
    }

    /**
     * 推断当前渠道用于承载正文的字段名。
     */
    private static String resolveContentCarrierFieldName(Map<String, Field> contentFieldMap) {
        if (contentFieldMap.containsKey(CONTENT_FIELD_NAME)) {
            return CONTENT_FIELD_NAME;
        }
        if (contentFieldMap.containsKey(OFFICIAL_ACCOUNT_PARAM_FIELD_NAME)) {
            return OFFICIAL_ACCOUNT_PARAM_FIELD_NAME;
        }
        if (contentFieldMap.containsKey(MINI_PROGRAM_PARAM_FIELD_NAME)) {
            return MINI_PROGRAM_PARAM_FIELD_NAME;
        }
        return null;
    }

    /**
     * 校验模板定义是否与对应渠道的 ContentModel 匹配。
     */
    private static void validateTemplateDefinition(TemplateContentDefinition definition,
                                                   Class<? extends ContentModel> contentModelClass,
                                                   Map<String, Field> contentFieldMap,
                                                   String contentCarrierFieldName) {
        if (StringUtils.hasText(definition.getContent()) && !StringUtils.hasText(contentCarrierFieldName)) {
            throw new IllegalArgumentException("Channel " + contentModelClass.getSimpleName() + " does not support content body field");
        }
        for (TemplateVariableDefinition definitionItem : definition.getExtraParamsSchema()) {
            if (definitionItem == null || !StringUtils.hasText(definitionItem.getKey())) {
                continue;
            }
            if (!contentFieldMap.containsKey(definitionItem.getKey())) {
                throw new IllegalArgumentException("Channel " + contentModelClass.getSimpleName() + " does not support extra field: " + definitionItem.getKey());
            }
        }
    }

    /**
     * 生成模板版本号。
     * <p>
     * 当前直接复用 `updated` 字段，满足“模板变更后版本变化”的要求。
     */
    private static String buildVersion(MessageTemplate template) {
        return String.valueOf(Objects.requireNonNullElse(template.getUpdated(), 0));
    }
}
