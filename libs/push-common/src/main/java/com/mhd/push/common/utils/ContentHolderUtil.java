package com.mhd.push.common.utils;

import org.springframework.util.PropertyPlaceholderHelper;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 内容占位符 替换， {$var}
 *
 * @author zhao-hao-dong
 */
public class ContentHolderUtil {
    /**
     * 占位符前缀
     */
    private static final String PLACE_HOLDER_PREFIX = "{$";
    /**
     * 占位符后缀
     */
    private static final String PLACE_HOLDER_SUFFIX = "}";
    private static final Pattern PLACE_HOLDER_PATTERN = Pattern.compile("\\{\\$([a-zA-Z0-9_.-]+)}");
    private static final PropertyPlaceholderHelper PROPERTY_PLACEHOLDER_HELPER = new PropertyPlaceholderHelper(PLACE_HOLDER_PREFIX, PLACE_HOLDER_SUFFIX);

    private ContentHolderUtil() {

    }

    public static String replacePlaceHolder(final String template, final Map<String, String> paramMap) {
        return PROPERTY_PLACEHOLDER_HELPER.replacePlaceholders(template, new CustomPlaceholderResolver(template, paramMap));
    }

    /**
     * 使用自定义解析策略替换模板中的占位符。
     *
     * @param template 模板原文
     * @param resolver 占位符解析器
     * @return 替换后的文本
     */
    public static String replacePlaceHolder(final String template, final PlaceholderValueResolver resolver) {
        return PROPERTY_PLACEHOLDER_HELPER.replacePlaceholders(template, resolver::resolve);
    }

    /**
     * 提取模板中使用到的全部占位符名称。
     *
     * @param template 模板原文
     * @return 占位符名称集合
     */
    public static Set<String> extractPlaceHolders(final String template) {
        if (template == null || template.isEmpty()) {
            return Collections.emptySet();
        }
        Matcher matcher = PLACE_HOLDER_PATTERN.matcher(template);
        Set<String> placeHolders = new LinkedHashSet<>();
        while (matcher.find()) {
            placeHolders.add(matcher.group(1));
        }
        return placeHolders;
    }

    /**
     * 占位符值解析器。
     */
    @FunctionalInterface
    public interface PlaceholderValueResolver {
        /**
         * 解析单个占位符的值。
         *
         * @param placeholderName 占位符名称
         * @return 占位符对应的渲染值
         */
        String resolve(String placeholderName);
    }

    private static class CustomPlaceholderResolver implements PropertyPlaceholderHelper.PlaceholderResolver {
        private final String template;
        private final Map<String, String> paramMap;

        public CustomPlaceholderResolver(String template, Map<String, String> paramMap) {
            super();
            this.template = template;
            this.paramMap = paramMap;
        }

        @Override
        public String resolvePlaceholder(String placeholderName) {
            if (Objects.isNull(paramMap)) {
                String errorStr = MessageFormat.format("template:{0} require param:{1},but not exist! paramMap:{2}", template, placeholderName, null);
                throw new IllegalArgumentException(errorStr);
            }
            String value = paramMap.get(placeholderName);
            if (Objects.isNull(value) || value.isEmpty()) {
                String errorStr = MessageFormat.format("template:{0} require param:{1},but not exist! paramMap:{2}", template, placeholderName, paramMap);
                throw new IllegalArgumentException(errorStr);
            }
            return value;
        }
    }
}
