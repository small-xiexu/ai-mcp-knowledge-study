package com.xbk.knowledge.domain.service.impl;

import com.xbk.knowledge.domain.model.entity.ConfigAudit;
import com.xbk.knowledge.domain.model.vo.audit.AuditQuery;
import com.xbk.knowledge.domain.model.adapter.repository.audit.ConfigAuditRepository;
import com.xbk.knowledge.types.common.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 验证审计查询的字段归一化与排序映射，防止非法排序注入。
 *
 * @author xiexu
 */
public class AuditServiceImplTest {

    private ConfigAuditRepository configAuditRepository;
    private AuditServiceImpl service;

    /**
     * 对外暴露 setUp 作为调用入口，便于上层复用。
     */
    @BeforeEach
    public void setUp() {
        configAuditRepository = Mockito.mock(ConfigAuditRepository.class);
        service = new AuditServiceImpl(configAuditRepository);
    }

    /**
     * 对外暴露 shouldRejectNullQuery 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldRejectNullQuery() {
        assertThrows(IllegalArgumentException.class, () -> service.queryAuditPage(null));
    }

    /**
     * 对外暴露 shouldNormalizeSortAndTrimFields 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldNormalizeSortAndTrimFields() {
        AuditQuery request = new AuditQuery(
                "  audit_table ",
                0,
                10,
                "tableName",
                "asc"
        );
        when(configAuditRepository.findByConditions(any(AuditQuery.class))).thenReturn(Collections.<ConfigAudit>emptyList());
        when(configAuditRepository.countByConditions(any(AuditQuery.class))).thenReturn(0L);

        PageResult<ConfigAudit> result = service.queryAuditPage(request);

        ArgumentCaptor<AuditQuery> captor = ArgumentCaptor.forClass(AuditQuery.class);
        Mockito.verify(configAuditRepository).findByConditions(captor.capture());

        AuditQuery normalized = captor.getValue();
        assertEquals("audit_table", normalized.getTableName());
        assertEquals("table_name", normalized.getSortField());
        assertEquals("ASC", normalized.getSortOrder());
        assertEquals(1, result.getPageNum());
    }
}
