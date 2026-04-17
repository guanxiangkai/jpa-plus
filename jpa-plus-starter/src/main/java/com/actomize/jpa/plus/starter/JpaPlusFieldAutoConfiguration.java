package com.actomize.jpa.plus.starter;

import com.actomize.jpa.plus.core.field.FieldEngine;
import com.actomize.jpa.plus.core.field.FieldHandler;
import com.actomize.jpa.plus.field.autofill.handler.AutoFillFieldHandler;
import com.actomize.jpa.plus.field.autofill.spi.CurrentUserProvider;
import com.actomize.jpa.plus.field.desensitize.handler.DesensitizeFieldHandler;
import com.actomize.jpa.plus.field.dict.handler.DictFieldHandler;
import com.actomize.jpa.plus.field.dict.provider.JdbcDictProvider;
import com.actomize.jpa.plus.field.dict.spi.DictProvider;
import com.actomize.jpa.plus.field.encrypt.handler.EncryptFieldHandler;
import com.actomize.jpa.plus.field.encrypt.spi.EncryptKeyProvider;
import com.actomize.jpa.plus.field.id.enums.IdType;
import com.actomize.jpa.plus.field.id.generator.SnowflakeIdGenerator;
import com.actomize.jpa.plus.field.id.handler.IdFieldHandler;
import com.actomize.jpa.plus.field.id.spi.IdGenerator;
import com.actomize.jpa.plus.field.sensitive.dfa.DfaEngine;
import com.actomize.jpa.plus.field.sensitive.dfa.HoubbSensitiveWordEngine;
import com.actomize.jpa.plus.field.sensitive.handler.SensitiveWordHandler;
import com.actomize.jpa.plus.field.sensitive.spi.SensitiveWordProvider;
import com.actomize.jpa.plus.field.sensitive.spi.SensitiveWordWhitelistProvider;
import com.actomize.jpa.plus.field.version.handler.VersionFieldHandler;
import com.actomize.jpa.plus.interceptor.logicdelete.handler.LogicDeleteFieldHandler;
import com.actomize.jpa.plus.starter.dict.CachedDictProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.util.List;

/**
 * JPA Plus 字段处理器自动装配
 *
 * <p>负责注册所有字段级处理器（ID、自动填充、加密、脱敏、字典、敏感词、乐观锁、逻辑删除）
 * 以及 {@link FieldEngine}。</p>
 *
 * @author guanxiangkai
 * @since 2026年04月12日
 */
@AutoConfiguration
@EnableConfigurationProperties(EncryptKeyProperties.class)
public class JpaPlusFieldAutoConfiguration {

    // ── ID 自动生成 ──

    @Bean
    @ConditionalOnMissingBean
    SnowflakeIdGenerator snowflakeIdGenerator(
            @Value("${jpa-plus.id-generator.snowflake.worker-id:1}") long workerId,
            @Value("${jpa-plus.id-generator.snowflake.datacenter-id:1}") long datacenterId,
            @Value("${jpa-plus.id-generator.snowflake.epoch:1700000000000}") long epoch) {
        return new SnowflakeIdGenerator(workerId, datacenterId, epoch);
    }

    @Bean
    @ConditionalOnMissingBean
    IdFieldHandler idFieldHandler(
            @Value("${jpa-plus.id-generator.type:AUTO}") IdType defaultType,
            SnowflakeIdGenerator snowflakeIdGenerator,
            ObjectProvider<IdGenerator> customGenerator) {
        return new IdFieldHandler(defaultType, snowflakeIdGenerator, customGenerator.getIfAvailable());
    }

    // ── 自动填充 ──

    @Bean
    @ConditionalOnMissingBean
    AutoFillFieldHandler autoFillFieldHandler(ObjectProvider<CurrentUserProvider> userProvider) {
        return new AutoFillFieldHandler(userProvider.getIfAvailable());
    }

    // ── 加密 ──

