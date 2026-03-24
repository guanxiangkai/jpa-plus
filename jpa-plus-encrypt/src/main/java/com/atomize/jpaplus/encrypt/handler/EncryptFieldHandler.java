package com.atomize.jpaplus.encrypt.handler;

import com.atomize.jpaplus.core.field.FieldHandler;
import com.atomize.jpaplus.core.util.ReflectionUtils;
import com.atomize.jpaplus.encrypt.annotation.Encrypt;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 字段加密处理器
 *
 * <p>实现 {@link FieldHandler} 接口，对标注了 {@link Encrypt} 的字段执行加密/解密操作：
 * <ul>
 *   <li>保存前（{@link #beforeSave}）：明文 → 密文（Base64 编码）</li>
 *   <li>查询后（{@link #afterQuery}）：密文 → 明文</li>
 * </ul>
 * </p>
 *
 * <p>加密密钥通过构造函数注入，避免硬编码。生产环境应通过配置属性
 * {@code jpa-plus.encrypt.key} 注入密钥。</p>
 *
 * <p><b>设计模式：</b>策略模式（Strategy） —— 通过 {@link Encrypt#algorithm()} 选择不同加密策略</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Slf4j
public class EncryptFieldHandler implements FieldHandler {

    private static final String DEFAULT_KEY = "JpaPlusEncKey128";

    private final String encryptKey;

    /**
     * 使用默认密钥构造（仅用于开发/测试环境）
     */
    public EncryptFieldHandler() {
        this(DEFAULT_KEY);
    }

    /**
     * 使用指定密钥构造（推荐：生产环境通过配置注入）
     *
     * @param encryptKey AES 密钥（长度须为 16/24/32 字节）
     */
    public EncryptFieldHandler(String encryptKey) {
        this.encryptKey = encryptKey;
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public boolean supports(Field field) {
        return field.isAnnotationPresent(Encrypt.class);
    }

    @Override
    public void beforeSave(Object entity, Field field) {
        try {
            Object value = ReflectionUtils.getFieldValue(entity, field);
            if (value instanceof String plainText) {
                Encrypt encrypt = field.getAnnotation(Encrypt.class);
                String encrypted = doEncrypt(plainText, encrypt.algorithm());
                ReflectionUtils.setFieldValue(entity, field, encrypted);
            }
        } catch (Exception e) {
            log.error("字段加密失败: field={}", field.getName(), e);
        }
    }

    @Override
    public void afterQuery(Object entity, Field field) {
        try {
            Object value = ReflectionUtils.getFieldValue(entity, field);
            if (value instanceof String encryptedText) {
                Encrypt encrypt = field.getAnnotation(Encrypt.class);
                String decrypted = doDecrypt(encryptedText, encrypt.algorithm());
                ReflectionUtils.setFieldValue(entity, field, decrypted);
            }
        } catch (Exception e) {
            log.error("字段解密失败: field={}", field.getName(), e);
        }
    }

    private String doEncrypt(String plainText, String algorithm) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(encryptKey.getBytes(StandardCharsets.UTF_8), algorithm);
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private String doDecrypt(String encryptedText, String algorithm) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(encryptKey.getBytes(StandardCharsets.UTF_8), algorithm);
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decoded = Base64.getDecoder().decode(encryptedText);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}

