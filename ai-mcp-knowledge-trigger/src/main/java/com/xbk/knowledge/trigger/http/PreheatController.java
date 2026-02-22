package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.IPreheatService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xbk.knowledge.api.dto.preheat.PreheatAgentVersionRequest;
import com.xbk.knowledge.api.dto.preheat.PreheatResponse;
import com.xbk.knowledge.api.dto.preheat.PreheatWorkflowVersionRequest;
import com.xbk.knowledge.application.model.preheat.PreheatResult;
import com.xbk.knowledge.application.service.app.PreheatAppService;
import com.xbk.knowledge.types.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * 显式 Preheat 接口。
 *
 * 说明：用于提前装配工具/Advisor/Workflow 校验等，降低首次调用延迟。
 *
 * @author sxie
 */
@RestController
@RequestMapping("/api/preheat")
@RequiredArgsConstructor
public class PreheatController implements IPreheatService {

    private final PreheatAppService preheatAppService;

    /**
     * preheatAgentVersion。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/agent-version")
    @SaCheckPermission("agent:write")
    @Override
    public Result<PreheatResponse> preheatAgentVersion(@Valid @RequestBody PreheatAgentVersionRequest request) {
        boolean refreshMcp = request.getRefreshMcp() != null && request.getRefreshMcp();
        if (refreshMcp) {
            StpUtil.checkPermission("tool:write");
        }
        PreheatResult result = preheatAppService.preheatAgentVersion(request.getAgentVersionId(), refreshMcp);
        return Result.success(toResponse(result));
    }

    /**
     * preheatWorkflowVersion。
     *
     * @param request 参数
     * @return 返回结果
     */
    @PostMapping("/workflow-version")
    @SaCheckPermission("workflow:write")
    @Override
    public Result<PreheatResponse> preheatWorkflowVersion(@Valid @RequestBody PreheatWorkflowVersionRequest request) {
        boolean refreshMcp = request.getRefreshMcp() != null && request.getRefreshMcp();
        if (refreshMcp) {
            StpUtil.checkPermission("tool:write");
        }
        PreheatResult result = preheatAppService.preheatWorkflowVersion(request.getWorkflowVersionId(), refreshMcp);
        return Result.success(toResponse(result));
    }

    private PreheatResponse toResponse(PreheatResult r) {
        if (r == null) {
            return null;
        }
        PreheatResponse resp = new PreheatResponse();
        resp.setTargetType(r.getTargetType());
        resp.setTargetId(r.getTargetId());
        resp.setMcpRefreshed(r.isMcpRefreshed());
        resp.setToolCallbacksWarmed(r.isToolCallbacksWarmed());
        resp.setAdvisorsWarmed(r.isAdvisorsWarmed());
        resp.setWorkflowValidated(r.isWorkflowValidated());
        resp.setWarnings(r.getWarnings());
        return resp;
    }

}
