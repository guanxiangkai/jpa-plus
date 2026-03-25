package com.atomize.jpa.plus.query.compiler;

import com.atomize.jpa.plus.query.ast.*;
import com.atomize.jpa.plus.query.context.Assignment;
import com.atomize.jpa.plus.query.context.QueryContext;
import com.atomize.jpa.plus.query.context.QueryRuntime;
import com.atomize.jpa.plus.query.context.QueryType;
import com.atomize.jpa.plus.query.metadata.JoinNode;
import com.atomize.jpa.plus.query.metadata.TableMeta;
import com.atomize.jpa.plus.query.normalizer.ConditionNormalizer;
import com.atomize.jpa.plus.query.normalizer.ParameterNamingStrategy;
import com.atomize.jpa.plus.query.wrapper.SelectColumn;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SQL 编译器抽象基类（模板方法模式）
 *
 * <p>封装所有 SQL 方言共享的编译逻辑（SELECT/UPDATE/DELETE 构建、FROM/JOIN 构建、
 * WHERE/ORDER BY 构建、条件 AST 访问者实现），子类仅需实现方言差异部分。</p>
 *
 * <p>编译过程中使用局部变量 {@code sql} 和 {@code params}，
 * 而非实例字段，保证线程安全（单例 Bean 可安全用于并发编译）。</p>
 *
 * <p><b>设计模式：</b>
 * <ul>
 *   <li>模板方法模式（Template Method） —— 定义编译骨架，子类覆盖方言差异</li>
 *   <li>访问者模式（Visitor） —— 遍历条件 AST 生成 SQL 片段</li>
 * </ul>
 * </p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
public abstract class AbstractSqlCompiler implements SqlCompiler {

    @Override
    public SqlResult compile(QueryContext ctx) {
        // 使用局部变量，保证线程安全
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new LinkedHashMap<>();
        ConditionNormalizer normalizer = new ConditionNormalizer();

        try {
            ParameterNamingStrategy.begin();

            // 0. 规范化条件
            Condition normalizedWhere = ctx.runtime().getWhere() != null
                    ? ctx.runtime().getWhere().accept(normalizer)
                    : null;
            QueryRuntime newRuntime = ctx.runtime().withWhere(normalizedWhere);
            QueryContext normalizedCtx = ctx.withRuntime(newRuntime);

            // 1. 构建语句头部
            switch (normalizedCtx.type()) {
                case SELECT -> buildSelect(sql, normalizedCtx);
                case UPDATE -> buildUpdate(sql, normalizedCtx);
                case DELETE -> buildDelete(sql, normalizedCtx);
            }

            // 2. 处理 FROM / JOIN
            buildFrom(sql, normalizedCtx);

            // 3. 处理 SET（UPDATE）
            if (normalizedCtx.type() == QueryType.UPDATE) {
                buildSet(sql, params, normalizedCtx);
            }

            // 4. 处理 WHERE
            if (normalizedCtx.runtime().getWhere() != null) {
                sql.append(" WHERE ");
                visitCondition(sql, params, normalizedCtx.runtime().getWhere());
            }

            // 5. 处理 ORDER BY
            if (!normalizedCtx.runtime().getOrderBys().isEmpty()) {
                sql.append(" ORDER BY ");
                String orderClause = normalizedCtx.runtime().getOrderBys().stream()
                        .map(ob -> ob.column().qualifiedName() + " " + ob.direction().name())
                        .collect(Collectors.joining(", "));
                sql.append(orderClause);
            }

            // 6. 处理 LIMIT（方言差异点 —— 由子类实现）
            appendLimit(sql, normalizedCtx.runtime().getOffset(), normalizedCtx.runtime().getRows());

            return new SqlResult(sql.toString(), Map.copyOf(params));
        } finally {
            ParameterNamingStrategy.end();
        }
    }

    // ─────────── 方言差异扩展点 ───────────

    /**
     * 追加分页语法（子类必须实现）
     *
     * @param sql    SQL 构建器
     * @param offset 偏移量（可能为 {@code null}）
     * @param rows   行数（可能为 {@code null}）
     */
    protected abstract void appendLimit(StringBuilder sql, Integer offset, Integer rows);

    /**
     * 创建当前方言的新编译器实例（用于子查询编译）
     */
    protected abstract AbstractSqlCompiler newInstance();

    // ─────────── SQL 构建方法 ───────────

    private void buildSelect(StringBuilder sql, QueryContext ctx) {
        sql.append("SELECT ");
        List<SelectColumn> selects = ctx.metadata().selects();
        if (selects == null || selects.isEmpty()) {
            sql.append(ctx.metadata().root().alias()).append(".*");
        } else {
            String cols = selects.stream()
                    .map(sc -> {
                        String qn = sc.column().qualifiedName();
                        String alias = sc.column().alias();
                        if (alias != null && !alias.equals(sc.column().columnName())) {
                            return qn + " AS " + alias;
                        }
                        return qn;
                    })
                    .collect(Collectors.joining(", "));
            sql.append(cols);
        }
    }

