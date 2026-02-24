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
     * 流程：
     * 1. 进入接口后执行 `@SaCheckLogin` 登录态校验。
     * 2. Controller 调用 `workbenchAppService.summary` 拉取工作台聚合数据。
     * 3. 在 Trigger 层将应用层模型转换为 `WorkbenchSummaryResponse`。
     * 4. 对空值场景做兜底（如 guideSteps 返回空列表）。
     * 5. 统一通过 `Result.success` 返回给前端工作台页面。
     *
     * @param ignored 请求体占位参数（可为空）
     * @return 工作台汇总响应
     */
    @PostMapping("/summary")
    @SaCheckLogin
    @Override
    public Result<WorkbenchSummaryResponse> summary(@RequestBody(required = false) Object ignored) {
        WorkbenchSummary summary = workbenchAppService.summary();
        return Result.success(toResponse(summary));
    }

    /**
     * 将输入数据转换为响应。
     *
     * @param s 源对象。
     * @return 返回WorkbenchSummaryResponse对象。
     */
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

    /**
     * 将输入数据转换为模型。
     *
     * @param m 模型对象。
     * @return 返回ModelInfo对象。
     */
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

    /**
     * 将输入数据转换为智能体信息。
     *
     * @param a 智能体对象。
     * @return 返回AgentInfo对象。
     */
    private WorkbenchSummaryResponse.AgentInfo toAgent(WorkbenchSummary.AgentInfo a) {
        if (a == null) {
            return null;
        }
        return WorkbenchSummaryResponse.AgentInfo.builder()
                .total(a.getTotal())
                .published(a.getPublished())
                .build();
    }

    /**
     * 将输入数据转换为提示词。
     *
     * @param p 提示词对象。
     * @return 返回PromptInfo对象。
     */
    private WorkbenchSummaryResponse.PromptInfo toPrompt(WorkbenchSummary.PromptInfo p) {
        if (p == null) {
            return null;
        }
        return WorkbenchSummaryResponse.PromptInfo.builder()
                .draft(p.getDraft())
                .published(p.getPublished())
                .build();
    }

    /**
     * 将输入数据转换为工具。
     *
     * @param t 工具对象。
     * @return 返回ToolInfo对象。
     */
    private WorkbenchSummaryResponse.ToolInfo toTool(WorkbenchSummary.ToolInfo t) {
        if (t == null) {
            return null;
        }
        return WorkbenchSummaryResponse.ToolInfo.builder()
                .approvalsPending(t.getApprovalsPending())
                .build();
    }

    /**
     * 将输入数据转换为调度。
     *
     * @param s 源对象。
     * @return 返回ScheduleInfo对象。
     */
    private WorkbenchSummaryResponse.ScheduleInfo toSchedule(WorkbenchSummary.ScheduleInfo s) {
        if (s == null) {
            return null;
        }
        return WorkbenchSummaryResponse.ScheduleInfo.builder()
                .total(s.getTotal())
                .enabled(s.getEnabled())
                .build();
    }

    /**
     * 将输入数据转换为知识信息。
     *
     * @param k 知识对象。
     * @return 返回KnowledgeInfo对象。
     */
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

    /**
     * 将输入数据转换为待办信息。
     *
     * @param t 工具对象。
     * @return 返回TodoInfo对象。
     */
    private WorkbenchSummaryResponse.TodoInfo toTodo(WorkbenchSummary.TodoInfo t) {
        if (t == null) {
            return null;
        }
        return WorkbenchSummaryResponse.TodoInfo.builder()
                .writeBlockedForSuperAdmin(t.isWriteBlockedForSuperAdmin())
                .build();
    }

    /**
     * 将输入数据转换为步骤。
     *
     * @param s 源对象。
     * @return 返回GuideStep对象。
     */
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
