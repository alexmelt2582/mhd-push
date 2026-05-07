package com.mhd.push.engine.config;

import cn.hutool.crypto.digest.DigestUtil;
import com.mhd.push.common.constant.CommonConstant;
import com.mhd.push.common.constant.RedisConstant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 敏感词配置
 *
 * @author zhao-hao-dong
 */
@Slf4j
@Configuration
public class SensitiveWordsConfig {
    /**
     * 文件前缀
     */
    private static final String FILE_PREFIX = "file:";
    /**
     * 更新时间
     */
    private static final long UPDATE_TIME_SECONDS = 10 * 60;
    private static final long DICT_KEY_TTL_HOURS = 24;
    private final AtomicReference<SensitiveWordSnapshot> localSnapshot = new AtomicReference<>(SensitiveWordSnapshot.empty());
    /**
     * 是否开启敏感词过滤
     */
    @Value("${mhd.senswords.filter.enabled}")
    private boolean filterEnabled;
    /**
     * 字典路径
     */
    @Value("${mhd.senswords.dict.path}")
    private String dictPath;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Resource
    private TaskExecutor taskExecutor;
    @Resource
    private ResourceLoader resourceLoader;
    /**
     * 是否终止线程
     */
    private volatile boolean stop = false;

    /**
     * 初始化敏感词字典
     */
    @PostConstruct
    public void loadSensitiveWords() {
        // 不开启过滤，直接返回
        if (!filterEnabled) {
            log.info("SensitiveWordConfig#loadSensitiveWords filterEnabled is false, return.");
            return;
        }
        refreshFromSource();
        // 定时更新
        taskExecutor.execute(this::startScheduledUpdate);
    }

    /**
     * 从词典源重新加载，并在内容变化时发布新快照。
     */
    private void refreshFromSource() {
        Set<String> latestWords = loadSensWords();
        if (CollectionUtils.isEmpty(latestWords)) {
            log.warn("SensitiveWordConfig#refreshFromSource skip publish because dictionary is empty.");
            return;
        }
        publishSnapshot(latestWords);
    }

