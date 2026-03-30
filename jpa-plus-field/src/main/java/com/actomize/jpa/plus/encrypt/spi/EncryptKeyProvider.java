package com.actomize.jpa.plus.encrypt.spi;

/**
 * 加密密钥提供者（SPI）
 *
 * <p>用户实现此接口提供加密密钥，解耦密钥管理与加解密逻辑。
 * 密钥来源完全由用户控制 —— 可以从配置文件、环境变量、KMS、Vault 等获取。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @Component
 * public class MyEncryptKeyProvider implements EncryptKeyProvider {
 *     @Value("${encrypt.secret-key}")
 *     private String key;
 *
 *     @Override
 *     public String getKey() {
 *         return key;
 *     }
 * }
 * }</pre>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期二
 */
@FunctionalInterface
public interface EncryptKeyProvider {

    /**
     * 获取加密密钥
     *
     * @return 密钥字符串（AES 要求 16/24/32 字节，SM4 要求 16 字节等）
     */
    String getKey();
}

