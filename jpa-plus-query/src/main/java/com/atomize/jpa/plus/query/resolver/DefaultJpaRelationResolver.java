package com.atomize.jpa.plus.query.resolver;

import com.atomize.jpa.plus.core.exception.JpaPlusException;
import com.atomize.jpa.plus.core.util.ReflectionUtils;
import com.atomize.jpa.plus.query.metadata.*;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 默认 JPA 关联解析器
 * <p>
 * 扫描实体类上的 JPA 关联注解，自动推断 Join 条件：
 * - @ManyToOne → 外键在当前表
 * - @OneToMany → 外键在对方表（通过 mappedBy）
 * - @ManyToMany → 中间表连接
 */
@Slf4j
public class DefaultJpaRelationResolver implements JpaRelationResolver {

    @Override
    public List<JoinNode> resolve(Class<?> entityClass, TableMeta tableMeta) {
        List<JoinNode> nodes = new ArrayList<>();
        Class<?> current = entityClass;

        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                JoinNode node = resolveField(field, tableMeta);
                if (node != null) {
                    nodes.add(node);
                }
            }
            current = current.getSuperclass();
        }
        return nodes;
    }

    @Override
    public JoinNode resolveProperty(Class<?> entityClass, String propertyName, TableMeta tableMeta) {
        Field field = ReflectionUtils.findField(entityClass, propertyName);
        if (field == null) {
            throw new JpaPlusException("Property not found: " + propertyName + " in " + entityClass.getName());
        }
        JoinNode node = resolveField(field, tableMeta);
        if (node == null) {
            throw new JpaPlusException("Property is not a JPA relation: " + propertyName);
        }
        return node;
    }

    private JoinNode resolveField(Field field, TableMeta sourceMeta) {
        if (field.isAnnotationPresent(ManyToOne.class)) {
            return resolveManyToOne(field, sourceMeta);
        } else if (field.isAnnotationPresent(OneToMany.class)) {
            return resolveOneToMany(field, sourceMeta);
        } else if (field.isAnnotationPresent(ManyToMany.class)) {
            return resolveManyToMany(field, sourceMeta);
        } else if (field.isAnnotationPresent(OneToOne.class)) {
            return resolveOneToOne(field, sourceMeta);
        }
        return null;
    }

    /**
     * @ManyToOne：外键在当前表
     */
    private JoinNode resolveManyToOne(Field field, TableMeta sourceMeta) {
        Class<?> targetClass = field.getType();
        TableMeta targetMeta = TableMeta.of(targetClass, field.getName());

        // 获取 @JoinColumn
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        String fkColumnName = joinColumn != null && !joinColumn.name().isEmpty()
                ? joinColumn.name()
                : field.getName() + "_id";

        ColumnMeta leftCol = ColumnMeta.of(sourceMeta, fkColumnName, Long.class);
        ColumnMeta rightCol = ColumnMeta.of(targetMeta, "id", Long.class);

        return new JoinNode(targetMeta, JoinType.LEFT, new JoinCondition(leftCol, rightCol));
    }

    /**
     * @OneToMany：外键在对方表
     */
    private JoinNode resolveOneToMany(Field field, TableMeta sourceMeta) {
        OneToMany annotation = field.getAnnotation(OneToMany.class);
        Class<?> targetClass = getCollectionGenericType(field);
        if (targetClass == null) return null;

        TableMeta targetMeta = TableMeta.of(targetClass, field.getName());

        String mappedBy = annotation.mappedBy();
        String fkColumnName = mappedBy.isEmpty() ? sourceMeta.tableName() + "_id" : mappedBy + "_id";

        ColumnMeta leftCol = ColumnMeta.of(sourceMeta, "id", Long.class);
        ColumnMeta rightCol = ColumnMeta.of(targetMeta, fkColumnName, Long.class);

        return new JoinNode(targetMeta, JoinType.LEFT, new JoinCondition(leftCol, rightCol));
    }

    /**
     * @ManyToMany：中间表
     */
    private JoinNode resolveManyToMany(Field field, TableMeta sourceMeta) {
        Class<?> targetClass = getCollectionGenericType(field);
        if (targetClass == null) return null;

        TableMeta targetMeta = TableMeta.of(targetClass, field.getName());

        // 简化处理：默认以 source_id → target_id 连接
        ColumnMeta leftCol = ColumnMeta.of(sourceMeta, "id", Long.class);
        ColumnMeta rightCol = ColumnMeta.of(targetMeta, "id", Long.class);

        return new JoinNode(targetMeta, JoinType.LEFT, new JoinCondition(leftCol, rightCol));
    }

    /**
     * @OneToOne
     */
    private JoinNode resolveOneToOne(Field field, TableMeta sourceMeta) {
        Class<?> targetClass = field.getType();
        TableMeta targetMeta = TableMeta.of(targetClass, field.getName());

        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        String fkColumnName = joinColumn != null && !joinColumn.name().isEmpty()
                ? joinColumn.name()
                : field.getName() + "_id";

        ColumnMeta leftCol = ColumnMeta.of(sourceMeta, fkColumnName, Long.class);
        ColumnMeta rightCol = ColumnMeta.of(targetMeta, "id", Long.class);

        return new JoinNode(targetMeta, JoinType.LEFT, new JoinCondition(leftCol, rightCol));
    }

    private Class<?> getCollectionGenericType(Field field) {
        if (Collection.class.isAssignableFrom(field.getType())) {
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType paramType) {
                Type[] typeArgs = paramType.getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> clazz) {
                    return clazz;
                }
            }
        }
        return null;
    }
}

