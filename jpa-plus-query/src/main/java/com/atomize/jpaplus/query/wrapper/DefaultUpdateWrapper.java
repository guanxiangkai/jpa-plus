package com.atomize.jpaplus.query.wrapper;

import com.atomize.jpaplus.query.context.Assignment;
import com.atomize.jpaplus.query.context.QueryContext;
import com.atomize.jpaplus.query.context.QueryMetadata;
import com.atomize.jpaplus.query.context.QueryType;
import com.atomize.jpaplus.query.metadata.ColumnMeta;
import com.atomize.jpaplus.query.metadata.JoinGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * 默认 UpdateWrapper 实现
 */
public class DefaultUpdateWrapper<T> extends AbstractWrapper<T> implements UpdateWrapper<T> {

    private final List<Assignment> assignments = new ArrayList<>();

    public DefaultUpdateWrapper(Class<T> entityClass) {
        super(entityClass);
    }

    @Override
    public UpdateWrapper<T> set(SFunction<T, ?> column, Object value) {
        ColumnMeta col = resolveColumn(column);
        assignments.add(new Assignment(col, value));
        return this;
    }

    @Override
    public UpdateWrapper<T> setNull(SFunction<T, ?> column) {
        ColumnMeta col = resolveColumn(column);
        assignments.add(new Assignment(col, null));
        return this;
    }

    @Override
    public QueryContext buildContext() {
        QueryMetadata metadata = new QueryMetadata(
                tableMeta,
                new JoinGraph(tableMeta),
                List.of(),
                QueryType.UPDATE,
                List.copyOf(assignments)
        );
        return new QueryContext(metadata, buildRuntime());
    }
}

