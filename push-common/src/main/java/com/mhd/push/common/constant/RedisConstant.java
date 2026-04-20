package com.mhd.push.common.constant;

/**
 * @author zhao-hao-dong
 */
public interface RedisConstant {
    String GLOBAL_PREFIX = "mhd:push:";

    /**
     * 敏感词
     */
    String SENSITIVE_WORD_DICT = GLOBAL_PREFIX + "sensitive_word_dict";
    String SENSITIVE_WORD_DICT_CURRENT_VERSION = GLOBAL_PREFIX + "sensitive_word_dict:current_version";
    String SENSITIVE_WORD_DICT_VERSION_PREFIX = GLOBAL_PREFIX + "sensitive_word_dict:version:";

    /**
     * 模板缓存
     */
    String MESSAGE_TEMPLATE_CACHE_KEY_PREFIX = GLOBAL_PREFIX + "message_template:cache:";
    String MESSAGE_TEMPLATE_CACHE_VERSION_KEY_PREFIX = GLOBAL_PREFIX + "message_template:version:";

    /**
     * 夜间屏蔽（次日九点发送）Key
     */
    String NIGHT_SHIELD_BUT_NEXT_DAY_SEND_KEY = GLOBAL_PREFIX + "night_shield_send";



    static String buildSensitiveWordDictKey(String version) {
        return SENSITIVE_WORD_DICT_VERSION_PREFIX + version;
    }

    static String buildMessageTemplateCacheKey(Long templateId) {
        return MESSAGE_TEMPLATE_CACHE_KEY_PREFIX + templateId;
    }

    static String buildMessageTemplateVersionKey(Long templateId) {
        return MESSAGE_TEMPLATE_CACHE_VERSION_KEY_PREFIX + templateId;
    }

    /**
     * 内容去重Key前缀
     */
    String DEDUPLICATION_CONTENT_KEY_PREFIX = GLOBAL_PREFIX + "deduplication:content:";

    /**
     * 构建完整的内容去重 Key
     * @param key 去重 Key
     * @return 完整的内容去重 Key
     */
    static String buildDeduplicationOfContentKey(String key) {
        return DEDUPLICATION_CONTENT_KEY_PREFIX + key;
    }

    /**
     * 频次去重Key前缀
     */
    String DEDUPLICATION_FREQUENCY_KEY_PREFIX = GLOBAL_PREFIX + "deduplication:frequency:";

    /**
     * 构建完整的频次去重 Key
     * @param key 去重 Key
     * @return 完整的频次去重 Key
     */
    static String buildDeduplicationOfFrequencyKey(String key) {
        return DEDUPLICATION_FREQUENCY_KEY_PREFIX + key;
    }
}
