package com.actomize.jpa.plus.starter;

import com.actomize.jpa.plus.field.encrypt.spi.EncryptKeyProvider;
import org.springframework.beans.factory.DisposableBean;

import java.util.*;

/**
 * 基于配置属性的密钥提供者（支持多版本密钥轮换）
 *
 * <h3>内存安全</h3>
 * <p>密钥在内部以 {@code char[]} 存储（而非 {@code String}），
 * 并在 Spring 容器关闭时通过 {@link DisposableBean#destroy()} 用零字节覆盖，
 * 以减少密钥在堆内存中的暴露窗口，降低堆转储泄露风险。</p>
 *
 * <p><b>注意：</b>{@link #getKey()} / {@link #getKeyByVersion(String)} 仍返回 {@code String}
 * （受 {@link EncryptKeyProvider} 接口约束），调用时会在堆上短暂创建明文 String。
 * 若对密钥生命周期有严格要求，请实现自定义 {@link EncryptKeyProvider} 并配合 HSM 或
 * Java {@code KeyStore} 使用。</p>
 */
public class PropertiesEncryptKeyProvider implements EncryptKeyProvider, DisposableBean {

    private static final String DEFAULT_VERSION = "v1";

    private final String activeVersion;
    /**
     * P1-10: Keys stored as char[] so they can be zeroed on shutdown.
     */
    private final Map<String, char[]> versionedKeys;

    public PropertiesEncryptKeyProvider(EncryptKeyProperties properties) {
        String configuredVersion = normalizeVersion(properties.getActiveVersion());
        LinkedHashMap<String, char[]> keys = new LinkedHashMap<>();
        if (properties.getKeys() != null) {
            properties.getKeys().forEach((version, key) -> {
                String normalizedVersion = normalizeVersion(version);
                if (key != null && !key.isBlank()) {
                    keys.put(normalizedVersion, key.toCharArray());
                }
            });
        }

        if (properties.getKey() != null && !properties.getKey().isBlank()) {
            keys.putIfAbsent(configuredVersion, properties.getKey().toCharArray());
        }
        if (keys.isEmpty()) {
            throw new IllegalStateException(
                    "[jpa-plus] No encryption key configured. " +
                            "Please set 'jpa-plus.encrypt.key' (or 'jpa-plus.encrypt.keys') in your application configuration. " +
                            "A missing key is a security misconfiguration — refusing to fall back to a well-known default.");
        }

        this.activeVersion = keys.containsKey(configuredVersion)
                ? configuredVersion
                : keys.keySet().iterator().next();
        this.versionedKeys = Map.copyOf(keys);
    }

    private static String normalizeVersion(String version) {
        if (version == null || version.isBlank()) {
            return DEFAULT_VERSION;
        }
        return version.trim();
    }

    @Override
    public String getKey() {
        return getKeyByVersion(activeVersion);
    }

    @Override
    public String getActiveVersion() {
        return activeVersion;
    }

    @Override
    public String getKeyByVersion(String version) {
        char[] keyChars = versionedKeys.get(normalizeVersion(version));
        if (keyChars == null) {
            throw new IllegalArgumentException("未配置加密密钥版本: " + version + "，已配置版本: " + versionedKeys.keySet());
        }
        return new String(keyChars);
    }

    @Override
    public List<String> getDecryptKeyVersions() {
        List<String> ordered = new ArrayList<>();
        ordered.add(activeVersion);
        for (String version : versionedKeys.keySet()) {
            if (!activeVersion.equals(version)) {
                ordered.add(version);
            }
        }
        return List.copyOf(ordered);
    }

    /**
     * P1-10: Zero out all key char arrays on Spring context shutdown to limit the window during
     * which plaintext keys reside in the heap.
     */
    @Override
    public void destroy() {
        versionedKeys.values().forEach(keyChars -> Arrays.fill(keyChars, '\0'));
    }
}
