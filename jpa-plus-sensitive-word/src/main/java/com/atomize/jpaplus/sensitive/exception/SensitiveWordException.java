package com.atomize.jpaplus.sensitive.exception;

import com.atomize.jpaplus.core.exception.JpaPlusException;

/**
 * 敏感词异常
 *
 * <p>当 {@link com.atomize.jpaplus.sensitive.annotation.SensitiveWordStrategy#REJECT}
 * 策略检测到敏感词时抛出。</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
public class SensitiveWordException extends JpaPlusException {

    public SensitiveWordException(String message) {
        super(message);
    }
}

