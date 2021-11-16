package com.blinkfox.pool.kit;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.helpers.MessageFormatter;

/**
 * 字符串工具类.
 *
 * @author blinkfox on 2021-11-16.
 * @since 1.0.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StringKit {

    /**
     * 判断给定的字符串是否是空字符串.
     *
     * @param str 待判断的字符串
     * @return 布尔值
     */
    public static boolean isBlank(CharSequence str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }

        for (int i = 0; i < strLen; ++i) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断给定的字符串是否不是空字符串.
     *
     * @param str 待判断的字符串
     * @return 布尔值
     */
    public static boolean isNotBlank(CharSequence str) {
        return !isBlank(str);
    }

    /**
     * 使用  Slf4j 的方式格式化字符串.
     *
     * @param pattern 字符串模式
     * @param args 不定长参数值
     * @return 格式化后的字符串
     */
    public static String format(String pattern, Object... args) {
        return args == null || args.length == 0 ? pattern : MessageFormatter.arrayFormat(pattern, args).getMessage();
    }

}
