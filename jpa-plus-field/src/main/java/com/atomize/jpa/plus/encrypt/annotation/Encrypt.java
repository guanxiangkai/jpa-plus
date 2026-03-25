package com.atomize.jpa.plus.encrypt.annotation;

import java.lang.annotation.*;

/**
 * 字段加密注解
 *
 * <p>标注在实体字段上，保存前自动加密、查询后自动解密。
 * 仅支持 {@link String} 类型字段。</p>
 *
 * <p><b>设计模式：</b>标记注解模式（Marker Annotation），配合策略模式（Strategy）选择加密算法</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Encrypt {

    /**
     * 加密算法名称（默认 AES）
     *
     * @return 算法名称，须为 {@link javax.crypto.Cipher} 支持的算法
     */
    String algorithm() default "AES";
}

