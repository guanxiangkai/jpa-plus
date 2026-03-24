package com.atomize.jpaplus.desensitize.annotation;

import java.lang.annotation.*;

/**
 * 字段脱敏注解
 *
 * <p>标注在实体字段上，查询后自动对敏感数据进行掩码处理。
 * 仅支持 {@link String} 类型字段。</p>
 *
 * <p><b>设计模式：</b>标记注解模式 + 策略模式（通过 {@link #strategy()} 选择脱敏算法）</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Desensitize {

    /**
     * 脱敏策略
     */
    DesensitizeStrategy strategy();

    /**
     * 掩码字符（默认 *）
     */
    char maskChar() default '*';
}

