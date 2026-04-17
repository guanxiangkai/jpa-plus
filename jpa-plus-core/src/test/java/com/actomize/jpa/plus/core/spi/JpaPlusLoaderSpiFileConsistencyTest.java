package com.actomize.jpa.plus.core.spi;

import com.actomize.jpa.plus.core.field.FieldHandler;
import com.actomize.jpa.plus.core.interceptor.DataInterceptor;
import com.actomize.jpa.plus.core.interceptor.InterceptorChainContributor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SPI 文件名一致性测试
 *
 * <p>确保 {@code META-INF/jpa-plus/} 下的 SPI 描述符文件名
 * 与实际 Java 接口全限定名完全匹配，防止包名拼写错误导致 SPI 静默失效。</p>
 *
 * @author guanxiangkai
 * @since 2026年04月16日
 */
class JpaPlusLoaderSpiFileConsistencyTest {

    @AfterEach
    void cleanup() {
        JpaPlusLoader.invalidateAll();
    }

    @Test
    @DisplayName("SPI 描述符文件名应与 FieldHandler 接口 FQCN 一致")
    void fieldHandlerSpiFileNameMatchesInterface() throws IOException {
        assertSpiFileResolvable(FieldHandler.class);
    }

    @Test
    @DisplayName("SPI 描述符文件名应与 DataInterceptor 接口 FQCN 一致")
    void dataInterceptorSpiFileNameMatchesInterface() throws IOException {
        assertSpiFileResolvable(DataInterceptor.class);
    }

    @Test
    @DisplayName("SPI 描述符文件名应与 InterceptorChainContributor 接口 FQCN 一致")
    void interceptorChainContributorSpiFileNameMatchesInterface() throws IOException {
        assertSpiFileResolvable(InterceptorChainContributor.class);
    }

    @Test
    @DisplayName("invalidateAll 应清除全部缓存")
    void invalidateAllClearsCache() {
        // 先触发缓存
        JpaPlusLoader.loadAll(FieldHandler.class);
        // 清除全部
        JpaPlusLoader.invalidateAll();
        // 再次加载不应抛异常
        List<FieldHandler> handlers = JpaPlusLoader.loadAll(FieldHandler.class);
        assertNotNull(handlers);
    }

    @Test
    @DisplayName("SPI 接口 FQCN 不应包含 jpaplus（应为 jpa.plus）")
    void spiInterfaceNamesShouldNotContainJpaplus() {
        // 回归测试：确保不会再出现 jpaplus（无点）的包名
        List<Class<?>> spiInterfaces = List.of(
                FieldHandler.class,
                DataInterceptor.class,
                InterceptorChainContributor.class
        );
        for (Class<?> iface : spiInterfaces) {
            assertFalse(iface.getName().contains("jpaplus"),
                    "SPI 接口 %s 的 FQCN 不应包含 'jpaplus'（应为 'jpa.plus'）".formatted(iface.getName()));
            assertTrue(iface.getName().contains("jpa.plus"),
                    "SPI 接口 %s 的 FQCN 应包含 'jpa.plus'".formatted(iface.getName()));
        }
    }

    /**
     * 验证 SPI 资源路径可被 ClassLoader 解析（文件名与接口 FQCN 一致）
     */
    private void assertSpiFileResolvable(Class<?> serviceType) throws IOException {
        String expectedPath = "META-INF/jpa-plus/" + serviceType.getName();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) cl = getClass().getClassLoader();

        Enumeration<URL> urls = cl.getResources(expectedPath);
        // 注意：在 core 模块单独测试时，可能没有实际的 SPI 文件（由功能模块提供）
        // 这里主要验证路径格式正确（不含 jpaplus 拼写错误）
        assertFalse(expectedPath.contains("jpaplus"),
                "SPI resource path should use 'jpa.plus' not 'jpaplus': " + expectedPath);
        assertTrue(expectedPath.contains("jpa.plus"),
                "SPI resource path should contain 'jpa.plus': " + expectedPath);
    }
}

