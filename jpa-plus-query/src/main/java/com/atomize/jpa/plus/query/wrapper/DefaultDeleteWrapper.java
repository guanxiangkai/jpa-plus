package com.atomize.jpa.plus.query.wrapper;

import com.atomize.jpa.plus.query.context.QueryContext;
import com.atomize.jpa.plus.query.context.QueryMetadata;

/**
 * 默认 DeleteWrapper 实现
 */
public class DefaultDeleteWrapper<T> extends AbstractWrapper<T> implements DeleteWrapper<T> {

    public DefaultDeleteWrapper(Class<T> entityClass) {
        super(entityClass);
    }

    @Override
    public QueryContext buildContext() {
        QueryMetadata metadata = QueryMetadata.delete(tableMeta);
        return new QueryContext(metadata, buildRuntime());
    }
}

