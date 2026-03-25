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
 * 根据字段类型自动处理：
 * <ul>
 *   <li>{@code Integer/int} → null 时初始化为 0，否则 +1</li>
 *   <li>{@code Long/long} → null 时初始化为 0L，否则 +1L</li>
 *   <li>{@code Short/short} → null 时初始化为 0，否则 +1</li>
 * </ul>
 * </p>
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
            if (value == null) {
                // 首次保存：根据字段类型初始化为 0
                Object initial = resolveInitialVersion(field.getType());
                ReflectionUtils.setFieldValue(entity, field, initial);
            } else {
                // 更新：版本号 +1
                Object incremented = switch (value) {
                    case Integer v -> v + 1;
                    case Long v -> v + 1L;
                    case Short v -> (short) (v + 1);
                    default -> value;
                };
                ReflectionUtils.setFieldValue(entity, field, incremented);
            }
        } catch (Exception e) {
            log.error("版本号处理失败: field={}", field.getName(), e);
        }
    }

    /**
     * 根据字段类型推导版本初始值
     */
    private Object resolveInitialVersion(Class<?> fieldType) {
        return switch (fieldType.getName()) {
            case "int", "java.lang.Integer" -> 0;
            case "long", "java.lang.Long" -> 0L;
            case "short", "java.lang.Short" -> (short) 0;
            default -> 0;
        };
    }
}
