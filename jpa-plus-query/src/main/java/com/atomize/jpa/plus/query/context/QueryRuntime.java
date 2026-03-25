package com.atomize.jpa.plus.query.context;

import com.atomize.jpa.plus.query.ast.Condition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询运行时（不可变值对象）
 *
 * <p>使用 Java {@code record} 保证不可变性。
 * 拦截器可通过 {@code with*} 方法创建修改后的新实例（Wither 模式）。</p>
 *
 * <p><b>设计模式：</b>不可变值对象模式（Immutable Value Object）</p>
 *
 * @param where      WHERE 条件 AST
 * @param orderBys   排序列表
 * @param parameters 额外参数
 * @param offset     分页偏移量
 * @param rows       分页行数
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
public record QueryRuntime(
        Condition where,
        List<OrderBy> orderBys,
        Map<String, Object> parameters,
        Integer offset,
        Integer rows
) {

    public QueryRuntime() {
        this(null, List.of(), Map.of(), null, null);
    }

    public QueryRuntime(Condition where, List<OrderBy> orderBys, Map<String, Object> parameters,
                        Integer offset, Integer rows) {
        this.where = where;
        this.orderBys = List.copyOf(orderBys);
        this.parameters = Map.copyOf(parameters);
        this.offset = offset;
        this.rows = rows;
    }

    public QueryRuntime withWhere(Condition where) {
        return new QueryRuntime(where, orderBys, parameters, offset, rows);
    }

    public QueryRuntime withParameter(String key, Object value) {
        Map<String, Object> newParams = new LinkedHashMap<>(parameters);
        newParams.put(key, value);
        return new QueryRuntime(where, orderBys, newParams, offset, rows);
    }

    public QueryRuntime withOrderBys(List<OrderBy> orderBys) {
        return new QueryRuntime(where, orderBys, parameters, offset, rows);
    }

    public QueryRuntime withLimit(Integer offset, Integer rows) {
        return new QueryRuntime(where, orderBys, parameters, offset, rows);
    }

    // ─── 向后兼容的 getter 别名（record 默认方法名为 where() 等，保留 getXxx 以兼容已有调用） ───

    public Condition getWhere() {
        return where;
    }

    public List<OrderBy> getOrderBys() {
        return orderBys;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public Integer getOffset() {
        return offset;
    }

    public Integer getRows() {
        return rows;
    }
}

