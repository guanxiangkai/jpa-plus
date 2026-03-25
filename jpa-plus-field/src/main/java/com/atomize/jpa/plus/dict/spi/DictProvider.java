package com.atomize.jpa.plus.dict.spi;

import com.atomize.jpa.plus.dict.model.DictItem;

import java.util.List;
import java.util.Optional;

/**
 * 字典数据提供者 SPI 接口
 *
 * <p>用户实现此接口提供字典数据获取能力，框架不关心数据来源 ——
 * 可以直接查数据库、走 Redis 缓存、本地缓存、三级缓存等，由用户自行决定。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @Component
 * public class MyDictProvider implements DictProvider {
 *
 *     @Override
 *     public List<DictItem> getItems(String dictCode) {
 *         // 从数据库/缓存获取字典项列表
 *         return dictMapper.selectByCode(dictCode);
 *     }
 * }
 * }</pre>
 *
 * <p><b>设计模式：</b>策略模式（Strategy） —— 解耦字典数据获取与字典翻译</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期二
 */
public interface DictProvider {

    /**
     * 根据字典类型编码获取该类型下的所有字典项
     *
     * <p>这是用户唯一需要实现的方法。框架会基于返回的列表完成
     * value → label 的翻译。数据来源完全由用户控制。</p>
     *
     * @param dictCode 字典类型编码（如 {@code "sys_user_sex"}）
     * @return 字典项列表，不存在时返回空列表
     */
    List<DictItem> getItems(String dictCode);

    /**
     * 根据字典类型和值获取标签
     *
     * <p>默认实现基于 {@link #getItems(String)} 进行匹配，
     * 用户一般不需要覆盖此方法。</p>
     *
     * @param dictCode 字典类型编码
     * @param value    字典值
     * @return 字典标签，不存在时返回 {@link Optional#empty()}
     */
    default Optional<String> getLabel(String dictCode, Object value) {
        if (value == null) {
            return Optional.empty();
        }
        String strValue = String.valueOf(value);
        return getItems(dictCode).stream()
                .filter(item -> strValue.equals(item.value()))
                .map(DictItem::label)
                .findFirst();
    }
}
