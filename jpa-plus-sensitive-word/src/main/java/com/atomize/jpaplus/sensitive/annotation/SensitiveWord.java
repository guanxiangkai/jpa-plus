package com.atomize.jpaplus.sensitive.annotation;

import java.lang.annotation.*;

/**
 * 敏感词检测注解
 *
 * <p>标注在实体字段上，保存前自动检测敏感词。
 * 根据 {@link #strategy()} 决定是拒绝保存还是替换后保存。</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SensitiveWord {

    /**
     * 处理策略（默认拒绝）
     */
    SensitiveWordStrategy strategy() default SensitiveWordStrategy.REJECT;

    /**
     * 替换字符（仅 REPLACE 策略生效）
     */
    String replacement() default "***";
}

