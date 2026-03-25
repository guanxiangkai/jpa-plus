package com.atomize.jpa.plus.sensitive.spi;

/**
 * 敏感词处理策略接口（可扩展枚举模式）
 *
 * <p>内置实现见 {@link com.atomize.jpa.plus.sensitive.annotation.SensitiveWordStrategy SensitiveWordStrategy}。
 * 用户可自定义枚举实现此接口，支持审计、标记、部分替换等任意处理方式。</p>
 *
 * <h3>扩展示例</h3>
 * <pre>{@code
 * public enum MySensitiveStrategy implements SensitiveStrategy {
 *     AUDIT {
 *         @Override
 *         public String handle(String text, SensitiveWordProvider provider, String replacement) {
 *             if (provider.contains(text)) {
 *                 auditLogger.log("敏感词命中: " + text);
 *             }
 *             return text; // 放行但记录
 *         }
 *     };
 * }
 * }</pre>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期二
 */
public interface SensitiveStrategy {

    /**
     * 处理含敏感词的文本
     *
     * @param text        原始文本
     * @param provider    敏感词检测提供者
     * @param replacement 替换字符串
     * @return 处理后的文本
     * @throws RuntimeException 策略为拒绝时可抛出异常阻止保存
     */
    String handle(String text, SensitiveWordProvider provider, String replacement);
}

