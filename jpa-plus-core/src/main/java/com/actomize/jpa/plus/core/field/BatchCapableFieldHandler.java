package com.actomize.jpa.plus.core.field;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 批处理能力标记接口
 *
 * <p>实现此接口的 {@link FieldHandler} 声明自身已提供优化的批量处理实现，
 * 框架 {@link FieldEngine} 通过 {@code instanceof BatchCapableFieldHandler} 零开销检测，
 * 替代原有的反射探测方案。</p>
 *
 * <h3>设计动机</h3>
 * <ul>
 *   <li><b>性能</b>：消除每次 {@code supportsBatchProcessing()} 的反射调用（即使有缓存，
 *       首次调用仍有成本；标记接口完全零开销）</li>
 *   <li><b>明确性</b>：实现类通过 {@code implements BatchCapableFieldHandler} 显式宣告能力，
 *       不再依赖"是否覆盖了默认方法"这一脆弱约定</li>
 *   <li><b>类型安全</b>：编译期即可发现实现不一致问题</li>
 * </ul>
 *
 * <h3>使用规范</h3>
 * <p>实现此接口时，必须同时覆盖 {@link FieldHandler#beforeSaveBatch} 或
 * {@link FieldHandler#afterQueryBatch}（或两者）以提供真正的批处理优化。
 * 否则仅继承默认的逐个遍历实现，声明此接口没有意义。</p>
 *
 * <pre>{@code
 * // 正确示例：声明批处理能力并提供真实实现
 * public class EncryptFieldHandler implements BatchCapableFieldHandler {
 *
 *     @Override
 *     public void beforeSaveBatch(List<?> entities, Field field) {
 *         // 批量加密，避免逐条 JCE 调用开销
 *     }
 *
 *     @Override
 *     public void afterQueryBatch(List<?> entities, Field field) {
 *         // 批量解密
 *     }
 * }
 * }</pre>
 *
 * @author guanxiangkai
 * @see FieldHandler
 * @see FieldEngine
 * @since 2026年04月17日（v4.1 — 替代反射检测方案）
 */
public interface BatchCapableFieldHandler extends FieldHandler {

    /**
     * {@inheritDoc}
     *
     * <p><b>实现要求</b>：实现此方法应提供真正的批量优化逻辑，
     * 而非简单地循环调用 {@link #beforeSave(Object, Field)}。</p>
     *
     * @param entities 实体列表（非空，至少 1 个元素）
     * @param field    待处理字段
     */
    @Override
    void beforeSaveBatch(List<?> entities, Field field);

    /**
     * {@inheritDoc}
     *
     * <p><b>实现要求</b>：实现此方法应提供真正的批量优化逻辑，
     * 而非简单地循环调用 {@link #afterQuery(Object, Field)}。</p>
     *
     * @param entities 实体列表（非空，至少 1 个元素）
     * @param field    待处理字段
     */
    @Override
    void afterQueryBatch(List<?> entities, Field field);
}
