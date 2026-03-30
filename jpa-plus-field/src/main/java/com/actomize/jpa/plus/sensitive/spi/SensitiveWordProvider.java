package com.actomize.jpa.plus.sensitive.spi;

/**
 * 敏感词数据提供者 SPI 接口
 *
 * <p>框架不内置任何敏感词库，用户必须实现此接口提供敏感词检测能力。
 * 可对接第三方敏感词库（如 DFA 算法、AC 自动机等）。</p>
 *
 * <p><b>设计模式：</b>SPI 服务发现模式 + 策略模式</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
public interface SensitiveWordProvider {

    /**
     * 检测文本是否包含敏感词
     *
     * @param text 待检测文本
     * @return 包含敏感词返回 {@code true}
     */
    boolean contains(String text);

    /**
     * 替换文本中的敏感词
     *
     * @param text        原始文本
     * @param replacement 替换字符串
     * @return 替换后的文本
     */
    String replace(String text, String replacement);
}

