package com.atomize.jpa.plus.starter.repository;

import com.atomize.jpa.plus.core.executor.JpaPlusExecutor;
import com.atomize.jpa.plus.query.executor.QueryExecutor;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * JPA-Plus Repository 工厂 Bean
 * <p>
 * 替换 Spring Data JPA 默认的 RepositoryFactoryBean，
 * 使所有 Repository 自动获得 JPA-Plus 增强功能。
 */
public class JpaPlusRepositoryFactoryBean<R extends JpaRepository<T, ID>, T, ID>
        extends JpaRepositoryFactoryBean<R, T, ID> {

    @Autowired
    private JpaPlusExecutor executor;

    @Autowired
    private QueryExecutor queryExecutor;

    public JpaPlusRepositoryFactoryBean(Class<? extends R> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
        return new JpaPlusRepositoryFactory(entityManager, executor, queryExecutor);
    }
}

