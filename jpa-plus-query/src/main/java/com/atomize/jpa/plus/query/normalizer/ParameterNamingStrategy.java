package com.atomize.jpa.plus.query.normalizer;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 参数命名策略
 *
 * <p>每次编译使用独立的计数器实例，避免全局计数器在并发编译时产生不确定的参数名。
 * 通过 {@link #begin()} 开启编译上下文，{@link #end()} 关闭，保证线程安全。</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
public class ParameterNamingStrategy {

    private static final ThreadLocal<AtomicInteger> COUNTER = ThreadLocal.withInitial(() -> new AtomicInteger(0));

    /**
     * 开启新的编译上下文（重置计数器）
     */
    public static void begin() {
        COUNTER.get().set(0);
    }

    /**
     * 结束编译上下文（清理 ThreadLocal，防止内存泄漏）
     */
    public static void end() {
        COUNTER.remove();
    }

    /**
     * 生成带前缀的唯一参数名
     */
    public static String next(String prefix) {
        return prefix + "_" + COUNTER.get().getAndIncrement();
    }

    /**
     * 生成带上下文的唯一参数名
     */
    public static String nextWithContext(String context) {
        return context + "_" + COUNTER.get().getAndIncrement();
    }

    /**
     * 重置计数器（仅用于测试）
     */
    public static void reset() {
        COUNTER.get().set(0);
    }
}