    /**
     * 加载敏感词字典
     */
    private Set<String> loadSensWords() {
        if (ObjectUtils.isEmpty(dictPath)) {
            log.error("SensitiveWordConfig#loadSensWords dictPath is null or empty, skipping load.");
            return Collections.emptySet();
        }
        // 为直接路径，添加前缀
        org.springframework.core.io.Resource resource = resourceLoader.getResource(dictPath.startsWith(CommonConstant.SLASH) ? FILE_PREFIX + dictPath : dictPath);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            log.error("SensitiveWordConfig#loadSensitiveWords Failed to load sensitive words from {}: {}",
                    dictPath, e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * 发布最新词典快照。
     * <p>
     * 版本号只由词典内容决定：
     * 1. 内容没变时不创建新版本，避免产生无意义的 version key。
     * 2. 内容没变但 Redis 中的版本 key 已过期时，会重新回填并续期。
     */
    private void publishSnapshot(Set<String> latestWords) {
        String version = buildVersion(latestWords);
        String versionedDictKey = RedisConstant.buildSensitiveWordDictKey(version);
        boolean dictKeyExists = Boolean.TRUE.equals(redisTemplate.hasKey(versionedDictKey));
        SensitiveWordSnapshot currentSnapshot = localSnapshot.get();

        if (version.equals(currentSnapshot.getVersion()) && dictKeyExists) {
            refreshSnapshotTtl(versionedDictKey, version);
            return;
        }

        redisTemplate.delete(versionedDictKey);
        redisTemplate.opsForSet().add(versionedDictKey, latestWords.toArray(new String[0]));
        refreshSnapshotTtl(versionedDictKey, version);
        localSnapshot.set(SensitiveWordSnapshot.of(version, latestWords));
        log.info("SensitiveWordConfig#publishSnapshot published sensitive words version:{}, count:{}", version, latestWords.size());
    }

    /**
     * 使用当前快照过滤文本内容。
     */
    public String filter(String content) {
        SensitiveWordSnapshot snapshot = getSnapshot();
        if (!snapshot.hasWords() || !StringUtils.hasText(content)) {
            return content;
        }
        return filter(content, snapshot.getRoot());
    }

    /**
     * 获取当前可用的敏感词快照。
     */
    public SensitiveWordSnapshot getSnapshot() {
        if (!filterEnabled) {
            return SensitiveWordSnapshot.empty();
        }
        SensitiveWordSnapshot currentLocalSnapshot = localSnapshot.get();
        String currentVersion = redisTemplate.opsForValue().get(RedisConstant.SENSITIVE_WORD_DICT_CURRENT_VERSION);
        if (!StringUtils.hasText(currentVersion) || currentVersion.equals(currentLocalSnapshot.getVersion())) {
            return currentLocalSnapshot;
        }

        Set<String> latestWords = Optional.ofNullable(redisTemplate.opsForSet().members(RedisConstant.buildSensitiveWordDictKey(currentVersion)))
                .orElse(Collections.emptySet());
        if (CollectionUtils.isEmpty(latestWords)) {
            log.warn("SensitiveWordConfig#getSnapshot current version:{} exists but dictionary is empty, keep last snapshot version:{}",
                    currentVersion, currentLocalSnapshot.getVersion());
            return currentLocalSnapshot;
        }

        SensitiveWordSnapshot refreshedSnapshot = SensitiveWordSnapshot.of(currentVersion, latestWords);
        localSnapshot.set(refreshedSnapshot);
        return refreshedSnapshot;
    }

    /**
     * 使用 Trie 对文本做敏感词替换。
     */
    private String filter(String content, TrieNode root) {
        StringBuilder result = new StringBuilder();
        int contentLength = content.length();
        int currentIndex = 0;

        while (currentIndex < contentLength) {
            TrieNode node = root;
            int scanIndex = currentIndex;
            int lastMatchEnd = -1;

            while (scanIndex < contentLength && node != null) {
                node = node.children.get(content.charAt(scanIndex));
                if (node != null && node.isEnd) {
                    lastMatchEnd = scanIndex;
                }
                scanIndex++;
            }

            if (lastMatchEnd != -1) {
                result.append("*".repeat(Math.max(0, lastMatchEnd - currentIndex + 1)));
                currentIndex = lastMatchEnd + 1;
                continue;
            }

            result.append(content.charAt(currentIndex));
            currentIndex++;
        }
        return result.toString();
    }

    /**
     * 生成稳定版本号。
     * <p>
     * 相同词典内容得到相同版本号，避免“内容没变却产生新版本”。
     */
    private String buildVersion(Set<String> latestWords) {
        String raw = latestWords.stream()
                .filter(StringUtils::hasText)
                .sorted()
                .collect(Collectors.joining("|"));
        return DigestUtil.md5Hex(raw);
    }

    /**
     * 构建 Trie 树，供敏感词匹配复用。
     */
    private static TrieNode buildTrie(Set<String> words) {
        TrieNode root = new TrieNode();
        for (String word : words) {
            TrieNode node = root;
            for (char c : word.toCharArray()) {
                node = node.children.computeIfAbsent(c, key -> new TrieNode());
            }
            node.isEnd = true;
        }
        return root;
    }

    /**
     * 刷新当前版本快照的 Redis TTL 和版本指针。
     */
    private void refreshSnapshotTtl(String versionedDictKey, String version) {
        redisTemplate.expire(versionedDictKey, DICT_KEY_TTL_HOURS, TimeUnit.HOURS);
        redisTemplate.opsForValue().set(RedisConstant.SENSITIVE_WORD_DICT_CURRENT_VERSION, version);
    }

    /**
     * 实现热更新，修改词典后自动加载
     */
    private void startScheduledUpdate() {
        while (!stop) {
            try {
                TimeUnit.SECONDS.sleep(UPDATE_TIME_SECONDS);
                log.debug("SensitiveWordConfig#startScheduledUpdate start update...");
                refreshFromSource();
            } catch (InterruptedException e) {
                log.error("SensitiveWordConfig#startScheduledUpdate interrupted: {}", e.getMessage());
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * onDestroy
     */
    @PreDestroy
    public void onDestroy() {
        stop = true;
        if (taskExecutor instanceof ThreadPoolTaskExecutor threadPoolTaskExecutor) {
            threadPoolTaskExecutor.shutdown();
        }
    }

    public static final class SensitiveWordSnapshot {
        private static final SensitiveWordSnapshot EMPTY = new SensitiveWordSnapshot("", new TrieNode(), false);

        private final String version;
        private final TrieNode root;
        private final boolean hasWords;

        private SensitiveWordSnapshot(String version, TrieNode root, boolean hasWords) {
            this.version = version;
            this.root = root;
            this.hasWords = hasWords;
        }

        /**
         * 返回空快照，表示当前没有可用词典。
         */
        public static SensitiveWordSnapshot empty() {
            return EMPTY;
        }

        /**
         * 根据词典内容构建一个新的只读快照。
         */
        public static SensitiveWordSnapshot of(String version, Set<String> words) {
            return new SensitiveWordSnapshot(version, buildTrie(words), !CollectionUtils.isEmpty(words));
        }

        /**
         * 返回快照版本号。
         */
        public String getVersion() {
            return version;
        }

        /**
         * 返回用于匹配的 Trie 根节点。
         */
        public TrieNode getRoot() {
            return root;
        }

        /**
         * 判断当前快照是否包含有效词典。
         */
        public boolean hasWords() {
            return hasWords;
        }
    }

    private static final class TrieNode {
        private final Map<Character, TrieNode> children = new HashMap<>();
        private boolean isEnd;
    }
}
