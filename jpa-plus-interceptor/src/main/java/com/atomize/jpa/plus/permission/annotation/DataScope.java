package com.atomize.jpa.plus.permission.annotation;

import com.atomize.jpa.plus.permission.enums.DataScopeType;

import java.lang.annotation.*;

/**
 * 数据权限注解
 *
 * <p>标注在实体类或 Repository 方法上，声明该实体/查询的数据权限范围。
 * 框架通过 {@link com.atomize.jpa.plus.permission.interceptor.PermissionInterceptor PermissionInterceptor} 在查询前自动注入对应的 WHERE 条件。</p>
 *
 * <h3>使用示例</h3>
 *
 * <p><b>1. 标注在实体类上（全局生效）：</b></p>
 * <pre>{@code
 * @Entity
 * @DataScope(type = DataScopeType.DEPT_AND_CHILD)
 * public class Order {
 *     private Long deptId;
 *     private String createBy;
 * }
 * }</pre>
 *
 * <p><b>2. 标注在 Repository 方法上（方法级覆盖）：</b></p>
 * <pre>{@code
 * public interface OrderRepository extends JpaRepository<Order, Long> {
 *
 *     @DataScope(type = DataScopeType.SELF)
 *     List<Order> findByStatus(String status);
 * }
 * }</pre>
 *
 * <p><b>属性说明：</b></p>
 * <ul>
 *   <li>{@link #type()} —— 权限范围类型，默认 {@link DataScopeType#DEPT}</li>
 *   <li>{@link #deptColumn()} —— 部门字段的数据库列名，默认 {@code "dept_id"}</li>
 *   <li>{@link #userColumn()} —— 创建人字段的数据库列名，默认 {@code "create_by"}（{@code SELF} 模式使用）</li>
 * </ul>
 *
 * @author guanxiangkai
 * @see DataScopeType
 * @since 2026年03月25日 星期二
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 数据权限范围类型（默认本部门）
     */
    DataScopeType type() default DataScopeType.DEPT;

    /**
     * 部门字段列名（默认 "dept_id"）
     *
     * <p>用于 {@link DataScopeType#DEPT} 和 {@link DataScopeType#DEPT_AND_CHILD} 模式，
     * 拦截器会追加 {@code dept_id = :deptId} 或 {@code dept_id IN (:deptIds)} 条件。</p>
     */
    String deptColumn() default "dept_id";

    /**
     * 创建人字段列名（默认 "create_by"）
     *
     * <p>用于 {@link DataScopeType#SELF} 模式，
     * 拦截器会追加 {@code create_by = :userId} 条件。</p>
     */
    String userColumn() default "create_by";
}

