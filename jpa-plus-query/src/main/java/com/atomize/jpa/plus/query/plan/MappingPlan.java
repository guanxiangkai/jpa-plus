package com.atomize.jpa.plus.query.plan;

import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 映射计划 —— 预编译的字段映射方案
 * <p>
 * 避免每次查询都反射查找 setter，通过 MethodHandle 直接设置字段值。
 */
public class MappingPlan<R> {

    private final List<FieldMapping> mappings;
    private final Constructor<R> constructor;

    public MappingPlan(Constructor<R> constructor, List<FieldMapping> mappings) {
        this.constructor = constructor;
        this.mappings = mappings;
        this.constructor.setAccessible(true);
    }

    /**
     * 从 ResultSet 当前行创建实例并填充字段
     */
    public R apply(ResultSet rs) throws SQLException {
        try {
            R instance = constructor.newInstance();
            for (FieldMapping mapping : mappings) {
                Object value = rs.getObject(mapping.columnIndex(), mapping.fieldType());
                mapping.setValue(instance, value);
            }
            return instance;
        } catch (SQLException e) {
            throw e;
        } catch (Throwable e) {
            throw new SQLException("Failed to map ResultSet row to " + constructor.getDeclaringClass().getSimpleName(), e);
        }
    }
}

