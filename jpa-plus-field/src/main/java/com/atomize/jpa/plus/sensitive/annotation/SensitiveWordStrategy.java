package com.atomize.jpa.plus.sensitive.annotation;

import com.atomize.jpa.plus.sensitive.exception.SensitiveWordException;
import com.atomize.jpa.plus.sensitive.spi.SensitiveStrategy;
import com.atomize.jpa.plus.sensitive.spi.SensitiveWordProvider;

/**
 * 内置敏感词处理策略枚举
 *
 * <p>实现 {@link SensitiveStrategy} 接口，用户可自定义枚举实现该接口以扩展处理方式。</p>
 *
 * @author guanxiangkai
 * @see SensitiveStrategy
 * @since 2026年03月25日 星期三
 */
public enum SensitiveWordStrategy implements SensitiveStrategy {

    /**
     * 拒绝：包含敏感词时抛出异常，阻止保存
     */
    REJECT {
        @Override
        public String handle(String text, SensitiveWordProvider provider, String replacement) {
            if (provider.contains(text)) {
                throw new SensitiveWordException("文本包含敏感词");
            }
            return text;
        }
    },

    /**
     * 替换：将敏感词替换为指定字符后保存
     */
    REPLACE {
        @Override
        public String handle(String text, SensitiveWordProvider provider, String replacement) {
            return provider.replace(text, replacement);
        }
    }
}
