package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.dto.audit.AuditQueryRequest;
import com.xbk.knowledge.api.dto.audit.AuditResponse;
import com.xbk.knowledge.application.service.app.AuditAppService;
import com.xbk.knowledge.domain.audit.model.entity.ConfigAudit;
import com.xbk.knowledge.domain.audit.model.valobj.AuditQuery;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证审计 Controller 的分页转换与字段映射。
 *
 * @author xiexu
 */
public class AuditControllerTest {

    /**
     * 对外暴露 shouldListAudits 作为调用入口，便于上层复用。
     */
    @Test
    public void shouldListAudits() {
        AuditAppService appService = Mockito.mock(AuditAppService.class);
        AuditController controller = new AuditController(appService);

        ConfigAudit audit = ConfigAudit.builder()
                .id(1L)
                .tableName("t")
                .recordId(2L)
                .operation("UPDATE")
                .operator("u")
                .createdAt(LocalDateTime.now())
                .build();
        PageResult<ConfigAudit> pageResult = PageResult.of(Collections.<ConfigAudit>singletonList(audit), 1L, 1, 10);
        when(appService.queryAuditPage(any(AuditQuery.class))).thenReturn(pageResult);

        AuditQueryRequest request = new AuditQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        request.validate();

        Result<PageResult<AuditResponse>> result = controller.listAudits(request);

        assertEquals("t", result.getData().getRecords().get(0).getTableName());
        assertEquals("UPDATE", result.getData().getRecords().get(0).getOperation());

        ArgumentCaptor<AuditQuery> captor = ArgumentCaptor.forClass(AuditQuery.class);
        verify(appService).queryAuditPage(captor.capture());
        assertEquals(0, captor.getValue().getOffset());
    }
}
