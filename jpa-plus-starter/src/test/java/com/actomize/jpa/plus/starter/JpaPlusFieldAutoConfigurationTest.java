package com.actomize.jpa.plus.starter;

import com.actomize.jpa.plus.field.dict.handler.DictFieldHandler;
import com.actomize.jpa.plus.field.dict.provider.JdbcDictProvider;
import com.actomize.jpa.plus.field.dict.spi.DictProvider;
import com.actomize.jpa.plus.starter.dict.CachedDictProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JpaPlusFieldAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JpaPlusFieldAutoConfiguration.class))
            .withBean(DataSource.class, () -> mock(DataSource.class));

    @Test
    void dictFieldHandlerUsesCachedProviderWhenJdbcAndCacheAreEnabled() {
        contextRunner
                .withPropertyValues(
                        "jpa-plus.encrypt.key=1234567890abcdef",
                        "jpa-plus.dict.jdbc.enabled=true",
                        "jpa-plus.dict.jdbc.auto-init-schema=false",
                        "jpa-plus.dict.cache.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DictFieldHandler.class);
                    assertThat(context).hasSingleBean(JdbcDictProvider.class);
                    assertThat(context).hasSingleBean(CachedDictProvider.class);

                    DictFieldHandler handler = context.getBean(DictFieldHandler.class);
                    Object delegate = new DirectFieldAccessor(handler).getPropertyValue("dictProvider");
                    assertThat(delegate).isInstanceOf(CachedDictProvider.class);

                    DictProvider provider = context.getBean(DictProvider.class);
                    assertThat(provider).isInstanceOf(CachedDictProvider.class);
                });
    }
}
