package com.atomize.jpaplus.dict.annotation;

import java.lang.annotation.*;

/**
 * 字典回写注解
 *
 * <p>标注在字典值字段上，查询后自动通过 {@link com.atomize.jpaplus.dict.spi.DictProvider}
 * 获取字典标签，并回写到指定的 label 字段。</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Dict {

    /**
     * 字典类型编码
     */
    String type();

    /**
     * 标签回写的目标字段名（默认为 "当前字段名 + Label"）
     */
    String labelField() default "";
}

