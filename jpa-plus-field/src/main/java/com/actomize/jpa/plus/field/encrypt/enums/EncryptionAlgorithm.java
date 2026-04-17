package com.actomize.jpa.plus.field.encrypt.enums;

/**
 * 加密算法接口（可扩展枚举模式）
 *
 * <p>内置实现见 {@link EncryptAlgorithm}。
 * 用户可自定义枚举实现此接口，支持国密 SM4、RSA 等任意算法。</p>
 *
 * <h3>扩展示例（hutool-crypto 国密 SM4）</h3>
 * <pre>{@code
 * public enum MyAlgorithm implements EncryptionAlgorithm {
 *     SM4("SM4", "SM4/ECB/PKCS5Padding");
 *     ...
 * }
 * }</pre>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期二
 */
public interface EncryptionAlgorithm {

    /**
     * 算法名称（用于 {@link javax.crypto.spec.SecretKeySpec} 构造）
     */
    String algorithmName();

    /**
     * Cipher transformation（用于 {@link javax.crypto.Cipher#getInstance(String)}）
     */
    String transformation();

    /**
     * 是否需要 IV（初始化向量）。
     * CBC/CTR/GCM 等模式需要 IV；ECB 不需要。
     * 默认 {@code false}（向后兼容 ECB）。
     */
    default boolean needsIv() {
        return false;
    }

    /**
     * 密钥长度（字节），用于对用户配置的 key 进行补齐或截断。
     * AES-128 = 16，AES-192 = 24，AES-256 = 32，SM4 = 16。
     */
    default int keySize() {
        return 16;
    }
}
