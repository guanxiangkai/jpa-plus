package com.atomize.jpa.plus.encrypt.handler;

import com.atomize.jpa.plus.core.field.FieldHandler;
import com.atomize.jpa.plus.core.util.ReflectionUtils;
import com.atomize.jpa.plus.encrypt.annotation.Encrypt;
import com.atomize.jpa.plus.encrypt.enums.Algorithm;
import com.atomize.jpa.plus.encrypt.spi.EncryptKeyProvider;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

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
 * <p>加密密钥通过 {@link EncryptKeyProvider} SPI 获取，
 * 加密算法通过 {@link Algorithm} 可扩展枚举确定。</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Slf4j
public class EncryptFieldHandler implements FieldHandler {

    private final EncryptKeyProvider keyProvider;

    /**
     * 自定义算法实例缓存
     */
    private final Map<Class<? extends Algorithm>, Algorithm> algorithmCache = new ConcurrentHashMap<>();

    public EncryptFieldHandler(EncryptKeyProvider keyProvider) {
        this.keyProvider = Objects.requireNonNull(keyProvider, "EncryptKeyProvider must not be null");
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
                Algorithm algo = resolveAlgorithm(field.getAnnotation(Encrypt.class));
                String encrypted = doEncrypt(plainText, algo);
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
                Algorithm algo = resolveAlgorithm(field.getAnnotation(Encrypt.class));
                String decrypted = doDecrypt(encryptedText, algo);
                ReflectionUtils.setFieldValue(entity, field, decrypted);
            }
        } catch (Exception e) {
            log.error("字段解密失败: field={}", field.getName(), e);
        }
    }

    /**
     * 解析算法：customAlgorithm 优先于 algorithm
     */
    private Algorithm resolveAlgorithm(Encrypt annotation) {
        Class<? extends Algorithm> customClass = annotation.customAlgorithm();
        if (customClass != Algorithm.class) {
            return algorithmCache.computeIfAbsent(customClass, this::instantiate);
        }
        return annotation.algorithm();
    }

    private Algorithm instantiate(Class<? extends Algorithm> clazz) {
        try {
            if (clazz.isEnum()) {
                Algorithm[] constants = clazz.getEnumConstants();
                if (constants.length > 0) return constants[0];
            }
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot instantiate Algorithm: " + clazz.getName(), e);
        }
    }

    private String doEncrypt(String plainText, Algorithm algo) throws Exception {
        String key = keyProvider.getKey();
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algo.algorithmName());
        Cipher cipher = Cipher.getInstance(algo.transformation());
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private String doDecrypt(String encryptedText, Algorithm algo) throws Exception {
        String key = keyProvider.getKey();
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algo.algorithmName());
        Cipher cipher = Cipher.getInstance(algo.transformation());
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decoded = Base64.getDecoder().decode(encryptedText);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}

