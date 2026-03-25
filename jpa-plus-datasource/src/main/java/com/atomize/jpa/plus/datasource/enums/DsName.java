package com.atomize.jpa.plus.datasource.enums;

/**
 * 数据源名称接口（可扩展枚举模式）
 *
 * <p>内置实现见 {@link DefaultDsName}。
 * 用户可自定义 {@code enum} 实现此接口，支持任意数据源名称。</p>
 *
 * <h3>在注解中使用（通过字符串常量）</h3>
 * <pre>{@code
 * @DS(DsName.MASTER)
 * @DS(DsName.SLAVE)
 * }</pre>
 *
 * <h3>在代码中使用（通过枚举实例）</h3>
 * <pre>{@code
 * JpaPlusContext.withDS(DefaultDsName.SLAVE.value(), () -> {
 *     return userRepo.findById(1L);
 * });
 * }</pre>
 *
 * <h3>扩展方式</h3>
 * <pre>{@code
 * public enum MyDsName implements DsName {
 *     ANALYTICS("analytics"),
 *     REPORTING("reporting");
 *
 *     // 注解用常量
 *     public static final String ANALYTICS_DS = "analytics";
 *     public static final String REPORTING_DS = "reporting";
 *
 *     private final String value;
 *     MyDsName(String value) { this.value = value; }
 *     @Override public String value() { return value; }
 * }
 *
 * // 使用
 * @DS(MyDsName.ANALYTICS_DS)
 * }</pre>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期二
 */
public interface DsName {

    // ─── 字符串常量（供 @DS 注解使用，注解要求编译时常量） ───

    String MASTER = "master";
    String SLAVE = "slave";

    /**
     * 数据源名称
     */
    String value();
}

