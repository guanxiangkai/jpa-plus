package com.actomize.jpa.plus.starter.repository;

import com.actomize.jpa.plus.core.exception.JpaPlusException;
import com.actomize.jpa.plus.core.executor.JpaPlusExecutor;
import com.actomize.jpa.plus.core.model.QueryInvocation;
import com.actomize.jpa.plus.query.executor.MutationExecutor;
import com.actomize.jpa.plus.query.executor.QueryExecutor;
import com.actomize.jpa.plus.query.pagination.PageResult;
import com.actomize.jpa.plus.query.wrapper.DeleteWrapper;
import com.actomize.jpa.plus.query.wrapper.QueryWrapper;
import com.actomize.jpa.plus.query.wrapper.UpdateWrapper;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * JPA-Plus Repository 默认实现
 * <p>
 * 继承 SimpleJpaRepository 保留所有 JPA 原生功能，
 * 同时委托 JpaPlusExecutor 和 QueryExecutor 实现增强查询。
 */
class SimpleJpaPlusRepository<T, ID>
        extends SimpleJpaRepository<T, ID>
        implements JpaPlusRepository<T, ID> {

    private final JpaPlusExecutor executor;
    private final QueryExecutor queryExecutor;
    private final MutationExecutor mutationExecutor;
    private final JpaEntityInformation<T, ?> entityInformation;

    SimpleJpaPlusRepository(JpaEntityInformation<T, ?> entityInformation,
                            EntityManager entityManager,
                            JpaPlusExecutor executor,
                            QueryExecutor queryExecutor,
                            MutationExecutor mutationExecutor) {
        super(entityInformation, entityManager);
        this.entityInformation = entityInformation;
        this.executor = executor;
        this.queryExecutor = queryExecutor;
        this.mutationExecutor = mutationExecutor;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> list(QueryWrapper<T> wrapper) {
        try {
            var invocation = new QueryInvocation(
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
    @Transactional
    public int update(UpdateWrapper<T> wrapper) {
        return mutationExecutor.update(wrapper);
    }

    @Override
    @Transactional
    public int updateBatch(List<UpdateWrapper<T>> wrappers) {
        return mutationExecutor.updateBatch(wrappers);
    }

    @Override
    @Transactional
    public int delete(DeleteWrapper<T> wrapper) {
        return mutationExecutor.delete(wrapper);
    }

    @Override
    @Transactional
    public int deleteBatch(List<DeleteWrapper<T>> wrappers) {
        return mutationExecutor.deleteBatch(wrappers);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Transactional
    public <S extends T> List<S> upsertBatch(List<S> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return (List<S>) mutationExecutor.upsertBatch((List<T>) entities, entityInformation.getJavaType());
    }

    @Override
    public Stream<T> stream(QueryWrapper<T> wrapper) {
        return queryExecutor.stream(wrapper);
    }

    @Override
    public void debug(QueryWrapper<T> wrapper) {
        queryExecutor.debug(wrapper);
    }
}
