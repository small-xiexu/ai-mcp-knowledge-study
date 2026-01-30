package com.xbk.knowledge.application.service.impl;

import com.xbk.knowledge.domain.model.vo.audit.AuditQuery;
import com.xbk.knowledge.domain.service.IAuditService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;

/**
 * 验证审计应用服务的委托行为，确保查询请求透传。
 *
 * @author xiexu
 */
public class AuditAppServiceImplTest {

    /**
     * 对外暴露 shouldDelegateQuery 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldDelegateQuery() {
        IAuditService auditService = Mockito.mock(IAuditService.class);
        AuditAppServiceImpl appService = new AuditAppServiceImpl(auditService);

        AuditQuery query = new AuditQuery(null, null, null, 0, 10, null, null);
        appService.queryAuditPage(query);

        verify(auditService).queryAuditPage(query);
    }
}
