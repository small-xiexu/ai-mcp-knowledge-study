package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.aggregate.audit.ConfigAuditAggregate;
import com.xbk.knowledge.domain.model.entity.ConfigAudit;
import com.xbk.knowledge.domain.model.vo.audit.AuditQuery;
import com.xbk.knowledge.infrastructure.mapper.ConfigAuditMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证配置审计仓储的时间戳补齐与空参处理。
 *
 * @author xiexu
 */
public class ConfigAuditRepositoryImplTest {

    /**
     * 对外暴露 shouldSetCreatedAtOnSave 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldSetCreatedAtOnSave() {
        ConfigAuditMapper mapper = Mockito.mock(ConfigAuditMapper.class);
        ConfigAuditRepositoryImpl repository = new ConfigAuditRepositoryImpl(mapper);

        ConfigAudit audit = ConfigAudit.builder().tableName("t").build();
        ConfigAuditAggregate aggregate = ConfigAuditAggregate.builder().configAudit(audit).build();

        ConfigAuditAggregate saved = repository.save(aggregate);

        assertNotNull(saved.getConfigAudit().getCreatedAt());
        Mockito.verify(mapper).insertConfigAudit(Mockito.any(ConfigAudit.class));
    }

    /**
     * 对外暴露 shouldReturnEmptyWhenQueryNull 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldReturnEmptyWhenQueryNull() {
        ConfigAuditMapper mapper = Mockito.mock(ConfigAuditMapper.class);
        ConfigAuditRepositoryImpl repository = new ConfigAuditRepositoryImpl(mapper);

        assertTrue(repository.findByConditions((AuditQuery) null).isEmpty());
    }
}
