package com.atomize.jpa.plus.datasource.enums;

/**
 * 内置数据源名称枚举
 *
 * <p>提供常见的数据源名称，适用于单主多从读写分离场景。</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期二
 */
public enum DefaultDsName implements DsName {

    MASTER(DsName.MASTER),
    SLAVE(DsName.SLAVE);

    private final String value;

    DefaultDsName(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}

