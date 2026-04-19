package com.actomize.jpa.plus.core.spi;

import com.actomize.jpa.plus.core.exception.JpaPlusException;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * JPA-Plus SPI 加载器
 *
 * <p>从 {@code META-INF/jpa-plus/} 目录加载 SPI 实现类（类似 {@link java.util.ServiceLoader}，
 * 但使用自定义路径）。文件名为接口全限定名，内容为实现类全限定名（每行一个，支持 # 注释）。</p>
 *
 * <p>特性：
 * <ul>
 *   <li>线程安全：使用同步包装的 {@link WeakHashMap} 缓存已加载的实现列表</li>
 *   <li>支持排序：实现 {@link Ordered} 接口的 SPI 实例按 {@link Ordered#order()} 升序排列</li>
 *   <li>生命周期管理：{@link #invalidateAll()} 可在应用关闭时清理全部缓存，防止 ClassLoader 泄漏</li>
 * </ul>
 * </p>
 *
 * <p><b>设计模式：</b>SPI 服务发现模式 + 缓存模式</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Slf4j
public final class JpaPlusLoader {

    private static final String SPI_DIR = "META-INF/jpa-plus/";
    /**
     * P1-12: Use WeakHashMap so that Class<?> keys (and transitively their ClassLoaders) can be
     * garbage-collected after a hot-redeploy replaces the application ClassLoader.
     * A ConcurrentHashMap would hold strong references, pinning the old ClassLoader indefinitely.
     * The synchronized wrapper provides the same thread-safety guarantee as ConcurrentHashMap
     * for the computeIfAbsent / remove / clear operations used here.
     */
    private static final Map<Class<?>, List<?>> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    static {
        // 注册 JVM 关闭钩子，在应用关闭时自动清除 SPI 缓存，防止 ClassLoader 泄漏
        Runtime.getRuntime().addShutdownHook(
                new Thread(JpaPlusLoader::invalidateAll, "jpa-plus-spi-cleanup"));
    }

    private JpaPlusLoader() {
    }

    /**
     * 加载第一个 SPI 实现
     */
    public static <T> T load(Class<T> serviceType) {
        return loadAll(serviceType).stream().findFirst()
                .orElseThrow(() -> new JpaPlusException("No SPI implementation for: " + serviceType.getName()));
    }

    /**
     * 加载指定接口的所有 SPI 实现
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> loadAll(Class<T> serviceType) {
        return (List<T>) CACHE.computeIfAbsent(serviceType, JpaPlusLoader::doLoad);
    }

    /**
     * 预热 SPI 缓存
     */
    public static void warmUp(Class<?>... serviceTypes) {
        for (Class<?> type : serviceTypes) {
            loadAll(type);
        }
    }

    /**
     * 使指定接口的 SPI 缓存失效
     */
    public static void invalidate(Class<?> serviceType) {
        CACHE.remove(serviceType);
    }

    /**
     * 清除全部 SPI 缓存（应用关闭 / 热重载时调用，防止 ClassLoader 泄漏）
     */
    public static void invalidateAll() {
        CACHE.clear();
        log.debug("[jpa-plus] JpaPlusLoader: all SPI caches invalidated");
    }

    private static <T> List<T> doLoad(Class<T> serviceType) {
        String resourceName = SPI_DIR + serviceType.getName();
        List<T> instances = new ArrayList<>();

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = JpaPlusLoader.class.getClassLoader();
            }

            Enumeration<URL> urls = classLoader.getResources(resourceName);
            Set<String> classNames = new LinkedHashSet<>();

            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        // 跳过空行和注释
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            classNames.add(line);
                        }
                    }
                }
            }

            for (String className : classNames) {
                try {
                    @SuppressWarnings("unchecked") // serviceType is Class<T>, so the cast is safe after isAssignableFrom check
                    Class<T> implClass = (Class<T>) Class.forName(className, true, classLoader);
                    if (!serviceType.isAssignableFrom(implClass)) {
                        log.warn("[jpa-plus] SPI 类型不匹配，已跳过: {} 未实现 {}",
                                className, serviceType.getName());
                        continue;
                    }
                    T instance = implClass.getDeclaredConstructor().newInstance();
                    instances.add(instance);
                } catch (ClassNotFoundException e) {
                    log.warn("[jpa-plus] SPI 类未找到（检查 classpath 或拼写）: {}", className);
                } catch (NoSuchMethodException e) {
                    log.warn("[jpa-plus] SPI 类缺少无参构造器: {}", className);
                } catch (InstantiationException e) {
                    log.warn("[jpa-plus] SPI 类无法实例化（可能是抽象类或接口）: {}", className);
                } catch (IllegalAccessException e) {
                    log.warn("[jpa-plus] SPI 类构造器不可访问（检查修饰符）: {}", className);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    throw new JpaPlusException(
                            "[jpa-plus] SPI 类构造器抛出异常，框架无法安全启动: " + className, cause);
                }
            }

            // 统一通过 Ordered 接口排序（所有 SPI 契约均继承 Ordered）
            instances.sort(Comparator.comparingInt(JpaPlusLoader::getOrder));

        } catch (java.io.IOException e) {
            log.error("[jpa-plus] 读取 SPI 资源文件失败: {}", resourceName, e);
        }

        return List.copyOf(instances);
    }

    private static int getOrder(Object instance) {
        return instance instanceof Ordered ordered ? ordered.order() : Integer.MAX_VALUE;
    }
}