    @Bean
    @ConditionalOnMissingBean
    EncryptKeyProvider encryptKeyProvider(EncryptKeyProperties properties) {
        return new PropertiesEncryptKeyProvider(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    EncryptFieldHandler encryptFieldHandler(EncryptKeyProvider keyProvider) {
        return new EncryptFieldHandler(keyProvider);
    }

    // ── 脱敏 ──

    @Bean
    @ConditionalOnMissingBean
    DesensitizeFieldHandler desensitizeFieldHandler() {
        return new DesensitizeFieldHandler();
    }

    // ── 乐观锁 ──

    @Bean
    @ConditionalOnMissingBean
    VersionFieldHandler versionFieldHandler() {
        return new VersionFieldHandler();
    }

    // ── 逻辑删除 ──

    @Bean
    @ConditionalOnMissingBean
    LogicDeleteFieldHandler logicDeleteFieldHandler() {
        return new LogicDeleteFieldHandler();
    }

    // ── 字典数据提供者（内置 JDBC 实现） ──

    @Bean
    @ConditionalOnMissingBean(DictProvider.class)
    @ConditionalOnProperty(prefix = "jpa-plus.dict.jdbc", name = "enabled", havingValue = "true")
    JdbcDictProvider jdbcDictProvider(
            DataSource dataSource,
            @Value("${jpa-plus.dict.jdbc.table-name:jpa_plus_dict}") String tableName,
            @Value("${jpa-plus.dict.jdbc.auto-init-schema:true}") boolean autoInitSchema) {
        return new JdbcDictProvider(dataSource, tableName, autoInitSchema);
    }

    /**
     * 字典缓存装饰器：自动包装用户提供的 DictProvider，避免每次查询都访问数据库
     *
     * <p>TTL 通过 {@code jpa-plus.dict.cache.ttl-seconds} 配置（默认 300 秒）。
     * 若用户已自行实现缓存逻辑，可通过注册 {@code CachedDictProvider} Bean 跳过此装饰。</p>
     */
    @Bean
    @ConditionalOnBean(DictProvider.class)
    @ConditionalOnMissingBean(CachedDictProvider.class)
    @ConditionalOnProperty(prefix = "jpa-plus.dict.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    CachedDictProvider cachedDictProvider(
            DictProvider dictProvider,
            @Value("${jpa-plus.dict.cache.ttl-seconds:300}") long ttlSeconds) {
        return new CachedDictProvider(dictProvider, ttlSeconds);
    }

    @Bean
    @ConditionalOnBean(DictProvider.class)
    @ConditionalOnMissingBean
    DictFieldHandler dictFieldHandler(ObjectProvider<CachedDictProvider> cachedProvider,
                                      DictProvider rawProvider) {
        // 优先使用带缓存的实现
        DictProvider effective = cachedProvider.getIfAvailable();
        return new DictFieldHandler(effective != null ? effective : rawProvider);
    }

    // ── 敏感词（houbb 引擎优先，含白名单） ──

    @Bean
    @ConditionalOnBean(SensitiveWordProvider.class)
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "com.github.houbb.sensitive.word.bs.SensitiveWordBs")
    SensitiveWordHandler sensitiveWordHandlerHoubb(
            SensitiveWordProvider provider,
            ObjectProvider<SensitiveWordWhitelistProvider> whitelistProvider) {
        return new SensitiveWordHandler(provider, whitelistProvider.getIfAvailable(), HoubbSensitiveWordEngine::new);
    }

    @Bean
    @ConditionalOnBean(SensitiveWordProvider.class)
    @ConditionalOnMissingBean
    SensitiveWordHandler sensitiveWordHandler(
            SensitiveWordProvider provider,
            ObjectProvider<SensitiveWordWhitelistProvider> whitelistProvider) {
        return new SensitiveWordHandler(provider, whitelistProvider.getIfAvailable(), DfaEngine::new);
    }

    // ── 字段引擎 ──

    @Bean
    @ConditionalOnMissingBean
    FieldEngine fieldEngine(List<FieldHandler> handlers) {
        return new FieldEngine(handlers);
    }
}
