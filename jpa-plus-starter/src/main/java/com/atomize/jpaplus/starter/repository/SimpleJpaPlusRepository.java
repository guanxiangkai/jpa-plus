package com.atomize.jpaplus.starter.repository;

import com.atomize.jpaplus.core.exception.JpaPlusException;
import com.atomize.jpaplus.core.executor.JpaPlusExecutor;
import com.atomize.jpaplus.core.model.DataInvocation;
import com.atomize.jpaplus.core.model.OperationType;
import com.atomize.jpaplus.query.executor.QueryExecutor;
import com.atomize.jpaplus.query.pagination.PageResult;
import com.atomize.jpaplus.query.wrapper.DeleteWrapper;
import com.atomize.jpaplus.query.wrapper.QueryWrapper;
import com.atomize.jpaplus.query.wrapper.UpdateWrapper;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * JPA-Plus Repository 默认实现
 * <p>
 * 继承 SimpleJpaRepository 保留所有 JPA 原生功能，
 * 同时委托 JpaPlusExecutor 和 QueryExecutor 实现增强查询。
 */
public class SimpleJpaPlusRepository<T, ID>
        extends SimpleJpaRepository<T, ID>
        implements JpaPlusRepository<T, ID> {

    private final JpaPlusExecutor executor;
    private final QueryExecutor queryExecutor;
    private final JpaEntityInformation<T, ?> entityInformation;

    public SimpleJpaPlusRepository(JpaEntityInformation<T, ?> entityInformation,
                                   EntityManager entityManager,
                                   JpaPlusExecutor executor,
                                   QueryExecutor queryExecutor) {
        super(entityInformation, entityManager);
        this.entityInformation = entityInformation;
        this.executor = executor;
        this.queryExecutor = queryExecutor;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> list(QueryWrapper<T> wrapper) {
        try {
            DataInvocation invocation = new DataInvocation(
                    OperationType.QUERY,
                    null,
                    entityInformation.getJavaType(),
                    wrapper.buildContext()
            );
            return (List<T>) executor.execute(invocation);
        } catch (Throwable e) {
            throw new JpaPlusException("Query execution failed", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<T> one(QueryWrapper<T> wrapper) {
        List<T> results = list(wrapper.limit(0, 1));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public long count(QueryWrapper<T> wrapper) {
        return queryExecutor.count(wrapper);
    }

    @Override
    public PageResult<T> page(QueryWrapper<T> wrapper, Pageable pageable) {
        return queryExecutor.page(wrapper, pageable);
    }

    @Override
    public int update(UpdateWrapper<T> wrapper) {
        return queryExecutor.update(wrapper);
    }

    @Override
    public int delete(DeleteWrapper<T> wrapper) {
        return queryExecutor.delete(wrapper);
    }

    @Override
    public void debug(QueryWrapper<T> wrapper) {
        queryExecutor.debug(wrapper);
    }
}

