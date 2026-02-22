package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.IWorkbenchService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.xbk.knowledge.api.dto.workbench.WorkbenchSummaryResponse;
import com.xbk.knowledge.application.model.workbench.WorkbenchSummary;
import com.xbk.knowledge.application.service.app.WorkbenchAppService;
import com.xbk.knowledge.types.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * 工作台聚合接口（方案B）。
 *
 * 职责：一次请求返回工作台所需的关键指标与治理引导步骤。
 *
 * @author sxie
 */
@RestController
@RequestMapping("/api/workbench")
@RequiredArgsConstructor
public class WorkbenchController implements IWorkbenchService {

    private final WorkbenchAppService workbenchAppService;

    /**
     * 获取工作台汇总信息。
     *
     * 说明：工作台本身为“入口页”，只要求登录即可。
     */
    @PostMapping("/summary")
    @SaCheckLogin
    @Override
    public Result<WorkbenchSummaryResponse> summary(@RequestBody(required = false) Object ignored) {
        WorkbenchSummary summary = workbenchAppService.summary();
        return Result.success(toResponse(summary));
    }

    private WorkbenchSummaryResponse toResponse(WorkbenchSummary s) {
        if (s == null) {
            return WorkbenchSummaryResponse.builder()
                    .guideSteps(Collections.emptyList())
                    .build();
        }
        List<WorkbenchSummaryResponse.GuideStep> steps = (s.getGuideSteps() == null ? Collections.<WorkbenchSummaryResponse.GuideStep>emptyList() :
                s.getGuideSteps().stream().map(this::toStep).toList());
        return WorkbenchSummaryResponse.builder()
                .model(toModel(s.getModel()))
                .agent(toAgent(s.getAgent()))
                .prompt(toPrompt(s.getPrompt()))
                .tool(toTool(s.getTool()))
                .schedule(toSchedule(s.getSchedule()))
                .knowledge(toKnowledge(s.getKnowledge()))
                .todo(toTodo(s.getTodo()))
                .guideSteps(steps)
                .build();
    }

    private WorkbenchSummaryResponse.ModelInfo toModel(WorkbenchSummary.ModelInfo m) {
        if (m == null) {
            return null;
        }
        return WorkbenchSummaryResponse.ModelInfo.builder()
                .total(m.getTotal())
                .enabled(m.getEnabled())
                .activeChatModelId(m.getActiveChatModelId())
                .activeEmbeddingModelId(m.getActiveEmbeddingModelId())
                .build();
    }

    private WorkbenchSummaryResponse.AgentInfo toAgent(WorkbenchSummary.AgentInfo a) {
        if (a == null) {
            return null;
        }
        return WorkbenchSummaryResponse.AgentInfo.builder()
                .total(a.getTotal())
                .published(a.getPublished())
                .build();
    }

    private WorkbenchSummaryResponse.PromptInfo toPrompt(WorkbenchSummary.PromptInfo p) {
        if (p == null) {
            return null;
        }
        return WorkbenchSummaryResponse.PromptInfo.builder()
                .draft(p.getDraft())
                .published(p.getPublished())
                .build();
    }

    private WorkbenchSummaryResponse.ToolInfo toTool(WorkbenchSummary.ToolInfo t) {
        if (t == null) {
            return null;
        }
        return WorkbenchSummaryResponse.ToolInfo.builder()
                .approvalsPending(t.getApprovalsPending())
                .build();
    }

    private WorkbenchSummaryResponse.ScheduleInfo toSchedule(WorkbenchSummary.ScheduleInfo s) {
        if (s == null) {
            return null;
        }
        return WorkbenchSummaryResponse.ScheduleInfo.builder()
                .total(s.getTotal())
                .enabled(s.getEnabled())
                .build();
    }

    private WorkbenchSummaryResponse.KnowledgeInfo toKnowledge(WorkbenchSummary.KnowledgeInfo k) {
        if (k == null) {
            return null;
        }
        return WorkbenchSummaryResponse.KnowledgeInfo.builder()
                .ragTagCount(k.getRagTagCount())
                .ragTaskTotal(k.getRagTaskTotal())
                .ragTaskProcessing(k.getRagTaskProcessing())
                .ragTaskFailedRecent(k.getRagTaskFailedRecent())
                .build();
    }

    private WorkbenchSummaryResponse.TodoInfo toTodo(WorkbenchSummary.TodoInfo t) {
        if (t == null) {
            return null;
        }
        return WorkbenchSummaryResponse.TodoInfo.builder()
                .writeBlockedForSuperAdmin(t.isWriteBlockedForSuperAdmin())
                .build();
    }

    private WorkbenchSummaryResponse.GuideStep toStep(WorkbenchSummary.GuideStep s) {
        if (s == null) {
            return null;
        }
        return WorkbenchSummaryResponse.GuideStep.builder()
                .key(s.getKey())
                .title(s.getTitle())
                .status(s.getStatus())
                .message(s.getMessage())
                .actionPath(s.getActionPath())
                .actionLabel(s.getActionLabel())
                .writeAction(s.isWriteAction())
                .build();
    }
}
