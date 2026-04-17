package com.actomize.jpa.plus.field.desensitize.spi;

/**
 * 脱敏策略接口（可扩展枚举模式）
 *
 * <p>内置实现见 {@link com.actomize.jpa.plus.field.desensitize.annotation.DesensitizeStrategy DesensitizeStrategy}。
 * 用户可自定义 {@code enum} 实现此接口，支持任意脱敏规则。</p>
 *
 * <h3>扩展示例</h3>
 * <pre>{@code
 * public enum MyMaskStrategy implements MaskStrategy {
 *     IP_ADDRESS {
 *         @Override
 *         public String mask(String value, char maskChar) {
 *             // 192.168.1.100 → 192.*.*.*
 *             return value.substring(0, value.indexOf('.')) + ".*.*.*";
 *         }
 *     };
 * }
 * }</pre>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期二
 */
@FunctionalInterface
public interface MaskStrategy {

    /**
     * 执行掩码处理
     *
     * @param value    原始值
     * @param maskChar 掩码字符
     * @return 脱敏后的值
     */
    String mask(String value, char maskChar);
}

