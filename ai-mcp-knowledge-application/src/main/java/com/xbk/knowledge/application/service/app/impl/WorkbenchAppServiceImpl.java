package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.model.workbench.WorkbenchSummary;
import com.xbk.knowledge.application.service.app.WorkbenchAppService;
import com.xbk.knowledge.domain.model.entity.ModelActivation;
import com.xbk.knowledge.domain.model.vo.agent.AgentPageQuery;
import com.xbk.knowledge.domain.model.vo.agent.AgentSchedulePageQuery;
import com.xbk.knowledge.domain.model.vo.agent.PromptTemplatePageQuery;
import com.xbk.knowledge.domain.model.vo.common.EnabledQuery;
import com.xbk.knowledge.domain.model.vo.tool.ToolPolicyPageQuery;
import com.xbk.knowledge.domain.repository.agent.AgentRepository;
import com.xbk.knowledge.domain.repository.agent.AgentScheduleRepository;
import com.xbk.knowledge.domain.repository.approval.ApprovalRequestRepository;
import com.xbk.knowledge.domain.repository.model.ModelActivationRepository;
import com.xbk.knowledge.domain.repository.model.ModelConfigRepository;
import com.xbk.knowledge.domain.repository.prompt.PromptTemplateRepository;
import com.xbk.knowledge.domain.repository.rag.RagTaskRepository;
import com.xbk.knowledge.domain.repository.tool.ToolPolicyRepository;
import com.xbk.knowledge.types.context.OrgContext;
import com.xbk.knowledge.types.context.OrgContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 工作台聚合应用服务实现（方案B）。
 *
 * 说明：仅做轻量聚合与口径封装，不承载复杂业务流程。
 
  * @author xiexu
  */
@Service
@RequiredArgsConstructor
public class WorkbenchAppServiceImpl implements WorkbenchAppService {

    private static final long DEFAULT_ROOT_ORG_ID = 1L;

    private final AgentRepository agentRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final ToolPolicyRepository toolPolicyRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final AgentScheduleRepository agentScheduleRepository;
    private final RagTaskRepository ragTaskRepository;
    private final ModelConfigRepository modelConfigRepository;
    private final ModelActivationRepository modelActivationRepository;

    /**
     * summary。
     *
     * @param orgId 参数
     * @return 返回结果
     */
    @Override
    public WorkbenchSummary summary(Long orgId) {
        OrgContext ctx = OrgContextHolder.get();
        Long currentOrgId = orgId != null ? orgId : (ctx != null && ctx.currentOrgId() != null ? ctx.currentOrgId() : DEFAULT_ROOT_ORG_ID);

        boolean superAdmin = ctx != null && ctx.superAdmin();
        boolean explicitTargetOrg = true;

        // Models
        long modelTotal = modelConfigRepository.countAll();
        long modelEnabled = modelConfigRepository.findByEnabled(new EnabledQuery(true)).size();
        ModelActivation activation = modelActivationRepository.queryActivation();
        Long activeChatModelId = activation == null ? null : activation.getChatModelId();
        Long activeEmbeddingModelId = activation == null ? null : activation.getEmbeddingModelId();

        // Agent
        long agentTotal = agentRepository.count(new AgentPageQuery(currentOrgId, null, null, 0, 1));
        long agentPublished = agentRepository.countPublishedByOrgId(currentOrgId);

        // Prompt (GLOBAL + ORG)
        long globalPublished = promptTemplateRepository.count(new PromptTemplatePageQuery(currentOrgId, null, "GLOBAL", "PUBLISHED", 0, 1));
        long orgDraft = promptTemplateRepository.count(new PromptTemplatePageQuery(currentOrgId, null, "ORG", "DRAFT", 0, 1));
        long orgPublished = promptTemplateRepository.count(new PromptTemplatePageQuery(currentOrgId, null, "ORG", "PUBLISHED", 0, 1));

        // Tool governance
        long toolPolicyTotal = toolPolicyRepository.count(new ToolPolicyPageQuery(currentOrgId, null, null, 0, 1));
        long toolPolicyEnabled = toolPolicyRepository.count(new ToolPolicyPageQuery(currentOrgId, null, 1, 0, 1));
        long approvalsPending = approvalRequestRepository.count(currentOrgId, "PENDING");

        // Schedule
        long scheduleTotal = agentScheduleRepository.count(new AgentSchedulePageQuery(currentOrgId, null, null, 0, 1));
        long scheduleEnabled = agentScheduleRepository.count(new AgentSchedulePageQuery(currentOrgId, null, true, 0, 1));

        // Knowledge (best effort: derive from tasks)
        long ragTaskTotal = ragTaskRepository.countByOrgId(currentOrgId);
        long ragTaskProcessing = ragTaskRepository.countByOrgIdAndStatus(currentOrgId, "PROCESSING");
        long ragTagCount = ragTaskRepository.countDistinctRagTagByOrgId(currentOrgId);
        long ragFailedRecent = ragTaskRepository.countFailedTasksSince(currentOrgId, LocalDateTime.now().minusDays(7));

        List<WorkbenchSummary.GuideStep> steps = buildGuideSteps(
                modelEnabled, activeChatModelId, ragTagCount,
                agentTotal, agentPublished, toolPolicyTotal, approvalsPending, scheduleEnabled);

        return WorkbenchSummary.builder()
                .org(WorkbenchSummary.OrgInfo.builder()
                        .currentOrgId(currentOrgId)
                        .operatorOrgId(ctx == null ? null : ctx.operatorOrgId())
                        .superAdmin(superAdmin)
                        .explicitTargetOrg(explicitTargetOrg)
                        .build())
                .model(WorkbenchSummary.ModelInfo.builder()
                        .total(modelTotal)
                        .enabled(modelEnabled)
                        .activeChatModelId(activeChatModelId)
                        .activeEmbeddingModelId(activeEmbeddingModelId)
                        .build())
                .agent(WorkbenchSummary.AgentInfo.builder()
                        .total(agentTotal)
                        .published(agentPublished)
                        .build())
                .prompt(WorkbenchSummary.PromptInfo.builder()
                        .globalPublished(globalPublished)
                        .orgDraft(orgDraft)
                        .orgPublished(orgPublished)
                        .build())
                .tool(WorkbenchSummary.ToolInfo.builder()
                        .toolPolicyTotal(toolPolicyTotal)
                        .toolPolicyEnabled(toolPolicyEnabled)
                        .approvalsPending(approvalsPending)
                        .build())
                .schedule(WorkbenchSummary.ScheduleInfo.builder()
                        .total(scheduleTotal)
                        .enabled(scheduleEnabled)
                        .build())
                .knowledge(WorkbenchSummary.KnowledgeInfo.builder()
                        .ragTagCount(ragTagCount)
                        .ragTaskTotal(ragTaskTotal)
                        .ragTaskProcessing(ragTaskProcessing)
                        .ragTaskFailedRecent(ragFailedRecent)
                        .build())
                .todo(WorkbenchSummary.TodoInfo.builder()
                        .writeBlockedForSuperAdmin(false)
                        .build())
                .guideSteps(steps)
                .build();
    }

