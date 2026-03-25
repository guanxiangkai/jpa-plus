package com.atomize.jpa.plus.version.handler;

import com.atomize.jpa.plus.core.field.FieldHandler;
import com.atomize.jpa.plus.core.util.ReflectionUtils;
import com.atomize.jpa.plus.version.annotation.Version;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

/**
 * 乐观锁版本自增处理器
 *
 * <p>实现 {@link FieldHandler}，在保存前自动将 {@link Version} 标注的字段值 +1。
 * 支持 {@link Integer} 和 {@link Long} 类型。</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Slf4j
public class VersionFieldHandler implements FieldHandler {

    @Override
    public int order() {
        return 400;
    }

    @Override
    public boolean supports(Field field) {
        return field.isAnnotationPresent(Version.class);
    }

    @Override
    public void beforeSave(Object entity, Field field) {
        try {
            Object value = ReflectionUtils.getFieldValue(entity, field);
            switch (value) {
                case Integer v -> ReflectionUtils.setFieldValue(entity, field, v + 1);
                case Long v -> ReflectionUtils.setFieldValue(entity, field, v + 1L);
                case null, default -> { /* 不处理其他类型或 null */ }
            }
        } catch (Exception e) {
            log.error("版本号自增失败: field={}", field.getName(), e);
        }
    }
}

