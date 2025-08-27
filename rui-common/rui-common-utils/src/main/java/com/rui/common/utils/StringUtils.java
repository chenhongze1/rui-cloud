package com.rui.common.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 字符串工具类
 */
public class StringUtils extends org.apache.commons.lang3.StringUtils {

    private static final String NULLSTR = "";

    private static final char SEPARATOR = '_';

    /**
     * 获取参数不为空值
     */
    public static <T> T nvl(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * 判断一个Collection是否为空， 包含List，Set，Queue
     */
    public static boolean isEmpty(Collection<?> coll) {
        return isNull(coll) || coll.isEmpty();
    }

    /**
     * 判断一个Collection是否非空，包含List，Set，Queue
     */
    public static boolean isNotEmpty(Collection<?> coll) {
        return !isEmpty(coll);
    }

    /**
     * 判断一个对象数组是否为空
     */
    public static boolean isEmpty(Object[] objects) {
        return isNull(objects) || (objects.length == 0);
    }

    /**
     * 判断一个对象数组是否非空
     */
    public static boolean isNotEmpty(Object[] objects) {
        return !isEmpty(objects);
    }

    /**
     * 判断一个Map是否为空
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return isNull(map) || map.isEmpty();
    }

    /**
     * 判断一个Map是否为空
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * 判断一个字符串是否为空串
     */
    public static boolean isEmpty(String str) {
        return isNull(str) || NULLSTR.equals(str.trim());
    }

    /**
     * 判断一个字符串是否为非空串
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 判断一个对象是否为空
     */
    public static boolean isNull(Object object) {
        return object == null;
    }

    /**
     * 判断一个对象是否非空
     */
    public static boolean isNotNull(Object object) {
        return !isNull(object);
    }

    /**
     * 判断一个对象是否是数组类型（Java基本型别的数组）
     */
    public static boolean isArray(Object object) {
        return isNotNull(object) && object.getClass().isArray();
    }

    /**
     * 去空格
     */
    public static String trim(String str) {
        return (str == null ? "" : str.trim());
    }

    /**
     * 截取字符串
     */
    public static String substring(final String str, int start) {
        if (str == null) {
            return NULLSTR;
        }

        if (start < 0) {
            start = str.length() + start;
        }

        if (start < 0) {
            start = 0;
        }
        if (start > str.length()) {
            return NULLSTR;
        }

        return str.substring(start);
    }

    /**
     * 截取字符串
     */
    public static String substring(final String str, int start, int end) {
        if (str == null) {
            return NULLSTR;
        }

        if (end < 0) {
            end = str.length() + end;
        }
        if (start < 0) {
            start = str.length() + start;
        }

        if (end > str.length()) {
            end = str.length();
        }

        if (start > end) {
            return NULLSTR;
        }

        if (start < 0) {
            start = 0;
        }
        if (end < 0) {
            end = 0;
        }

        return str.substring(start, end);
    }

    /**
     * 格式化文本, {} 表示占位符<br>
     * 此方法只是简单将占位符 {} 按照顺序替换为参数<br>
     * 如果想输出 {} 使用 \\转义 { 即可，如果想输出 {} 之前的 \ 使用双转义符 \\\\ 即可<br>
     * 例：<br>
     * 通常使用：format("this is {} for {}", "a", "b") -> this is a for b<br>
     * 转义{}： format("this is \\{} for {}", "a", "b") -> this is \{} for a<br>
     * 转义\： format("this is \\\\{} for {}", "a", "b") -> this is \\a for b<br>
     */
    public static String format(String template, Object... params) {
        if (isEmpty(params) || isEmpty(template)) {
            return template;
        }
        return StrFormatter.format(template, params);
    }

    /**
     * 字符串转set
     */
    public static final Set<String> str2Set(String str, String sep) {
        return new HashSet<String>(str2List(str, sep, true, false));
    }

    /**
     * 字符串转list
     */
    public static final List<String> str2List(String str, String sep, boolean filterBlank, boolean trim) {
        List<String> list = new ArrayList<String>();
        if (StringUtils.isEmpty(str)) {
            return list;
        }

        // 过滤空白字符串
        if (filterBlank && StringUtils.isBlank(str)) {
            return list;
        }
        String[] split = str.split(sep);
        for (String string : split) {
            if (filterBlank && StringUtils.isBlank(string)) {
                continue;
            }
            if (trim) {
                string = string.trim();
            }
            list.add(string);
        }

        return list;
    }

    /**
     * 查找指定字符串是否匹配指定字符串列表中的任意一个字符串
     */
    public static boolean startsWithAny(String string, Collection<String> prefixes) {
        if (isEmpty(string) || isEmpty(prefixes)) {
            return false;
        }
        for (String prefix : prefixes) {
            if (string.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 驼峰转换
     */
    public static String toCapitalizeCamelCase(String s) {
        if (s == null) {
            return null;
        }
        s = toCamelCase(s);
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * 驼峰转换
     */
    public static String toCamelCase(String s) {
        if (s == null) {
            return null;
        }

        s = s.toLowerCase();

        StringBuilder sb = new StringBuilder(s.length());
        boolean upperCase = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == SEPARATOR) {
                upperCase = true;
            } else if (upperCase) {
                sb.append(Character.toUpperCase(c));
                upperCase = false;
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * 驼峰转下划线命名
     */
    public static String toUnderScoreCase(String s) {
        if (s == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        boolean upperCase = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            boolean nextUpperCase = true;

            if (i < (s.length() - 1)) {
                nextUpperCase = Character.isUpperCase(s.charAt(i + 1));
            }

            if ((i > 0) && Character.isUpperCase(c)) {
                if (!upperCase || !nextUpperCase) {
                    sb.append(SEPARATOR);
                }
                upperCase = true;
            } else {
                upperCase = false;
            }

            sb.append(Character.toLowerCase(c));
        }

        return sb.toString();
    }

    /**
     * 是否包含字符串
     */
    public static boolean containsAnyIgnoreCase(CharSequence cs, CharSequence... searchCharSequences) {
        if (isEmpty(cs) || isEmpty(searchCharSequences)) {
            return false;
        }
        for (CharSequence testStr : searchCharSequences) {
            if (containsIgnoreCase(cs, testStr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 驼峰转kebab命名
     */
    public static String toKebabCase(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('-');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * 查找指定字符串是否匹配指定字符串列表中的任意一个字符串
     */
    public static boolean matches(String str, List<String> strs) {
        if (isEmpty(str) || isEmpty(strs)) {
            return false;
        }
        for (String pattern : strs) {
            if (isMatch(pattern, str)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断url是否与规则配置:
     * ? 表示单个字符;
     * * 表示一层路径内的任意字符串，不可跨层级;
     * ** 表示任意层路径;
     */
    public static boolean isMatch(String pattern, String url) {
        AntPathMatcher matcher = new AntPathMatcher();
        return matcher.match(pattern, url);
    }

    @SuppressWarnings("unchecked")
    public static <T> T cast(Object obj) {
        return (T) obj;
    }

    /**
     * 数字左边补齐0，使之达到指定长度。注意，如果数字转换为字符串后，长度大于size，则只保留 最后size个字符。
     */
    public static final String padl(final Number num, final int size) {
        return padl(num.toString(), size, '0');
    }

    /**
     * 字符串左补齐。如果原始字符串s长度大于size，则只保留最后size个字符。
     */
    public static final String padl(final String s, final int size, final char c) {
        final StringBuilder sb = new StringBuilder(size);
        if (s != null) {
            final int len = s.length();
            if (s.length() <= size) {
                for (int i = size - len; i > 0; i--) {
                    sb.append(c);
                }
                sb.append(s);
            } else {
                return s.substring(len - size, len);
            }
        } else {
            for (int i = size; i > 0; i--) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 格式化器
     */
    public static class StrFormatter {
        public static final String EMPTY_JSON = "{}";
        public static final char C_BACKSLASH = '\\';
        public static final char C_DELIM_START = '{';
        public static final char C_DELIM_END = '}';

        /**
         * 格式化字符串<br>
         * 此方法只是简单将占位符 {} 按照顺序替换为参数<br>
         * 如果想输出 {} 使用 \\转义 { 即可，如果想输出 {} 之前的 \ 使用双转义符 \\\\ 即可<br>
         * 例：<br>
         * 通常使用：format("this is {} for {}", "a", "b") -> this is a for b<br>
         * 转义{}： format("this is \\{} for {}", "a", "b") -> this is \{} for a<br>
         * 转义\： format("this is \\\\{} for {}", "a", "b") -> this is \\a for b<br>
         */
        public static String format(final String strPattern, final Object... argArray) {
            if (strPattern == null || argArray == null) {
                return strPattern;
            }
            final int strPatternLength = strPattern.length();

            // 初始化定义好的长度以获得更好的性能
            StringBuilder sbuf = new StringBuilder(strPatternLength + 50);

            int handledPosition = 0;
            int delimIndex;// 占位符所在位置
            for (int argIndex = 0; argIndex < argArray.length; argIndex++) {
                delimIndex = strPattern.indexOf(EMPTY_JSON, handledPosition);
                if (delimIndex == -1) {
                    if (handledPosition == 0) {
                        return strPattern;
                    } else { // 字符串模板剩余部分不再包含占位符，加入剩余部分后跳出循环
                        sbuf.append(strPattern, handledPosition, strPatternLength);
                        return sbuf.toString();
                    }
                }

                // 转义符
                if (delimIndex > 0 && strPattern.charAt(delimIndex - 1) == C_BACKSLASH) {
                    if (delimIndex > 1 && strPattern.charAt(delimIndex - 2) == C_BACKSLASH) {
                        // 转义符之前还有一个转义符，占位符依旧有效
                        sbuf.append(strPattern, handledPosition, delimIndex - 1);
                        sbuf.append(utf8Str(argArray[argIndex]));
                        handledPosition = delimIndex + 2;
                    } else {
                        // 占位符被转义
                        argIndex--;
                        sbuf.append(strPattern, handledPosition, delimIndex - 1);
                        sbuf.append(C_DELIM_START);
                        handledPosition = delimIndex + 1;
                    }
                } else {
                    // 正常占位符
                    sbuf.append(strPattern, handledPosition, delimIndex);
                    sbuf.append(utf8Str(argArray[argIndex]));
                    handledPosition = delimIndex + 2;
                }
            }
            // 加入最后一个占位符后所有的字符
            sbuf.append(strPattern, handledPosition, strPattern.length());

            return sbuf.toString();
        }

        /**
         * 将对象转为字符串<br>
         */
        public static String utf8Str(Object obj) {
            return str(obj, java.nio.charset.StandardCharsets.UTF_8);
        }

        /**
         * 将对象转为字符串
         */
        public static String str(Object obj, java.nio.charset.Charset charset) {
            if (null == obj) {
                return null;
            }

            if (obj instanceof String) {
                return (String) obj;
            } else if (obj instanceof byte[]) {
                return str((byte[]) obj, charset);
            } else if (obj instanceof Byte[]) {
                return str((Byte[]) obj, charset);
            } else if (obj instanceof java.nio.ByteBuffer) {
                return str((java.nio.ByteBuffer) obj, charset);
            }
            return obj.toString();
        }

        /**
         * 解码字节码
         */
        public static String str(byte[] data, java.nio.charset.Charset charset) {
            if (data == null) {
                return null;
            }

            if (null == charset) {
                return new String(data);
            }
            return new String(data, charset);
        }

        /**
         * 解码字节码
         */
        public static String str(Byte[] data, java.nio.charset.Charset charset) {
            if (data == null) {
                return null;
            }

            byte[] bytes = new byte[data.length];
            Byte dataByte;
            for (int i = 0; i < data.length; i++) {
                dataByte = data[i];
                bytes[i] = (null == dataByte) ? -1 : dataByte.byteValue();
            }

            return str(bytes, charset);
        }

        /**
         * 将编码的byteBuffer数据转换为字符串
         */
        public static String str(java.nio.ByteBuffer data, java.nio.charset.Charset charset) {
            if (null == charset) {
                charset = java.nio.charset.Charset.defaultCharset();
            }
            return charset.decode(data).toString();
        }
    }

    /**
     * Ant路径匹配器
     */
    public static class AntPathMatcher {
        public static final String DEFAULT_PATH_SEPARATOR = "/";
        private static final int CACHE_TURNOFF_THRESHOLD = 65536;
        private static final char[] WILDCARD_CHARS = { '*', '?', '{' };
        private String pathSeparator;
        private boolean caseSensitive = true;
        private boolean trimTokens = false;
        private volatile Boolean cachePatterns;
        private final Map<String, String[]> tokenizedPatternCache = new ConcurrentHashMap<String, String[]>(256);
        final Map<String, AntPathStringMatcher> stringMatcherCache = new ConcurrentHashMap<String, AntPathStringMatcher>(256);

        public AntPathMatcher() {
            this.pathSeparator = DEFAULT_PATH_SEPARATOR;
        }

        public AntPathMatcher(String pathSeparator) {
            this.pathSeparator = pathSeparator;
        }

        public boolean match(String pattern, String path) {
            return doMatch(pattern, path, true, null);
        }

        protected boolean doMatch(String pattern, String path, boolean fullMatch, Map<String, String> uriTemplateVariables) {
            if (path.startsWith(this.pathSeparator) != pattern.startsWith(this.pathSeparator)) {
                return false;
            }

            String[] pattDirs = tokenizePattern(pattern);
            String[] pathDirs = tokenizePath(path);

            int pattIdxStart = 0;
            int pattIdxEnd = pattDirs.length - 1;
            int pathIdxStart = 0;
            int pathIdxEnd = pathDirs.length - 1;

            // Match all elements up to the first **
            while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
                String pattDir = pattDirs[pattIdxStart];
                if ("**".equals(pattDir)) {
                    break;
                }
                if (!matchStrings(pattDir, pathDirs[pathIdxStart], uriTemplateVariables)) {
                    return false;
                }
                pattIdxStart++;
                pathIdxStart++;
            }

            if (pathIdxStart > pathIdxEnd) {
                // Path is exhausted, only match if rest of pattern is * or **'s
                if (pattIdxStart > pattIdxEnd) {
                    return (pattern.endsWith(this.pathSeparator) ? path.endsWith(this.pathSeparator) :
                            !path.endsWith(this.pathSeparator));
                }
                if (!fullMatch) {
                    return true;
                }
                if (pattIdxStart == pattIdxEnd && pattDirs[pattIdxStart].equals("*") && path.endsWith(this.pathSeparator)) {
                    return true;
                }
                for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
                    if (!pattDirs[i].equals("**")) {
                        return false;
                    }
                }
                return true;
            } else if (pattIdxStart > pattIdxEnd) {
                // String not exhausted, but pattern is. Failure.
                return false;
            } else if (!fullMatch && "**".equals(pattDirs[pattIdxStart])) {
                // Path start definitely matches due to "**" part in pattern.
                return true;
            }

            // up to last '**'
            while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
                String pattDir = pattDirs[pattIdxEnd];
                if (pattDir.equals("**")) {
                    break;
                }
                if (!matchStrings(pattDir, pathDirs[pathIdxEnd], uriTemplateVariables)) {
                    return false;
                }
                pattIdxEnd--;
                pathIdxEnd--;
            }
            if (pathIdxStart > pathIdxEnd) {
                // String is exhausted
                for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
                    if (!pattDirs[i].equals("**")) {
                        return false;
                    }
                }
                return true;
            }

            while (pattIdxStart != pattIdxEnd && pathIdxStart <= pathIdxEnd) {
                int patIdxTmp = -1;
                for (int i = pattIdxStart + 1; i <= pattIdxEnd; i++) {
                    if (pattDirs[i].equals("**")) {
                        patIdxTmp = i;
                        break;
                    }
                }
                if (patIdxTmp == pattIdxStart + 1) {
                    // '**/**' situation, so skip one
                    pattIdxStart++;
                    continue;
                }
                // Find the pattern between padIdxStart & padIdxTmp in str between
                // strIdxStart & strIdxEnd
                int patLength = (patIdxTmp - pattIdxStart - 1);
                int strLength = (pathIdxEnd - pathIdxStart + 1);
                int foundIdx = -1;

                strLoop:
                for (int i = 0; i <= strLength - patLength; i++) {
                    for (int j = 0; j < patLength; j++) {
                        String subPat = pattDirs[pattIdxStart + j + 1];
                        String subStr = pathDirs[pathIdxStart + i + j];
                        if (!matchStrings(subPat, subStr, uriTemplateVariables)) {
                            continue strLoop;
                        }
                    }
                    foundIdx = pathIdxStart + i;
                    break;
                }

                if (foundIdx == -1) {
                    return false;
                }

                pattIdxStart = patIdxTmp;
                pathIdxStart = foundIdx + patLength;
            }

            for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
                if (!pattDirs[i].equals("**")) {
                    return false;
                }
            }

            return true;
        }

        protected String[] tokenizePattern(String pattern) {
            String[] tokenized = null;
            Boolean cachePatterns = this.cachePatterns;
            if (cachePatterns == null || cachePatterns.booleanValue()) {
                tokenized = this.tokenizedPatternCache.get(pattern);
            }
            if (tokenized == null) {
                tokenized = tokenizePath(pattern);
                if (cachePatterns == null && this.tokenizedPatternCache.size() >= CACHE_TURNOFF_THRESHOLD) {
                    // Try to adapt to the runtime situation that we're encountering:
                    // There are obviously too many different patterns coming in here...
                    // So let's turn off the cache since the patterns are unlikely to be reoccurring.
                    deactivatePatternCache();
                    return tokenized;
                }
                if (cachePatterns == null || cachePatterns.booleanValue()) {
                    this.tokenizedPatternCache.put(pattern, tokenized);
                }
            }
            return tokenized;
        }

        protected String[] tokenizePath(String path) {
            return tokenizeToStringArray(path, this.pathSeparator, this.trimTokens, true);
        }

        private boolean matchStrings(String pattern, String str, Map<String, String> uriTemplateVariables) {
            return getStringMatcher(pattern).matchStrings(str, uriTemplateVariables);
        }

        protected AntPathStringMatcher getStringMatcher(String pattern) {
            AntPathStringMatcher matcher = null;
            Boolean cachePatterns = this.cachePatterns;
            if (cachePatterns == null || cachePatterns.booleanValue()) {
                matcher = this.stringMatcherCache.get(pattern);
            }
            if (matcher == null) {
                matcher = new AntPathStringMatcher(pattern, this.caseSensitive);
                if (cachePatterns == null && this.stringMatcherCache.size() >= CACHE_TURNOFF_THRESHOLD) {
                    // Try to adapt to the runtime situation that we're encountering:
                    // There are obviously too many different patterns coming in here...
                    // So let's turn off the cache since the patterns are unlikely to be reoccurring.
                    deactivatePatternCache();
                    return matcher;
                }
                if (cachePatterns == null || cachePatterns.booleanValue()) {
                    this.stringMatcherCache.put(pattern, matcher);
                }
            }
            return matcher;
        }

        private void deactivatePatternCache() {
            this.cachePatterns = false;
            this.tokenizedPatternCache.clear();
            this.stringMatcherCache.clear();
        }

        protected static class AntPathStringMatcher {
            private static final Pattern GLOB_PATTERN = Pattern.compile("\\?|\\*|\\{((?:\\{[^/]+?\\}|[^/{}]|\\\\[{}])+?)\\}");
            private static final String DEFAULT_VARIABLE_PATTERN = "([^/]*)";
            private final Pattern pattern;
            private final List<String> variableNames;

            public AntPathStringMatcher(String pattern, boolean caseSensitive) {
                StringBuilder patternBuilder = new StringBuilder();
                java.util.regex.Matcher matcher = GLOB_PATTERN.matcher(pattern);
                int end = 0;
                this.variableNames = new ArrayList<String>();
                while (matcher.find()) {
                    patternBuilder.append(quote(pattern, end, matcher.start()));
                    String match = matcher.group();
                    if ("?".equals(match)) {
                        patternBuilder.append(".");
                    } else if ("*".equals(match)) {
                        patternBuilder.append(".*");
                    } else if (match.startsWith("{") && match.endsWith("}")) {
                        int colonIdx = match.indexOf(':');
                        if (colonIdx == -1) {
                            patternBuilder.append(DEFAULT_VARIABLE_PATTERN);
                            this.variableNames.add(matcher.group(1));
                        } else {
                            String variablePattern = match.substring(colonIdx + 1, match.length() - 1);
                            patternBuilder.append('(');
                            patternBuilder.append(variablePattern);
                            patternBuilder.append(')');
                            String variableName = match.substring(1, colonIdx);
                            this.variableNames.add(variableName);
                        }
                    }
                    end = matcher.end();
                }
                patternBuilder.append(quote(pattern, end, pattern.length()));
                this.pattern = (caseSensitive ? Pattern.compile(patternBuilder.toString()) :
                        Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE));
            }

            private String quote(String s, int start, int end) {
                if (start == end) {
                    return "";
                }
                return Pattern.quote(s.substring(start, end));
            }

            public boolean matchStrings(String str, Map<String, String> uriTemplateVariables) {
                java.util.regex.Matcher matcher = this.pattern.matcher(str);
                if (matcher.matches()) {
                    if (uriTemplateVariables != null) {
                        // SPR-8455
                        if (this.variableNames.size() != matcher.groupCount()) {
                            throw new IllegalArgumentException("The number of capturing groups in the pattern segment " +
                                    this.pattern + " does not match the number of URI template variables it defines, " +
                                    "which can occur if capturing groups are used in a URI template regex. " +
                                    "Use non-capturing groups instead.");
                        }
                        for (int i = 1; i <= matcher.groupCount(); i++) {
                            String name = this.variableNames.get(i - 1);
                            String value = matcher.group(i);
                            uriTemplateVariables.put(name, value);
                        }
                    }
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    public static String[] tokenizeToStringArray(
            String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens) {

        if (str == null) {
            return new String[0];
        }

        StringTokenizer st = new StringTokenizer(str, delimiters);
        List<String> tokens = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (trimTokens) {
                token = token.trim();
            }
            if (!ignoreEmptyTokens || token.length() > 0) {
                tokens.add(token);
            }
        }
        return toStringArray(tokens);
    }

    public static String[] toStringArray(Collection<String> collection) {
        return collection.toArray(new String[0]);
    }

    public static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
        if (index + substring.length() > str.length()) {
            return false;
        }
        for (int i = 0; i < substring.length(); i++) {
            if (str.charAt(index + i) != substring.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasText(CharSequence str) {
        return (str != null && str.length() > 0 && containsText(str));
    }

    public static boolean hasText(String str) {
        return (str != null && !str.isEmpty() && containsText(str));
    }

    private static boolean hasLength(CharSequence str) {
        return (str != null && str.length() > 0);
    }

    private static boolean containsText(CharSequence str) {
        int strLen = str.length();
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static boolean inStringIgnoreCase(String str, String... strs) {
        if (str != null && strs != null) {
            for (String s : strs) {
                if (str.equalsIgnoreCase(trim(s))) {
                    return true;
                }
            }
        }
        return false;
    }
}