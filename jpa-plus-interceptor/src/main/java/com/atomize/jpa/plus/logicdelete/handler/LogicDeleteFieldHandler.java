package com.atomize.jpa.plus.logicdelete.handler;

import com.atomize.jpa.plus.core.field.FieldHandler;
import com.atomize.jpa.plus.core.util.ReflectionUtils;
import com.atomize.jpa.plus.logicdelete.annotation.LogicDelete;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

/**
 * 逻辑删除字段处理器
 *
 * <p>保存时若逻辑删除字段为 null，自动设为"未删除"默认值。</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Slf4j
public class LogicDeleteFieldHandler implements FieldHandler {

    @Override
    public int order() {
        return 500;
    }

    @Override
    public boolean supports(Field field) {
        return field.isAnnotationPresent(LogicDelete.class);
    }

    @Override
    public void beforeSave(Object entity, Field field) {
        try {
            Object value = ReflectionUtils.getFieldValue(entity, field);
            if (value == null) {
                LogicDelete annotation = field.getAnnotation(LogicDelete.class);
                Object converted = convertValue(field.getType(), annotation.defaultValue());
                ReflectionUtils.setFieldValue(entity, field, converted);
            }
        } catch (Exception e) {
            log.error("逻辑删除字段处理失败: field={}", field.getName(), e);
        }
    }

    /**
     * 根据字段类型转换默认值（使用 switch 模式匹配）
     */
    private Object convertValue(Class<?> type, String value) {
        return switch (type.getName()) {
            case "int", "java.lang.Integer" -> Integer.parseInt(value);
            case "boolean", "java.lang.Boolean" -> Boolean.parseBoolean(value);
            case "long", "java.lang.Long" -> Long.parseLong(value);
            default -> value;
        };
    }
}

