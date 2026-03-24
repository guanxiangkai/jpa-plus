package com.atomize.jpaplus.query.wrapper;

import com.atomize.jpaplus.query.context.QueryContext;
import com.atomize.jpaplus.query.context.QueryMetadata;

/**
 * 默认 QueryWrapper 实现
 */
public class DefaultQueryWrapper<T> extends AbstractWrapper<T> {

    public DefaultQueryWrapper(Class<T> entityClass) {
        super(entityClass);
    }

    @Override
    public QueryContext buildContext() {
        QueryMetadata metadata = QueryMetadata.select(tableMeta);
        return new QueryContext(metadata, buildRuntime());
    }
}

