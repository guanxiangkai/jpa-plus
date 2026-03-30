package com.actomize.jpa.plus.query.wrapper;

/**
 * 删除条件构造器接口
 */
public interface DeleteWrapper<T> extends QueryWrapper<T> {

    /**
     * 静态工厂方法
     */
    static <T> DeleteWrapper<T> from(Class<T> entityClass) {
        return new DefaultDeleteWrapper<>(entityClass);
    }
}

