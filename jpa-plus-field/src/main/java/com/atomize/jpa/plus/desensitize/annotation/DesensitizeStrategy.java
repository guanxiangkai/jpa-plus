package com.atomize.jpa.plus.desensitize.annotation;

/**
 * 脱敏策略枚举
 *
 * <p>定义常见的数据脱敏规则，每种策略封装自己的掩码算法。
 * 通过枚举多态替代 switch 分派，符合开闭原则（新增策略只需添加枚举常量）。</p>
 *
 * <p><b>设计模式：</b>策略模式（Strategy） —— 每个枚举值封装一种脱敏算法</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
public enum DesensitizeStrategy {

    /**
     * 手机号脱敏：138****1234
     */
    PHONE {
        @Override
        public String mask(String value, char c) {
            if (value.length() < 7) return value;
            return value.substring(0, 3) + repeat(c, 4) + value.substring(value.length() - 4);
        }
    },

    /**
     * 邮箱脱敏：a***@example.com
     */
    EMAIL {
        @Override
        public String mask(String value, char c) {
            int at = value.indexOf('@');
            if (at <= 1) return value;
            return value.charAt(0) + repeat(c, at - 1) + value.substring(at);
        }
    },

    /**
     * 身份证脱敏：110***********1234
     */
    ID_CARD {
        @Override
        public String mask(String value, char c) {
            if (value.length() < 8) return value;
            return value.substring(0, 3) + repeat(c, value.length() - 7) + value.substring(value.length() - 4);
        }
    },

    /**
     * 姓名脱敏：张**
     */
    NAME {
        @Override
        public String mask(String value, char c) {
            if (value.length() <= 1) return value;
            return value.charAt(0) + repeat(c, value.length() - 1);
        }
    },

    /**
     * 银行卡脱敏：6222 **** **** 1234
     */
    BANK_CARD {
        @Override
        public String mask(String value, char c) {
            if (value.length() < 8) return value;
            return value.substring(0, 4) + " " + repeat(c, 4) + " " +
                    repeat(c, 4) + " " + value.substring(value.length() - 4);
        }
    },

    /**
     * 地址脱敏：北京市海淀区******
     */
    ADDRESS {
        @Override
        public String mask(String value, char c) {
            if (value.length() <= 6) return value;
            return value.substring(0, 6) + repeat(c, value.length() - 6);
        }
    },

    /**
     * 自定义脱敏
     */
    CUSTOM {
        @Override
        public String mask(String value, char c) {
            if (value.length() <= 2) return value;
            return value.charAt(0) + repeat(c, value.length() - 2) + value.charAt(value.length() - 1);
        }
    };

    /**
     * 生成重复字符的字符串
     */
    private static String repeat(char c, int count) {
        return String.valueOf(c).repeat(count);
    }

    /**
     * 执行掩码处理
     *
     * @param value    原始值
     * @param maskChar 掩码字符
     * @return 脱敏后的值
     */
    public abstract String mask(String value, char maskChar);
}

