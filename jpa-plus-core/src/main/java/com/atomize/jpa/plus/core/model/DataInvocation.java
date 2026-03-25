package com.atomize.jpa.plus.core.model;

/**
 * 数据调用封装（不可变值对象）
 *
 * <p>描述一次数据操作的完整信息，贯穿整个拦截器链。
 * 使用 Java {@code record} 保证不可变性，拦截器若需修改查询模型，
 * 必须通过 {@link #withQueryModel(Object)} 创建新实例。</p>
 *
 * <p><b>设计模式：</b>不可变值对象模式（Immutable Value Object）</p>
 *
 * @param type        操作类型（{@link OperationType#QUERY} / {@link OperationType#SAVE} / {@link OperationType#DELETE}）
 * @param entity      保存/删除时的实体对象（查询时为 {@code null}）
 * @param entityClass 实体类型（用于元数据解析）
 * @param queryModel  查询模型，可以是 {@code QueryWrapper}、{@code JoinWrapper} 或 {@code QueryContext}
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
public record DataInvocation(
        OperationType type,
        Object entity,
        Class<?> entityClass,
        Object queryModel
) {

    /**
     * 替换 queryModel 生成新的 DataInvocation（不可变模式）
     */
    public DataInvocation withQueryModel(Object newQueryModel) {
        return new DataInvocation(type, entity, entityClass, newQueryModel);
    }
}
