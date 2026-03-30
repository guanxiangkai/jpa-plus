package com.actomize.jpa.plus.sensitive.exception;

import com.actomize.jpa.plus.core.exception.JpaPlusException;
import com.actomize.jpa.plus.sensitive.annotation.SensitiveWordStrategy;

/**
 * 敏感词异常
 *
 * <p>当 {@link SensitiveWordStrategy#REJECT}
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

