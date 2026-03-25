package com.atomize.jpaplus.orderby.annotation;

import java.lang.annotation.*;

/**
 * 自动排序注解
 *
 * <p>标注在实体字段上，声明该字段的默认排序规则。
 * 当查询未显式指定 ORDER BY 时，框架自动按该注解配置的排序规则排列结果集。</p>
 *
 * <p>支持多字段排序，通过 {@link #priority()} 控制排序字段的先后顺序。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @Entity
 * public class Article {
 *
 *     @AutoOrderBy(direction = Direction.DESC, priority = 1)
 *     private LocalDateTime createTime;
 *
 *     @AutoOrderBy(direction = Direction.ASC, priority = 2)
 *     private Integer sortOrder;
 * }
 * }</pre>
 *
 * <p>效果：查询 Article 时若未手动 orderBy，自动追加
 * {@code ORDER BY create_time DESC, sort_order ASC}</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoOrderBy {

    /**
     * 排序方向（默认升序）
     */
    Direction direction() default Direction.ASC;

    /**
     * 排序优先级（值越小越靠前，默认 0）
     *
     * <p>当实体上有多个 {@code @AutoOrderBy} 字段时，
     * 按 priority 从小到大排列，作为 ORDER BY 子句中的先后顺序。</p>
     */
    int priority() default 0;

    /**
     * 自定义列名（默认空，使用驼峰转蛇形命名）
     *
     * <p>若字段对应的数据库列名与驼峰转换结果不一致，可手动指定。</p>
     */
    String column() default "";

    /**
     * 排序方向枚举
     */
    enum Direction {
        ASC, DESC
    }
}

