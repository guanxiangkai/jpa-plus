package com.actomize.jpa.plus.desensitize.handler;

import com.actomize.jpa.plus.core.field.FieldHandler;
import com.actomize.jpa.plus.core.util.ReflectionUtils;
import com.actomize.jpa.plus.desensitize.annotation.Desensitize;
import com.actomize.jpa.plus.desensitize.spi.MaskStrategy;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字段脱敏处理器
 *
 * <p>实现 {@link FieldHandler} 接口，对标注了 {@link Desensitize} 的字段
 * 在查询后执行掩码处理。优先使用 {@code customStrategy}，否则使用内置 {@code strategy}。</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Slf4j
public class DesensitizeFieldHandler implements FieldHandler {

    /**
     * 自定义策略实例缓存
     */
    private final Map<Class<? extends MaskStrategy>, MaskStrategy> strategyCache = new ConcurrentHashMap<>();

    @Override
    public int order() {
        return 300;
    }

    @Override
    public boolean supports(Field field) {
        return field.isAnnotationPresent(Desensitize.class);
    }

    @Override
    public void afterQuery(Object entity, Field field) {
        try {
            Object value = ReflectionUtils.getFieldValue(entity, field);
            if (value instanceof String str && !str.isEmpty()) {
                Desensitize annotation = field.getAnnotation(Desensitize.class);
                MaskStrategy strategy = resolveStrategy(annotation);
                String masked = strategy.mask(str, annotation.maskChar());
                ReflectionUtils.setFieldValue(entity, field, masked);
            }
        } catch (Exception e) {
            log.error("字段脱敏失败: field={}", field.getName(), e);
        }
    }

    /**
     * 解析脱敏策略：customStrategy 优先于 strategy
     */
    private MaskStrategy resolveStrategy(Desensitize annotation) {
        Class<? extends MaskStrategy> customClass = annotation.customStrategy();
        if (customClass != MaskStrategy.class) {
            return strategyCache.computeIfAbsent(customClass, ReflectionUtils::instantiate);
        }
        // 内置枚举本身就是 MaskStrategy
        return annotation.strategy();
    }
}
