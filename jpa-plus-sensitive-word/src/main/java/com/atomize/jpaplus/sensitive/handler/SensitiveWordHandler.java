package com.atomize.jpaplus.sensitive.handler;

import com.atomize.jpaplus.core.field.FieldHandler;
import com.atomize.jpaplus.core.util.ReflectionUtils;
import com.atomize.jpaplus.sensitive.annotation.SensitiveWord;
import com.atomize.jpaplus.sensitive.annotation.SensitiveWordStrategy;
import com.atomize.jpaplus.sensitive.exception.SensitiveWordException;
import com.atomize.jpaplus.sensitive.spi.SensitiveWordProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

/**
 * 敏感词检测处理器
 *
 * <p>实现 {@link FieldHandler}，在保存前检测 {@link SensitiveWord} 标注的字段：
 * <ul>
 *   <li>{@link SensitiveWordStrategy#REJECT}：检测到敏感词时抛出 {@link SensitiveWordException}</li>
 *   <li>{@link SensitiveWordStrategy#REPLACE}：将敏感词替换为指定字符后继续保存</li>
 * </ul>
 * </p>
 *
 * <p><b>设计模式：</b>策略模式（Strategy） —— 通过 {@link SensitiveWordProvider} SPI 解耦敏感词检测实现</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Slf4j
@RequiredArgsConstructor
public class SensitiveWordHandler implements FieldHandler {

    /**
     * 敏感词数据提供者（由用户通过 SPI 或 Spring Bean 注入）
     */
    private final SensitiveWordProvider provider;

    @Override
    public int order() {
        return 50;
    }

    @Override
    public boolean supports(Field field) {
        return field.isAnnotationPresent(SensitiveWord.class);
    }

    @Override
    public void beforeSave(Object entity, Field field) {
        try {
            Object value = ReflectionUtils.getFieldValue(entity, field);
            if (value instanceof String text) {
                SensitiveWord anno = field.getAnnotation(SensitiveWord.class);
                switch (anno.strategy()) {
                    case REJECT -> {
                        if (provider.contains(text)) {
                            throw new SensitiveWordException("字段 '" + field.getName() + "' 包含敏感词");
                        }
                    }
                    case REPLACE -> {
                        String replaced = provider.replace(text, anno.replacement());
                        ReflectionUtils.setFieldValue(entity, field, replaced);
                    }
                }
            }
        } catch (SensitiveWordException e) {
            throw e;
        } catch (Exception e) {
            log.error("敏感词检测失败: field={}", field.getName(), e);
        }
    }
}

