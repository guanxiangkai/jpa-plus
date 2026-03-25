package com.atomize.jpa.plus.sensitive.annotation;

/**
 * 敏感词处理策略枚举
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
public enum SensitiveWordStrategy {
    /**
     * 拒绝：包含敏感词时抛出异常，阻止保存
     */
    REJECT,
    /**
     * 替换：将敏感词替换为指定字符后保存
     */
    REPLACE
}