    private void buildUpdate(StringBuilder sql, QueryContext ctx) {
        sql.append("UPDATE ");
        TableMeta root = ctx.metadata().root();
        sql.append(root.tableName()).append(" ").append(root.alias());
    }

    private void buildDelete(StringBuilder sql, QueryContext ctx) {
        sql.append("DELETE ");
        sql.append(ctx.metadata().root().alias());
    }

    private void buildFrom(StringBuilder sql, QueryContext ctx) {
        TableMeta root = ctx.metadata().root();
        if (ctx.type() != QueryType.UPDATE) {
            sql.append(" FROM ").append(root.tableName()).append(" ").append(root.alias());
        }

        // JOIN
        if (ctx.metadata().joinGraph() != null && ctx.metadata().joinGraph().hasJoins()) {
            for (JoinNode node : ctx.metadata().joinGraph().nodes()) {
                sql.append(" ").append(node.type().name()).append(" JOIN ");
                sql.append(node.table().tableName()).append(" ").append(node.table().alias());
                sql.append(" ON ");
                sql.append(node.condition().left().qualifiedName());
                sql.append(" = ");
                sql.append(node.condition().right().qualifiedName());
            }
        }
    }

    private void buildSet(StringBuilder sql, Map<String, Object> params, QueryContext ctx) {
        List<Assignment> assignments = ctx.metadata().assignments();
        if (assignments != null && !assignments.isEmpty()) {
            sql.append(" SET ");
            String setClauses = assignments.stream()
                    .map(a -> {
                        if (a.value() == null) {
                            return a.column().qualifiedName() + " = NULL";
                        }
                        String paramName = ParameterNamingStrategy.next("set");
                        params.put(paramName, a.value());
                        return a.column().qualifiedName() + " = :" + paramName;
                    })
                    .collect(Collectors.joining(", "));
            sql.append(setClauses);
        }
    }

    // ─────────── 条件访问者（线程安全的局部访问） ───────────

    private void visitCondition(StringBuilder sql, Map<String, Object> params, Condition condition) {
        switch (condition) {
            case Eq eq -> {
                String p = ParameterNamingStrategy.next("eq");
                sql.append(eq.column().qualifiedName()).append(" = :").append(p);
                params.put(p, eq.value());
            }
            case Ne ne -> {
                String p = ParameterNamingStrategy.next("ne");
                sql.append(ne.column().qualifiedName()).append(" <> :").append(p);
                params.put(p, ne.value());
            }
            case Gt gt -> {
                String p = ParameterNamingStrategy.next("gt");
                sql.append(gt.column().qualifiedName()).append(" > :").append(p);
                params.put(p, gt.value());
            }
            case Ge ge -> {
                String p = ParameterNamingStrategy.next("ge");
                sql.append(ge.column().qualifiedName()).append(" >= :").append(p);
                params.put(p, ge.value());
            }
            case Lt lt -> {
                String p = ParameterNamingStrategy.next("lt");
                sql.append(lt.column().qualifiedName()).append(" < :").append(p);
                params.put(p, lt.value());
            }
            case Le le -> {
                String p = ParameterNamingStrategy.next("le");
                sql.append(le.column().qualifiedName()).append(" <= :").append(p);
                params.put(p, le.value());
            }
            case Like like -> {
                String p = ParameterNamingStrategy.next("like");
                sql.append(like.column().qualifiedName()).append(" LIKE :").append(p);
                params.put(p, like.pattern());
            }
            case In in -> {
                String p = ParameterNamingStrategy.next("in");
                sql.append(in.column().qualifiedName()).append(" IN (:").append(p).append(")");
                params.put(p, in.values());
            }
            case Between between -> {
                String sp = ParameterNamingStrategy.next("between_start");
                String ep = ParameterNamingStrategy.next("between_end");
                sql.append(between.column().qualifiedName())
                        .append(" BETWEEN :").append(sp).append(" AND :").append(ep);
                params.put(sp, between.start());
                params.put(ep, between.end());
            }
            case And and -> {
                sql.append("(");
                List<Condition> conditions = and.conditions();
                for (int i = 0; i < conditions.size(); i++) {
                    if (i > 0) sql.append(" AND ");
                    visitCondition(sql, params, conditions.get(i));
                }
                sql.append(")");
            }
            case Or or -> {
                sql.append("(");
                List<Condition> conditions = or.conditions();
                for (int i = 0; i < conditions.size(); i++) {
                    if (i > 0) sql.append(" OR ");
                    visitCondition(sql, params, conditions.get(i));
                }
                sql.append(")");
            }
            case Not not -> {
                sql.append("NOT (");
                visitCondition(sql, params, not.condition());
                sql.append(")");
            }
            case Exists exists -> {
                sql.append("EXISTS (");
                SqlResult subResult = newInstance().compile(exists.subQuery().query());
                sql.append(subResult.sql());
                params.putAll(subResult.params());
                sql.append(")");
            }
            case SubQuery subQuery -> {
                sql.append("(");
                SqlResult subResult = newInstance().compile(subQuery.query());
                sql.append(subResult.sql());
                params.putAll(subResult.params());
                sql.append(")");
            }
        }
    }
}

