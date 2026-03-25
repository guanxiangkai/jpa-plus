package com.atomize.jpa.plus.query.executor;

import com.atomize.jpa.plus.query.pagination.PageResult;
import com.atomize.jpa.plus.query.wrapper.DeleteWrapper;
import com.atomize.jpa.plus.query.wrapper.JoinWrapper;
import com.atomize.jpa.plus.query.wrapper.QueryWrapper;
import com.atomize.jpa.plus.query.wrapper.UpdateWrapper;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 查询执行器接口
 * <p>
 * 提供所有查询操作的统一入口。
 */
public interface QueryExecutor {

    /**
     * 查询列表
     */
    <T> List<T> list(QueryWrapper<T> wrapper);

    /**
     * 查询单条
     */
    <T> Optional<T> one(QueryWrapper<T> wrapper);

    /**
     * 查询总数
     */
    long count(QueryWrapper<?> wrapper);

    /**
     * 分页查询
     */
    <T> PageResult<T> page(QueryWrapper<T> wrapper, Pageable pageable);

    /**
     * 执行更新
     */
    int update(UpdateWrapper<?> wrapper);

    /**
     * 执行删除
     */
    int delete(DeleteWrapper<?> wrapper);

    /**
     * Join 查询列表
     */
    <R> List<R> list(JoinWrapper<?> wrapper, Class<R> resultType);

    /**
     * Join 查询单条
     */
    <R> Optional<R> one(JoinWrapper<?> wrapper, Class<R> resultType);

    /**
     * Join 分页查询
     */
    <R> PageResult<R> page(JoinWrapper<?> wrapper, Class<R> resultType, Pageable pageable);

    /**
     * 调试输出 SQL
     */
    void debug(QueryWrapper<?> wrapper);

    /**
     * 调试输出 Join SQL
     */
    void debug(JoinWrapper<?> wrapper);
}

