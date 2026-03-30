package com.actomize.jpa.plus.query.wrapper;

import com.actomize.jpa.plus.core.exception.JpaPlusException;
import com.actomize.jpa.plus.core.util.NamingUtils;
import com.actomize.jpa.plus.core.util.ReflectionUtils;
import com.actomize.jpa.plus.query.metadata.ColumnMeta;
import com.actomize.jpa.plus.query.metadata.TableMeta;
import jakarta.persistence.Column;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lambda 列名解析器
 *
 * <p>从 {@link SFunction} Lambda 方法引用中提取方法名，推导列名。
 * 支持 {@code @Column} 注解覆盖列名。</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
public final class LambdaColumnResolver {

    private static final Map<String, String> COLUMN_CACHE = new ConcurrentHashMap<>();

    private LambdaColumnResolver() {
    }

    /**
     * 从 Lambda 中解析列名
     */
    public static <T> String resolve(SFunction<T, ?> func) {
        SerializedLambda lambda = getSerializedLambda(func);
        String methodName = lambda.getImplMethodName();
        String className = lambda.getImplClass().replace('/', '.');

        String cacheKey = className + "#" + methodName;
        return COLUMN_CACHE.computeIfAbsent(cacheKey, _ -> doResolve(className, methodName));
    }

    /**
     * 从 Lambda 中解析 ColumnMeta
     */
    public static <T> ColumnMeta resolveColumn(SFunction<T, ?> func, TableMeta table) {
        String columnName = resolve(func);
        Class<?> fieldType = resolveFieldType(func);
        return ColumnMeta.of(table, columnName, fieldType);
    }

    /**
     * 从 Lambda 中解析字段类型
     */
    public static <T> Class<?> resolveFieldType(SFunction<T, ?> func) {
        SerializedLambda lambda = getSerializedLambda(func);
        String methodName = lambda.getImplMethodName();
        String className = lambda.getImplClass().replace('/', '.');

        try {
            Class<?> clazz = Class.forName(className);
            String fieldName = methodToFieldName(methodName);
            Field field = ReflectionUtils.findField(clazz, fieldName);
            return field != null ? field.getType() : Object.class;
        } catch (ClassNotFoundException e) {
            return Object.class;
        }
    }

    private static String doResolve(String className, String methodName) {
        try {
            Class<?> clazz = Class.forName(className);
            String fieldName = methodToFieldName(methodName);

            // 查找字段上的 @Column 注解
            Field field = ReflectionUtils.findField(clazz, fieldName);
            if (field != null) {
                Column column = field.getAnnotation(Column.class);
                if (column != null && !column.name().isEmpty()) {
                    return column.name();
                }
            }

            // 驼峰转蛇形
            return NamingUtils.camelToSnake(fieldName);
        } catch (ClassNotFoundException e) {
            throw new JpaPlusException("Cannot resolve class: " + className, e);
        }
    }

    private static String methodToFieldName(String methodName) {
        String fieldName;
        if (methodName.startsWith("get") && methodName.length() > 3) {
            fieldName = methodName.substring(3);
        } else if (methodName.startsWith("is") && methodName.length() > 2) {
            fieldName = methodName.substring(2);
        } else {
            fieldName = methodName;
        }
        return fieldName.substring(0, 1).toLowerCase(Locale.ROOT) + fieldName.substring(1);
    }


    private static SerializedLambda getSerializedLambda(Serializable func) {
        try {
            Method method = func.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(true);
            return (SerializedLambda) method.invoke(func);
        } catch (Exception e) {
            throw new JpaPlusException("Failed to extract SerializedLambda from function reference", e);
        }
    }
}

