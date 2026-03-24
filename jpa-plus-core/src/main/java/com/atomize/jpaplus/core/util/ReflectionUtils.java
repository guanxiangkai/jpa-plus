package com.atomize.jpaplus.core.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 反射工具类
 *
 * <p>集中管理所有反射操作，避免反射代码在各模块中重复。
 * 所有方法均为线程安全的静态方法。</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
public final class ReflectionUtils {

    private ReflectionUtils() {
    }

    /**
     * 在类层次结构中查找指定名称的字段
     *
     * @param clazz 起始类
     * @param name  字段名
     * @return 找到的字段，未找到返回 {@code null}
     */
    public static Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 获取类层次结构中的所有字段（包括父类），并设置可访问
     *
     * @param clazz 目标类
     * @return 所有字段的不可变列表
     */
    public static List<Field> getHierarchyFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                field.setAccessible(true);
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return List.copyOf(fields);
    }

    /**
     * 安全设置字段可访问并获取值
     *
     * @param entity 实体对象
     * @param field  字段
     * @return 字段值
     */
    public static Object getFieldValue(Object entity, Field field) {
        try {
            field.setAccessible(true);
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access field: " + field.getName(), e);
        }
    }

    /**
     * 安全设置字段值
     *
     * @param entity 实体对象
     * @param field  字段
     * @param value  要设置的值
     */
    public static void setFieldValue(Object entity, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(entity, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot set field: " + field.getName(), e);
        }
    }
}

