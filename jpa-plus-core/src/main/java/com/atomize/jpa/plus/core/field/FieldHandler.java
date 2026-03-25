package com.atomize.jpa.plus.core.field;

import java.lang.reflect.Field;

/**
 * 字段处理器接口
 *
 * <p>对实体字段进行增强处理的 SPI 扩展点。框架内置以下实现：
 * <ul>
 *   <li>EncryptFieldHandler —— 字段加密/解密</li>
 *   <li>DictFieldHandler —— 字典标签回写</li>
 *   <li>DesensitizeFieldHandler —— 字段脱敏</li>
 *   <li>SensitiveWordHandler —— 敏感词检测</li>
 *   <li>VersionFieldHandler —— 乐观锁版本自增</li>
 *   <li>LogicDeleteFieldHandler —— 逻辑删除标记</li>
 * </ul>
 * </p>
 *
 * <p>触发时机：
 * <ul>
 *   <li>{@link #beforeSave} —— 在 {@code EntityManager.persist/merge} 之前</li>
 *   <li>{@link #afterQuery} —— 在查询结果返回给调用方之前</li>
 * </ul>
 * </p>
 *
 * <p><b>设计模式：</b>策略模式（Strategy） —— 每个实现封装一种字段处理策略</p>
 *
 * @author guanxiangkai
 * @see FieldEngine
 * @since 2026年03月25日 星期三
 */
public interface FieldHandler {

    /**
     * 排序值，值越小越优先执行
     *
     * @return 排序优先级
     */
    int order();

    /**
     * 判断是否支持处理指定字段（通常检测字段上的注解）
     *
     * @param field 实体字段
     * @return 支持返回 {@code true}
     */
    boolean supports(Field field);

    /**
     * 保存前处理（如加密、敏感词检测、版本自增）
     *
     * @param entity 实体对象
     * @param field  待处理字段
     */
    default void beforeSave(Object entity, Field field) {
        // 默认空实现
    }

    /**
     * 查询后处理（如解密、字典回写、脱敏）
     *
     * @param entity 实体对象
     * @param field  待处理字段
     */
    default void afterQuery(Object entity, Field field) {
        // 默认空实现
    }
}
