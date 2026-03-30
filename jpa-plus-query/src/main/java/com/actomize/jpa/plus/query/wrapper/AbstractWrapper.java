package com.actomize.jpa.plus.query.wrapper;

import com.actomize.jpa.plus.query.ast.*;
import com.actomize.jpa.plus.query.context.OrderBy;
import com.actomize.jpa.plus.query.context.QueryRuntime;
import com.actomize.jpa.plus.query.metadata.ColumnMeta;
import com.actomize.jpa.plus.query.metadata.TableMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * QueryWrapper 抽象基类
 * <p>
 * 实现通用的条件构建、排序、分页逻辑。
 *
 * @param <T> 实体类型
 */
public abstract class AbstractWrapper<T> implements QueryWrapper<T> {

    protected final Class<T> entityClass;
    protected final TableMeta tableMeta;
    protected final List<Condition> conditions = new ArrayList<>();
    protected final List<OrderBy> orderBys = new ArrayList<>();
    protected Integer offset;
    protected Integer rows;

    protected AbstractWrapper(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.tableMeta = TableMeta.of(entityClass);
    }

    @Override
    public Class<T> getEntityClass() {
        return entityClass;
    }

    @Override
    public QueryWrapper<T> eq(SFunction<T, ?> column, Object value) {
        conditions.add(new Eq(resolveColumn(column), value));
        return this;
    }

    @Override
    public QueryWrapper<T> ne(SFunction<T, ?> column, Object value) {
        conditions.add(new Ne(resolveColumn(column), value));
        return this;
    }

    @Override
    public QueryWrapper<T> gt(SFunction<T, ?> column, Object value) {
        conditions.add(new Gt(resolveColumn(column), value));
        return this;
    }

    @Override
    public QueryWrapper<T> ge(SFunction<T, ?> column, Object value) {
        conditions.add(new Ge(resolveColumn(column), value));
        return this;
    }

    @Override
    public QueryWrapper<T> lt(SFunction<T, ?> column, Object value) {
        conditions.add(new Lt(resolveColumn(column), value));
        return this;
    }

    @Override
    public QueryWrapper<T> le(SFunction<T, ?> column, Object value) {
        conditions.add(new Le(resolveColumn(column), value));
        return this;
    }

    @Override
    public QueryWrapper<T> like(SFunction<T, ?> column, String value) {
        conditions.add(new Like(resolveColumn(column), value, LikeMode.ANYWHERE));
        return this;
    }

    @Override
    public QueryWrapper<T> in(SFunction<T, ?> column, Collection<?> values) {
        conditions.add(new In(resolveColumn(column), values));
        return this;
    }

    @Override
    public QueryWrapper<T> between(SFunction<T, ?> column, Object start, Object end) {
        conditions.add(new Between(resolveColumn(column), start, end));
        return this;
    }

    @Override
    public QueryWrapper<T> and(Consumer<QueryWrapper<T>> consumer) {
        DefaultQueryWrapper<T> nested = new DefaultQueryWrapper<>(entityClass);
        consumer.accept(nested);
        Condition nestedCondition = nested.buildCondition();
        if (nestedCondition != null) {
            conditions.add(nestedCondition);
        }
        return this;
    }

    @Override
    public QueryWrapper<T> or(Consumer<QueryWrapper<T>> consumer) {
        DefaultQueryWrapper<T> nested = new DefaultQueryWrapper<>(entityClass);
        consumer.accept(nested);
        Condition nestedCondition = nested.buildCondition();
        if (nestedCondition != null) {
            conditions.add(new Or(List.of(nestedCondition)));
        }
        return this;
    }

    @Override
    public QueryWrapper<T> condition(Condition condition) {
        if (condition != null) {
            conditions.add(condition);
        }
        return this;
    }

    @Override
    public QueryWrapper<T> exists(SubQuery subQuery) {
        conditions.add(new Exists(subQuery));
        return this;
    }

    @Override
    public QueryWrapper<T> orderByAsc(SFunction<T, ?> column) {
        orderBys.add(new OrderBy(resolveColumn(column), OrderBy.Direction.ASC));
        return this;
    }

    @Override
    public QueryWrapper<T> orderByDesc(SFunction<T, ?> column) {
        orderBys.add(new OrderBy(resolveColumn(column), OrderBy.Direction.DESC));
        return this;
    }

    @Override
    public QueryWrapper<T> limit(int offset, int rows) {
        this.offset = offset;
        this.rows = rows;
        return this;
    }

    @Override
    public Condition buildCondition() {
        if (conditions.isEmpty()) {
            return null;
        }
        if (conditions.size() == 1) {
            return conditions.getFirst();
        }
        return new And(List.copyOf(conditions));
    }

    /**
     * 解析 Lambda 为 ColumnMeta
     */
    protected ColumnMeta resolveColumn(SFunction<T, ?> column) {
        return LambdaColumnResolver.resolveColumn(column, tableMeta);
    }

    /**
     * 构建查询运行时
     */
    protected QueryRuntime buildRuntime() {
        return new QueryRuntime(
                buildCondition(),
                List.copyOf(orderBys),
                Map.of(),
                offset,
                rows
        );
    }
}

