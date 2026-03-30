package com.actomize.jpa.plus.query.compiler;

import com.actomize.jpa.plus.query.context.QueryContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 调试 SQL 编译器（装饰器）
 *
 * <p>包装任意 {@link SqlCompiler} 实现，在编译后通过日志输出 SQL 和参数，
 * 便于开发调试。通过配置 {@code jpa-plus.debug.enabled=true} 启用。</p>
 *
 * <p><b>设计模式：</b>装饰器模式（Decorator） —— 在不修改原编译器的前提下增强其行为</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Slf4j
@RequiredArgsConstructor
public class DebugSqlCompiler implements SqlCompiler {

    private final SqlCompiler delegate;
    private final boolean printParams;

    public DebugSqlCompiler(SqlCompiler delegate) {
        this(delegate, true);
    }

    @Override
    public SqlResult compile(QueryContext context) {
        SqlResult result = delegate.compile(context);
        log.info("==> SQL: {}", result.sql());
        if (printParams) {
            log.info("==> Params: {}", result.params());
        }
        return result;
    }
}
