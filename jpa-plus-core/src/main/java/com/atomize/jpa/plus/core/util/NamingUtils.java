package com.atomize.jpa.plus.core.util;

/**
 * 命名转换工具类
 *
 * <p>提供驼峰命名和蛇形命名之间的相互转换。</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
public final class NamingUtils {

    private NamingUtils() {
    }

    /**
     * 驼峰命名转蛇形命名
     *
     * @param camelCase 驼峰命名字符串
     * @return 蛇形命名字符串
     */
    public static String camelToSnake(String camelCase) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) result.append('_');
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * 蛇形命名转驼峰命名
     *
     * @param snake 蛇形命名字符串
     * @return 驼峰命名字符串
     */
    public static String snakeToCamel(String snake) {
        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;
        for (char c : snake.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                result.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            }
        }
        return result.toString();
    }
}

