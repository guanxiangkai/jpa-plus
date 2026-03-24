package com.atomize.jpaplus.desensitize.handler;

import com.atomize.jpaplus.core.field.FieldHandler;
import com.atomize.jpaplus.core.util.ReflectionUtils;
import com.atomize.jpaplus.desensitize.annotation.Desensitize;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

/**
 * 字段脱敏处理器
 *
 * <p>实现 {@link FieldHandler} 接口，对标注了 {@link Desensitize} 的字段
 * 在查询后执行掩码处理。掩码算法封装在 {@link com.atomize.jpaplus.desensitize.annotation.DesensitizeStrategy}
 * 枚举中，遵循开闭原则。</p>
 *
 * <p><b>设计模式：</b>策略模式（Strategy） —— 枚举多态实现不同掩码算法</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Slf4j
public class DesensitizeFieldHandler implements FieldHandler {

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
                String masked = annotation.strategy().mask(str, annotation.maskChar());
                ReflectionUtils.setFieldValue(entity, field, masked);
            }
        } catch (Exception e) {
            log.error("字段脱敏失败: field={}", field.getName(), e);
        }
    }
}

