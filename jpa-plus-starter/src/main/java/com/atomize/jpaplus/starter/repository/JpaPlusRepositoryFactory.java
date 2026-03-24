package com.atomize.jpaplus.starter.repository;

import com.atomize.jpaplus.core.executor.JpaPlusExecutor;
import com.atomize.jpaplus.query.executor.QueryExecutor;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;

/**
 * JPA-Plus Repository 工厂
 * <p>
 * 创建 SimpleJpaPlusRepository 实例替代默认的 SimpleJpaRepository。
 */
public class JpaPlusRepositoryFactory extends JpaRepositoryFactory {

    private final EntityManager entityManager;
    private final JpaPlusExecutor executor;
    private final QueryExecutor queryExecutor;

    public JpaPlusRepositoryFactory(EntityManager entityManager,
                                    JpaPlusExecutor executor,
                                    QueryExecutor queryExecutor) {
        super(entityManager);
        this.entityManager = entityManager;
        this.executor = executor;
        this.queryExecutor = queryExecutor;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected SimpleJpaPlusRepository<?, ?> getTargetRepository(RepositoryInformation information, EntityManager em) {
        JpaEntityInformation<?, ?> entityInformation = getEntityInformation(information.getDomainType());
        return new SimpleJpaPlusRepository(entityInformation, em, executor, queryExecutor);
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return SimpleJpaPlusRepository.class;
    }
}

