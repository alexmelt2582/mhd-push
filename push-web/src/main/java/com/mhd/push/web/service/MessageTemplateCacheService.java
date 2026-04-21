package com.mhd.push.web.service;

import cn.hutool.core.util.ReflectUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mhd.push.common.constant.CommonConstant;
import com.mhd.push.common.constant.RedisConstant;
import com.mhd.push.common.dto.model.ContentModel;
import com.mhd.push.common.enums.ChannelType;
import com.mhd.push.support.domain.entity.MessageTemplate;
import com.mhd.push.support.mapper.MessageTemplateMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 模板两级缓存：
 * 1. 本地缓存保存“模板 + 解析结果 + 反射元数据”，降低高频组装开销。
 * 2. Redis 保存模板原文与版本，降低多实例下的数据库访问压力。
 * 3. 读路径优先走本地快照，定期只校验版本，不重复解析模板结构。
 */
@Service
public class MessageTemplateCacheService {
    private static final Map<Integer, Class<? extends ContentModel>> CHANNEL_MODEL_CLASS_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<? extends ContentModel>, Field[]> CONTENT_FIELD_CACHE = new ConcurrentHashMap<>();

    private final Cache<Long, LocalTemplateSnapshot> localCache;

    @Resource
    private MessageTemplateMapper messageTemplateMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final long localVersionCheckMillis;
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
        this.localVersionCheckMillis = TimeUnit.SECONDS.toMillis(versionCheckSeconds);
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
     *
     * 快照里不仅有模板实体，还包含：
     * 1. 已解析的模板 JSON。
     * 2. 对应的 ContentModel 类型。
     * 3. 已缓存的字段反射元数据。
     */
    public MessageTemplateSnapshot getTemplateSnapshot(Long templateId) {
        if (templateId == null) {
            return null;
        }

        LocalTemplateSnapshot localSnapshot = localCache.getIfPresent(templateId);
        if (isLocalSnapshotUsable(templateId, localSnapshot)) {
            return localSnapshot.toSnapshot();
        }

        MessageTemplate template = loadTemplate(templateId);
        if (template == null) {
            localCache.invalidate(templateId);
            return null;
        }

        LocalTemplateSnapshot refreshedSnapshot = LocalTemplateSnapshot.from(template);
        localCache.put(templateId, refreshedSnapshot);
        return refreshedSnapshot.toSnapshot();
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
     * 判断本地快照是否仍然可用。
     */
    private boolean isLocalSnapshotUsable(Long templateId, LocalTemplateSnapshot localSnapshot) {
        if (localSnapshot == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - localSnapshot.getLastVersionCheckAt() < localVersionCheckMillis) {
            return true;
        }

        String remoteVersion = stringRedisTemplate.opsForValue().get(RedisConstant.buildMessageTemplateVersionKey(templateId));
        if (!StringUtils.hasText(remoteVersion)) {
            localCache.invalidate(templateId);
            return false;
        }
        if (Objects.equals(remoteVersion, localSnapshot.getVersion())) {
            localSnapshot.touchVersionCheck(now);
            return true;
        }
        localCache.invalidate(templateId);
        return false;
    }

    /**
     * 加载模板。
     *
     * 顺序为：Redis -> MySQL。
     */
    private MessageTemplate loadTemplate(Long templateId) {
        MessageTemplate template = getTemplateFromRedis(templateId);
        if (template != null) {
            return template;
        }

        template = messageTemplateMapper.selectById(templateId);
        if (template != null && !Objects.equals(template.getIsDeleted(), CommonConstant.TRUE)) {
            writeRedisCache(template);
        }
        return template;
    }

    /**
     * 从 Redis 读取模板原文。
     */
    private MessageTemplate getTemplateFromRedis(Long templateId) {
        String cachedTemplate = stringRedisTemplate.opsForValue().get(RedisConstant.buildMessageTemplateCacheKey(templateId));
        if (!StringUtils.hasText(cachedTemplate)) {
            return null;
        }
        return JSON.parseObject(cachedTemplate, MessageTemplate.class);
    }

    /**
     * 将模板写入 Redis，并同步版本号。
     */
    private void writeRedisCache(MessageTemplate template) {
        String dataKey = RedisConstant.buildMessageTemplateCacheKey(template.getId());
        String versionKey = RedisConstant.buildMessageTemplateVersionKey(template.getId());
        String version = buildVersion(template);
        stringRedisTemplate.opsForValue().set(dataKey, JSON.toJSONString(template), redisTtlMinutes, TimeUnit.MINUTES);
        stringRedisTemplate.opsForValue().set(versionKey, version, redisTtlMinutes, TimeUnit.MINUTES);
    }

    /**
     * 生成模板版本号。
     *
     * 当前直接复用 `updated` 字段，满足“模板变更后版本变化”的要求。
     */
    private String buildVersion(MessageTemplate template) {
        return String.valueOf(Objects.requireNonNullElse(template.getUpdated(), 0));
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
    private static Field[] resolveContentFields(Class<? extends ContentModel> contentModelClass) {
        return CONTENT_FIELD_CACHE.computeIfAbsent(contentModelClass, ReflectUtil::getFields);
    }

    /**
     * 对外暴露的只读模板快照。
     */
    public static final class MessageTemplateSnapshot {
        private final MessageTemplate template;
        private final JSONObject templateContent;
        private final Class<? extends ContentModel> contentModelClass;
        private final Field[] contentFields;

        private MessageTemplateSnapshot(MessageTemplate template, JSONObject templateContent,
                                        Class<? extends ContentModel> contentModelClass, Field[] contentFields) {
            this.template = template;
            this.templateContent = templateContent;
            this.contentModelClass = contentModelClass;
            this.contentFields = contentFields;
        }

        /**
         * 返回模板实体。
         */
        public MessageTemplate getTemplate() {
            return template;
        }

        /**
         * 返回已解析的模板 JSON。
         */
        public JSONObject getTemplateContent() {
            return templateContent;
        }

        /**
         * 返回当前模板对应的 ContentModel 类型。
         */
        public Class<? extends ContentModel> getContentModelClass() {
            return contentModelClass;
        }

        /**
         * 返回 ContentModel 的字段列表。
         */
        public Field[] getContentFields() {
            return contentFields;
        }
    }

    /**
     * 本地缓存中的模板快照。
     */
    private static final class LocalTemplateSnapshot {
        private final MessageTemplateSnapshot snapshot;
        private final String version;
        private volatile long lastVersionCheckAt;

        private LocalTemplateSnapshot(MessageTemplateSnapshot snapshot, String version, long lastVersionCheckAt) {
            this.snapshot = snapshot;
            this.version = version;
            this.lastVersionCheckAt = lastVersionCheckAt;
        }

        /**
         * 根据模板实体构建本地快照。
         */
        private static LocalTemplateSnapshot from(MessageTemplate template) {
            Integer sendChannel = template.getSendChannel();
            Class<? extends ContentModel> contentModelClass = resolveContentModelClass(sendChannel);
            JSONObject templateContent = JSON.parseObject(template.getMsgContent());
            Field[] fields = resolveContentFields(contentModelClass);
            String version = String.valueOf(Objects.requireNonNullElse(template.getUpdated(), 0));
            long now = System.currentTimeMillis();
            return new LocalTemplateSnapshot(
                    new MessageTemplateSnapshot(template, templateContent, contentModelClass, fields),
                    version,
                    now
            );
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
        private long getLastVersionCheckAt() {
            return lastVersionCheckAt;
        }

        /**
         * 更新上次版本校验时间。
         */
        private void touchVersionCheck(long now) {
            this.lastVersionCheckAt = now;
        }
    }
}