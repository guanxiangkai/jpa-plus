package com.atomize.jpa.plus.core.spi;

import com.atomize.jpa.plus.core.exception.JpaPlusException;
import com.atomize.jpa.plus.core.field.FieldHandler;
import com.atomize.jpa.plus.core.interceptor.DataInterceptor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JPA-Plus SPI 加载器
 *
 * <p>从 {@code META-INF/jpa-plus/} 目录加载 SPI 实现类（类似 {@link java.util.ServiceLoader}，
 * 但使用自定义路径）。文件名为接口全限定名，内容为实现类全限定名（每行一个，支持 # 注释）。</p>
 *
 * <p>特性：
 * <ul>
 *   <li>线程安全：使用 {@link ConcurrentHashMap} 缓存已加载的实现列表</li>
 *   <li>支持排序：实现类可通过 {@link FieldHandler#order()} 或
 *       {@link DataInterceptor#order()} 控制优先级</li>
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
    private static final ConcurrentHashMap<Class<?>, List<?>> CACHE = new ConcurrentHashMap<>();

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
                    @SuppressWarnings("unchecked")
                    Class<T> implClass = (Class<T>) Class.forName(className, true, classLoader);
                    T instance = implClass.getDeclaredConstructor().newInstance();
                    instances.add(instance);
                } catch (Exception e) {
                    log.warn("Failed to load SPI implementation: {}", className, e);
                }
            }

            // 按 @Order 或接口中的 order() 方法排序
            instances.sort(Comparator.comparingInt(JpaPlusLoader::getOrder));

        } catch (Exception e) {
            log.error("Failed to load SPI for: {}", serviceType.getName(), e);
        }

        return List.copyOf(instances);
    }

    private static int getOrder(Object instance) {
        return switch (instance) {
            case FieldHandler fh -> fh.order();
            case DataInterceptor di -> di.order();
            default -> Integer.MAX_VALUE;
        };
    }
}
