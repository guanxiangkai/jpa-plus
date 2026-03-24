package com.atomize.jpaplus.query.compiler;

import com.atomize.jpaplus.query.context.QueryContext;

/**
 * SQL 编译器接口
 *
 * <p>将 {@link QueryContext}（包含条件 AST、排序、分页等信息）编译为可执行的原生 SQL。
 * 框架内置以下方言实现：
 * <ul>
 *   <li>{@link MySqlCompiler} —— MySQL 方言</li>
 *   <li>{@link PgSqlCompiler} —— PostgreSQL 方言</li>
 *   <li>{@link DebugSqlCompiler} —— 装饰器，输出调试 SQL</li>
 * </ul>
 * </p>
 *
 * <p><b>设计模式：</b>策略模式（Strategy） —— 不同数据库方言对应不同编译策略</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
public interface SqlCompiler {

    /**
     * 编译查询上下文为 SQL
     *
     * @param context 查询上下文（包含 AST、参数、排序、分页等）
     * @return SQL 编译结果（包含 SQL 语句和命名参数映射）
     */
    SqlResult compile(QueryContext context);
}
