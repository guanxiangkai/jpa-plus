package com.actomize.jpa.plus.query.wrapper;

/**
 * 更新条件构造器接口
 */
public interface UpdateWrapper<T> extends QueryWrapper<T> {

    /**
     * 静态工厂方法
     */
    static <T> UpdateWrapper<T> from(Class<T> entityClass) {
        return new DefaultUpdateWrapper<>(entityClass);
    }

    /**
     * 设置字段值
     */
    UpdateWrapper<T> set(SFunction<T, ?> column, Object value);

    /**
     * 设置字段为 NULL
     */
    UpdateWrapper<T> setNull(SFunction<T, ?> column);
}

