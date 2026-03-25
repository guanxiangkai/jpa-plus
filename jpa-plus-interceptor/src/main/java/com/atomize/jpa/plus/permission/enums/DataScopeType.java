package com.atomize.jpa.plus.permission.enums;

/**
 * 数据权限范围枚举
 *
 * <p>定义行级数据权限的五种常见控制粒度：
 * <ul>
 *   <li>{@link #ALL} —— 全部数据，不做任何过滤</li>
 *   <li>{@link #DEPT} —— 仅本部门数据</li>
 *   <li>{@link #DEPT_AND_CHILD} —— 本部门及其子部门数据</li>
 *   <li>{@link #SELF} —— 仅自己创建的数据</li>
 *   <li>{@link #CUSTOM} —— 自定义数据权限，由 {@code DataScopeHandler} 实现方自行构建条件</li>
 * </ul>
 * </p>
 *
 * <p><b>设计模式：</b>策略枚举（Strategy Enum） —— 每种范围对应一种过滤策略</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期二
 */
public enum DataScopeType {

    /**
     * 全部数据 —— 不追加任何权限条件
     */
    ALL,

    /**
     * 本部门数据 —— 追加 {@code dept_id = :currentDeptId}
     */
    DEPT,

    /**
     * 本部门及子部门数据 —— 追加 {@code dept_id IN (:deptIds)}
     */
    DEPT_AND_CHILD,

    /**
     * 仅自己的数据 —— 追加 {@code create_by = :currentUserId}
     */
    SELF,

    /**
     * 自定义权限 —— 由 {@code DataScopeHandler#customCondition} 完全控制
     */
    CUSTOM
}

