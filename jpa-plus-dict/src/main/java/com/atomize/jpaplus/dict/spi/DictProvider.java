package com.atomize.jpaplus.dict.spi;

import java.util.Optional;

/**
 * 字典数据提供者 SPI 接口
 *
 * <p>框架不内置任何字典缓存，用户必须实现此接口提供字典翻译能力。</p>
 *
 * <p><b>设计模式：</b>SPI 服务发现模式</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
public interface DictProvider {

    /**
     * 根据字典类型和值获取标签
     *
     * @param type  字典类型编码
     * @param value 字典值
     * @return 字典标签，不存在时返回 {@link Optional#empty()}
     */
    Optional<String> getLabel(String type, Object value);
}

