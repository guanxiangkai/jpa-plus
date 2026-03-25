package com.atomize.jpa.plus.sensitive.handler;

import com.atomize.jpa.plus.core.field.FieldHandler;
import com.atomize.jpa.plus.core.util.ReflectionUtils;
import com.atomize.jpa.plus.sensitive.annotation.SensitiveWord;
import com.atomize.jpa.plus.sensitive.exception.SensitiveWordException;
import com.atomize.jpa.plus.sensitive.spi.SensitiveStrategy;
import com.atomize.jpa.plus.sensitive.spi.SensitiveWordProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 敏感词检测处理器
 *
 * <p>实现 {@link FieldHandler}，在保存前检测 {@link SensitiveWord} 标注的字段，
 * 委托给 {@link SensitiveStrategy} 执行具体处理逻辑。</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Slf4j
@RequiredArgsConstructor
public class SensitiveWordHandler implements FieldHandler {

    private final SensitiveWordProvider provider;

    /**
     * 自定义策略实例缓存
     */
    private final Map<Class<? extends SensitiveStrategy>, SensitiveStrategy> strategyCache = new ConcurrentHashMap<>();

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
                SensitiveStrategy strategy = resolveStrategy(anno);
                String result = strategy.handle(text, provider, anno.replacement());
                ReflectionUtils.setFieldValue(entity, field, result);
            }
        } catch (SensitiveWordException e) {
            throw e;
        } catch (Exception e) {
            log.error("敏感词检测失败: field={}", field.getName(), e);
        }
    }

    /**
     * 解析策略：customStrategy 优先于 strategy
     */
    private SensitiveStrategy resolveStrategy(SensitiveWord annotation) {
        Class<? extends SensitiveStrategy> customClass = annotation.customStrategy();
        if (customClass != SensitiveStrategy.class) {
            return strategyCache.computeIfAbsent(customClass, ReflectionUtils::instantiate);
        }
        return annotation.strategy();
    }
}
