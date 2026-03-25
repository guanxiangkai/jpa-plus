package com.atomize.jpa.plus.logicdelete.annotation;

import java.lang.annotation.*;

/**
 * 逻辑删除注解
 *
 * <p>标注在逻辑删除标识字段上，框架会：
 * <ul>
 *   <li>查询时自动追加 {@code deleted = defaultValue} 条件</li>
 *   <li>删除时改写为 UPDATE SET {@code deleted = value}</li>
 * </ul>
 * </p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogicDelete {

    /**
     * 已删除值（默认 "1"）
     */
    String value() default "1";

    /**
     * 未删除值（默认 "0"）
     */
    String defaultValue() default "0";
}

