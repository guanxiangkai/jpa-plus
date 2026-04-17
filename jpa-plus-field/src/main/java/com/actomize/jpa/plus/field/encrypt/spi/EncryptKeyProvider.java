package com.actomize.jpa.plus.field.encrypt.spi;

import java.util.List;

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

    /**
     * 当前生效密钥版本（用于新数据加密）
     *
     * <p>默认返回 {@code v1}。实现多版本密钥轮换时应返回当前写入版本。</p>
     */
    default String getActiveVersion() {
        return "v1";
    }

    /**
     * 按版本获取密钥
     *
     * <p>默认实现回退到 {@link #getKey()} 以保证向后兼容。</p>
     *
     * @param version 密钥版本（如 v1 / v2）
     * @return 对应版本的密钥
     */
    default String getKeyByVersion(String version) {
        return getKey();
    }

    /**
     * 旧版非版本密文的解密尝试顺序
     *
     * <p>平滑迁移时可返回多个版本：先尝试 active，再尝试历史版本。</p>
     */
    default List<String> getDecryptKeyVersions() {
        return List.of(getActiveVersion());
    }
}
