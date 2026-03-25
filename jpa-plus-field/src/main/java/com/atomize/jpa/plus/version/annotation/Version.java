package com.atomize.jpa.plus.version.annotation;

import java.lang.annotation.*;

/**
 * 乐观锁版本注解
 *
 * <p>标注在版本字段上（{@link Integer} 或 {@link Long}），保存时自动递增版本号。</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Version {
}

