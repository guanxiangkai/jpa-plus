package com.actomize.jpa.plus.starter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 加密密钥配置（支持多版本轮换）
 *
 * <pre>{@code
 * jpa-plus:
 *   encrypt:
 *     active-version: v2
 *     key: your-secret-key-16chars
 *     keys:
 *       v1: oldSecretKey16Chars
 *       v2: newSecretKey16Chars
 * }</pre>
 *
 * <p><b>安全要求：</b>密钥长度不得少于 16 个字符，请勿使用默认值或简单密钥。
 * 密钥应通过环境变量或密钥管理服务注入，避免硬编码在配置文件中。</p>
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "jpa-plus.encrypt")
public class EncryptKeyProperties {

    /**
     * 兼容旧版本的单密钥配置（向后兼容，建议迁移到 keys 多版本模式）
     *
     * <p><b>必须配置</b>：{@code jpa-plus.encrypt.key=yourSecretKey}</p>
     * <p>密钥长度至少 16 个字符，使用 AES-128 时建议 16 字符，AES-256 时建议 32 字符。</p>
     */
    @NotBlank(message = "jpa-plus.encrypt.key 不能为空，请在配置文件中指定加密密钥")
    @Size(min = 16, message = "jpa-plus.encrypt.key 长度不能少于 16 个字符")
    private String key;

    /**
     * 当前写入密钥版本（多版本模式下生效）
     */
    private String activeVersion = "v1";

    /**
     * 多版本密钥表（version → key），用于密钥轮换场景
     */
    private Map<String, String> keys = new LinkedHashMap<>();

    /**
     * 返回加密密钥。
     *
     * <p>在 Spring 配置校验前（如单元测试中直接实例化）调用时，
     * 提前抛出 {@link com.actomize.jpa.plus.core.exception.JpaPlusConfigException}，
     * 而非等到第一次加密时才抛出 {@code NullPointerException}。</p>
     *
     * @return 加密密钥（非空，长度 ≥ 16）
     * @throws com.actomize.jpa.plus.core.exception.JpaPlusConfigException 若密钥未配置
     */
    public String getKey() {
        if (key == null || key.isBlank()) {
            throw new com.actomize.jpa.plus.core.exception.JpaPlusConfigException(
                    "加密密钥未配置，请设置 jpa-plus.encrypt.key（长度 ≥ 16 个字符）");
        }
        return key;
    }

    /**
     * 返回不可变的多版本密钥视图，防止外部意外修改
     */
    public Map<String, String> getKeys() {
        return Collections.unmodifiableMap(keys);
    }

    /**
     * Spring 配置绑定专用 setter（保留内部可变 Map）
     */
    public void setKeys(Map<String, String> keys) {
        if (keys != null) {
            for (Map.Entry<String, String> entry : keys.entrySet()) {
                String v = entry.getValue();
                if (v == null || v.length() < 16) {
                    throw new IllegalArgumentException(
                            "jpa-plus.encrypt.keys[" + entry.getKey() + "] 长度不能少于 16 个字符");
                }
            }
        }
        this.keys = keys == null ? new LinkedHashMap<>() : new LinkedHashMap<>(keys);
    }
}
