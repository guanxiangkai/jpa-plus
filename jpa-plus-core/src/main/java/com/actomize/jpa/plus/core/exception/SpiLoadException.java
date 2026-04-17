package com.actomize.jpa.plus.core.exception;

/**
 * SPI 加载异常
 *
 * <p>当从 {@code META-INF/jpa-plus/} 目录加载 SPI 实现时发生错误（类不存在、
 * 无法实例化、类型不匹配等），抛出此异常。</p>
 */
public final class SpiLoadException extends JpaPlusException {

    public SpiLoadException(String message) {
        super(message);
    }

    public SpiLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
