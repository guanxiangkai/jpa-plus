package com.atomize.jpa.plus.query.executor;

import com.atomize.jpa.plus.query.compiler.SqlCompiler;
import com.atomize.jpa.plus.query.compiler.SqlResult;
import com.atomize.jpa.plus.query.context.QueryContext;
import com.atomize.jpa.plus.query.pagination.PageResult;
import com.atomize.jpa.plus.query.pagination.PaginationOptimizer;
import com.atomize.jpa.plus.query.wrapper.DeleteWrapper;
import com.atomize.jpa.plus.query.wrapper.JoinWrapper;
import com.atomize.jpa.plus.query.wrapper.QueryWrapper;
import com.atomize.jpa.plus.query.wrapper.UpdateWrapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 默认查询执行器
 * <p>
 * 通过 SqlCompiler 编译 SQL，使用 EntityManager.createNativeQuery() 执行。
 * 强制使用命名参数绑定，确保 SQL 注入安全。
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultQueryExecutor implements QueryExecutor {

    private final EntityManager entityManager;
    private final SqlCompiler sqlCompiler;
    private final PaginationOptimizer paginationOptimizer;

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> list(QueryWrapper<T> wrapper) {
        QueryContext ctx = wrapper.buildContext();
        SqlResult result = sqlCompiler.compile(ctx);
        Query query = createNativeQuery(result, wrapper.getEntityClass());
        return query.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> one(QueryWrapper<T> wrapper) {
        QueryWrapper<T> limited = wrapper.limit(0, 1);
        List<T> list = list(limited);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public long count(QueryWrapper<?> wrapper) {
        QueryContext ctx = wrapper.buildContext();
        QueryContext countCtx = paginationOptimizer.buildCountContext(ctx);
        SqlResult result = sqlCompiler.compile(countCtx);

        String countSql = "SELECT COUNT(*) FROM (" + result.sql() + ") _count_tmp";
        Query query = entityManager.createNativeQuery(countSql);
        bindParameters(query, result.params());

        Object count = query.getSingleResult();
        return ((Number) count).longValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> PageResult<T> page(QueryWrapper<T> wrapper, Pageable pageable) {
        // 1. 查询总数
        long total = count(wrapper);
        if (total == 0) {
            return PageResult.empty(pageable.getPageNumber() + 1, pageable.getPageSize());
        }

        // 2. 查询当前页数据
        QueryWrapper<T> paged = wrapper.limit((int) pageable.getOffset(), pageable.getPageSize());
        List<T> records = list(paged);

        return new PageResult<>(records, total, pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    @Override
    public int update(UpdateWrapper<?> wrapper) {
        QueryContext ctx = wrapper.buildContext();
        SqlResult result = sqlCompiler.compile(ctx);
        Query query = entityManager.createNativeQuery(result.sql());
        bindParameters(query, result.params());
        return query.executeUpdate();
    }

    @Override
    public int delete(DeleteWrapper<?> wrapper) {
        QueryContext ctx = wrapper.buildContext();
        SqlResult result = sqlCompiler.compile(ctx);
        Query query = entityManager.createNativeQuery(result.sql());
        bindParameters(query, result.params());
        return query.executeUpdate();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> List<R> list(JoinWrapper<?> wrapper, Class<R> resultType) {
        QueryContext ctx = wrapper.buildContext();
        SqlResult result = sqlCompiler.compile(ctx);
        Query query = createNativeQuery(result, resultType);
        return query.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Optional<R> one(JoinWrapper<?> wrapper, Class<R> resultType) {
        JoinWrapper<?> limited = wrapper.limit(0, 1);
        List<R> list = list(limited, resultType);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> PageResult<R> page(JoinWrapper<?> wrapper, Class<R> resultType, Pageable pageable) {
        // 1. 查询总数
        QueryContext ctx = wrapper.buildContext();
        QueryContext countCtx = paginationOptimizer.buildCountContext(ctx);
        SqlResult countResult = sqlCompiler.compile(countCtx);
        String countSql = "SELECT COUNT(*) FROM (" + countResult.sql() + ") _count_tmp";
        Query countQuery = entityManager.createNativeQuery(countSql);
        bindParameters(countQuery, countResult.params());
        long total = ((Number) countQuery.getSingleResult()).longValue();

        if (total == 0) {
            return PageResult.empty(pageable.getPageNumber() + 1, pageable.getPageSize());
        }

        // 2. 查询当前页数据
        JoinWrapper<?> paged = wrapper.limit((int) pageable.getOffset(), pageable.getPageSize());
        List<R> records = list(paged, resultType);

        return new PageResult<>(records, total, pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    @Override
    public void debug(QueryWrapper<?> wrapper) {
        QueryContext ctx = wrapper.buildContext();
        SqlResult result = sqlCompiler.compile(ctx);
        log.info("==> Debug SQL: {}", result.sql());
        log.info("==> Debug Params: {}", result.params());
    }

    @Override
    public void debug(JoinWrapper<?> wrapper) {
        QueryContext ctx = wrapper.buildContext();
        SqlResult result = sqlCompiler.compile(ctx);
        log.info("==> Debug SQL: {}", result.sql());
        log.info("==> Debug Params: {}", result.params());
    }

    // ─────────── 私有方法 ───────────

    private Query createNativeQuery(SqlResult result, Class<?> resultClass) {
        Query query = entityManager.createNativeQuery(result.sql(), resultClass);
        bindParameters(query, result.params());
        return query;
    }

    private void bindParameters(Query query, Map<String, Object> params) {
        params.forEach(query::setParameter);
    }
}