    private List<WorkbenchSummary.GuideStep> buildGuideSteps(long modelEnabled,
                                                             Long activeChatModelId,
                                                             long ragTagCount,
                                                             long agentTotal,
                                                             long agentPublished,
                                                             long toolPolicyTotal,
                                                             long approvalsPending,
                                                             long scheduleEnabled) {
        List<WorkbenchSummary.GuideStep> steps = new ArrayList<>();

        // 1) 模型
        boolean modelOk = modelEnabled > 0 && activeChatModelId != null;
        steps.add(step("models", "配置并激活模型", modelOk ? "DONE" : "TODO",
                modelOk ? null : "需至少启用 1 个模型并激活对话模型",
                "/models", "去配置", false));

        // 2) Prompt
        boolean promptOk = true; // GLOBAL 模板为平台内置，允许为空；ORG 模板按需创建
        String promptMsg = null;
        steps.add(step("templates", "准备 Prompt 模板（GLOBAL/ORG）", promptOk ? "DONE" : "TODO",
                promptMsg,
                "/templates", "去管理", true));

        // 3) 知识库
        boolean knowledgeOk = ragTagCount > 0;
        steps.add(step("knowledge", "导入知识库资料（可选）", knowledgeOk ? "DONE" : "TODO",
                knowledgeOk ? null : "未发现当前组织的知识库标签，可先导入资料",
                "/knowledge", "去导入", true));

        // 4) Agent
        boolean agentOk = agentTotal > 0;
        steps.add(step("agents", "创建 Agent", agentOk ? "DONE" : "TODO",
                agentOk ? null : "还没有 Agent，先创建一个最小可用 Agent",
                "/agents", "去创建", true));

        // 5) 工具策略
        boolean toolOk = toolPolicyTotal > 0;
        steps.add(step("toolPolicies", "配置工具策略与门禁", toolOk ? "DONE" : "TODO",
                toolOk ? null : "未配置 tool policy，HIGH 风险默认会生成审批单",
                "/tool-policies", "去配置", true));

        // 6) 发布
        boolean publishOk = agentPublished > 0;
        steps.add(step("publish", "发布 Agent 版本", publishOk ? "DONE" : (agentOk ? "TODO" : "BLOCKED"),
                publishOk ? null : (agentOk ? "至少发布 1 个版本，调度/调用默认取当前发布版本" : "先创建 Agent 再发布"),
                "/agents", "去发布", true));

        // 7) 审批
        boolean approvalOk = approvalsPending == 0;
        steps.add(step("approvals", "处理工具审批单", approvalOk ? "DONE" : "TODO",
                approvalOk ? null : ("有待审批单：" + approvalsPending),
                "/approvals", "去处理", false));

        // 8) 调度
        boolean scheduleOk = scheduleEnabled > 0;
        steps.add(step("schedules", "配置调度并运行", scheduleOk ? "DONE" : "TODO",
                scheduleOk ? null : "暂无启用调度，可配置 CRON 定时运行",
                "/schedules", "去配置", true));

        return steps;
    }

    private WorkbenchSummary.GuideStep step(String key,
                                            String title,
                                            String status,
                                            String message,
                                            String actionPath,
                                            String actionLabel,
                                            boolean writeAction) {
        return WorkbenchSummary.GuideStep.builder()
                .key(key)
                .title(title)
                .status(status)
                .message(message)
                .actionPath(actionPath)
                .actionLabel(actionLabel)
                .writeAction(writeAction)
                .build();
    }
}
