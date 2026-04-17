package com.actomize.jpa.plus.query.wrapper;

import com.actomize.jpa.plus.core.exception.JpaPlusException;
import com.actomize.jpa.plus.core.util.NamingUtils;
import com.actomize.jpa.plus.core.util.ReflectionUtils;
import com.actomize.jpa.plus.query.metadata.ColumnMeta;
import com.actomize.jpa.plus.query.metadata.TableMeta;
import jakarta.persistence.Column;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lambda 列名解析器
 *
 * <p>从 {@link TypedGetter} Lambda 方法引用中提取方法名，推导列名。
 * 支持 {@code @Column} 注解覆盖列名。</p>
 *
 * <p>使用 {@link MethodHandles#privateLookupIn(Class, MethodHandles.Lookup)} 替代
 * {@code method.setAccessible(true)}，在 Java 17+ 模块系统下零警告。</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
public final class LambdaColumnResolver {

    private static final Map<String, String> COLUMN_CACHE = new ConcurrentHashMap<>();
    /**
     * Lambda writeReplace 方法缓存，避免反复查找
     */
    private static final Map<Class<?>, Method> WRITE_REPLACE_CACHE = new ConcurrentHashMap<>();

    private LambdaColumnResolver() {
    }

    /**
     * 从 Lambda 中解析列名
     */
    public static <T> String resolve(TypedGetter<T, ?> func) {
        SerializedLambda lambda = getSerializedLambda(func);
        String methodName = lambda.getImplMethodName();
        String className = lambda.getImplClass().replace('/', '.');

        // 缓存键使用完全限定类名（binary name 经 replace('/','.')，含包路径），已是全局唯一键
        String cacheKey = className + "#" + methodName;
        return COLUMN_CACHE.computeIfAbsent(cacheKey, _ -> doResolve(className, methodName));
    }

    /**
     * 从 Lambda 中解析 ColumnMeta
     */
    public static <T> ColumnMeta resolveColumn(TypedGetter<T, ?> func, TableMeta table) {
        String columnName = resolve(func);
        Class<?> fieldType = resolveFieldType(func);
        return ColumnMeta.of(table, columnName, fieldType);
    }

    /**
     * 从 Lambda 中解析字段类型
     */
    public static <T> Class<?> resolveFieldType(TypedGetter<T, ?> func) {
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

    /**
     * 获取 Lambda 的 SerializedLambda。
     *
     * <p>优先尝试通过 {@link MethodHandles#privateLookupIn} 调用，避免 JVM 强封装警告（Java 17+）。
     * 若受模块限制则回退到 {@code setAccessible(true)}。</p>
     */
    private static SerializedLambda getSerializedLambda(Serializable func) {
        Class<?> funcClass = func.getClass();
        Method writeReplace = WRITE_REPLACE_CACHE.computeIfAbsent(funcClass, cls -> {
            try {
                return cls.getDeclaredMethod("writeReplace");
            } catch (NoSuchMethodException e) {
                throw new JpaPlusException("No writeReplace method found on lambda class: " + cls.getName(), e);
            }
        });

        try {
            // 优先使用 MethodHandles.privateLookupIn（Java 9+，模块友好）
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(funcClass, MethodHandles.lookup());
            return (SerializedLambda) lookup.unreflect(writeReplace).bindTo(func).invokeWithArguments();
        } catch (IllegalAccessException e) {
            // 模块封装问题，回退到 setAccessible
            try {
                writeReplace.setAccessible(true);
                return (SerializedLambda) writeReplace.invoke(func);
            } catch (Exception ex) {
                throw new JpaPlusException("Failed to extract SerializedLambda from function reference", ex);
            }
        } catch (Throwable t) {
            throw new JpaPlusException("Failed to extract SerializedLambda from function reference", t);
        }
    }
}

