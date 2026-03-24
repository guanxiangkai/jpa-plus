package com.atomize.jpaplus.datasource.annotation;

import java.lang.annotation.*;

/**
 * 数据源切换注解
 *
 * <p>标注在方法或类上，指定执行时使用的数据源名称。
 * 配合 {@link com.atomize.jpaplus.datasource.context.JpaPlusContext} 实现多数据源路由。</p>
 *
 * <p><b>注意：</b>在活跃事务内不允许切换数据源，防止事务一致性问题。</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DS {

    /**
     * 数据源名称（默认 "master"）
     */
    String value() default "master";
}

