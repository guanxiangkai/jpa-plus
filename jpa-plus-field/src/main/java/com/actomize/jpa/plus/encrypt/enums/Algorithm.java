package com.actomize.jpa.plus.encrypt.enums;

/**
 * 加密算法接口（可扩展枚举模式）
 *
 * <p>内置实现见 {@link EncryptAlgorithm}。
 * 用户可自定义枚举实现此接口，支持国密 SM4、RSA 等任意算法。</p>
 *
 * <h3>扩展示例</h3>
 * <pre>{@code
 * public enum MyAlgorithm implements Algorithm {
 *     SM4("SM4", "SM4/ECB/PKCS5Padding");
 *
 *     private final String name;
 *     private final String transformation;
 *     MyAlgorithm(String name, String transformation) {
 *         this.name = name;
 *         this.transformation = transformation;
 *     }
 *     @Override public String algorithmName() { return name; }
 *     @Override public String transformation() { return transformation; }
 * }
 * }</pre>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期二
 */
public interface Algorithm {

    /**
     * 算法名称（用于 {@link javax.crypto.spec.SecretKeySpec} 构造）
     */
    String algorithmName();

    /**
     * Cipher transformation（用于 {@link javax.crypto.Cipher#getInstance(String)}）
     */
    String transformation();
}

