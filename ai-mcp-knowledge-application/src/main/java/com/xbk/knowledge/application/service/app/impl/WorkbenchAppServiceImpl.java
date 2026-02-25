package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.model.workbench.WorkbenchSummary;
import com.xbk.knowledge.application.service.app.WorkbenchAppService;
import com.xbk.knowledge.domain.llm.model.entity.ModelActivation;
import com.xbk.knowledge.domain.agent.model.valobj.AgentPageQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentSchedulePageQuery;
import com.xbk.knowledge.domain.agent.model.valobj.PromptTemplatePageQuery;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentRepository;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentScheduleRepository;
import com.xbk.knowledge.domain.approval.adapter.repository.ApprovalRequestRepository;
import com.xbk.knowledge.domain.llm.adapter.repository.ModelActivationRepository;
import com.xbk.knowledge.domain.llm.adapter.repository.ModelConfigRepository;
import com.xbk.knowledge.domain.agent.adapter.repository.PromptTemplateRepository;
import com.xbk.knowledge.domain.rag.adapter.repository.RagTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 工作台聚合应用服务实现（方案B）。
 *
 * 说明：仅做轻量聚合与口径封装，不承载复杂业务流程。
 *
 * @author sxie
 */
@Service
@RequiredArgsConstructor
public class WorkbenchAppServiceImpl implements WorkbenchAppService {
    /**
     * Agent 仓储，用于统计智能体总量与发布量。
     */
    private final AgentRepository agentRepository;

    /**
     * PromptTemplate 仓储，用于统计模板草稿与发布数量。
     */
    private final PromptTemplateRepository promptTemplateRepository;

    /**
     * 审批单仓储，用于统计待审批数量。
     */
    private final ApprovalRequestRepository approvalRequestRepository;

    /**
     * Agent 调度仓储，用于统计调度总量与启用量。
     */
    private final AgentScheduleRepository agentScheduleRepository;

    /**
     * RAG 任务仓储，用于统计知识导入任务与标签规模。
     */
    private final RagTaskRepository ragTaskRepository;

    /**
     * 模型配置仓储，用于统计模型总量与启用量。
     */
    private final ModelConfigRepository modelConfigRepository;

    /**
     * 模型激活仓储，用于读取当前激活对话/嵌入模型。
     */
    private final ModelActivationRepository modelActivationRepository;

    /**
     * 汇总工作台首页关键指标数据。
     * 
     * @return WorkbenchSummary 数据。
     */
    @Override
    public WorkbenchSummary summary() {
        // Models
        long modelTotal = modelConfigRepository.countAll();
        long modelEnabled = modelConfigRepository.findByEnabled(new EnabledQuery(true)).size();
        ModelActivation activation = modelActivationRepository.queryActivation();
        Long activeChatModelId = activation == null ? null : activation.getChatModelId();
        Long activeEmbeddingModelId = activation == null ? null : activation.getEmbeddingModelId();

        // Agent
        long agentTotal = agentRepository.count(new AgentPageQuery(null, null, 0, 1));
        long agentPublished = agentRepository.countPublished();

        // Prompt
        long published = promptTemplateRepository.count(new PromptTemplatePageQuery(null, "PUBLISHED", 0, 1));
        long draft = promptTemplateRepository.count(new PromptTemplatePageQuery(null, "DRAFT", 0, 1));

        // Tool governance
        long approvalsPending = approvalRequestRepository.count("PENDING");

        // Schedule
        long scheduleTotal = agentScheduleRepository.count(new AgentSchedulePageQuery(null, null, null, 0, 1));
        long scheduleEnabled = agentScheduleRepository.count(new AgentSchedulePageQuery(null, null, true, 0, 1));

        // Knowledge (best effort: derive from tasks)
        long ragTaskTotal = ragTaskRepository.countAll();
        long ragTaskProcessing = ragTaskRepository.countByStatus("PROCESSING");
        long ragTagCount = ragTaskRepository.countDistinctRagTag();
        long ragFailedRecent = ragTaskRepository.countFailedTasksSince(LocalDateTime.now().minusDays(7));

        List<WorkbenchSummary.GuideStep> steps = buildGuideSteps(
                modelEnabled, activeChatModelId, ragTagCount,
                agentTotal, agentPublished, approvalsPending, scheduleEnabled);

        return WorkbenchSummary.builder()
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
                        .draft(draft)
                        .published(published)
                        .build())
                .tool(WorkbenchSummary.ToolInfo.builder()
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

    /**
     * 构建引导步骤列表。
     * 
     * @param modelEnabled 模型可用数量。
     * @param activeChatModelId 当前激活对话模型ID。
     * @param ragTagCount RAG标签数量。
     * @param agentTotal 智能体总数。
     * @param agentPublished 已发布智能体数量。
     * @param approvalsPending 待审批数量。
     * @param scheduleEnabled 启用调度数量。
     * @return 步骤集合。
     */
    private List<WorkbenchSummary.GuideStep> buildGuideSteps(long modelEnabled,
                                                             Long activeChatModelId,
                                                             long ragTagCount,
                                                             long agentTotal,
                                                             long agentPublished,
                                                             long approvalsPending,
                                                             long scheduleEnabled) {
        List<WorkbenchSummary.GuideStep> steps = new ArrayList<>();

        // 1、 模型
        boolean modelOk = modelEnabled > 0 && activeChatModelId != null;
        steps.add(step("models", "配置并激活模型", modelOk ? "DONE" : "TODO",
                modelOk ? null : "需至少启用 1 个模型并激活对话模型",
                "/models", "去配置", false));

        // 2、 Prompt
        boolean promptOk = true;
        String promptMsg = null;
        steps.add(step("templates", "准备 Prompt 模板", promptOk ? "DONE" : "TODO",
                promptMsg,
                "/templates", "去管理", true));

        // 3、 知识库
        boolean knowledgeOk = ragTagCount > 0;
        steps.add(step("knowledge", "导入知识库资料（可选）", knowledgeOk ? "DONE" : "TODO",
                knowledgeOk ? null : "未发现知识库标签，可先导入资料",
                "/knowledge", "去导入", true));

        // 4、 Agent
        boolean agentOk = agentTotal > 0;
        steps.add(step("agents", "创建 Agent", agentOk ? "DONE" : "TODO",
                agentOk ? null : "还没有 Agent，先创建一个最小可用 Agent",
                "/agents", "去创建", true));

        // 5、 发布
        boolean publishOk = agentPublished > 0;
        steps.add(step("publish", "发布 Agent 版本", publishOk ? "DONE" : (agentOk ? "TODO" : "BLOCKED"),
                publishOk ? null : (agentOk ? "至少发布 1 个版本，调度/调用默认取当前发布版本" : "先创建 Agent 再发布"),
                "/agents", "去发布", true));

        // 6、 审批
        boolean approvalOk = approvalsPending == 0;
        steps.add(step("approvals", "处理工具审批单", approvalOk ? "DONE" : "TODO",
                approvalOk ? null : ("有待审批单" + approvalsPending),
                "/approvals", "去处理", false));

        // 7、 调度
        boolean scheduleOk = scheduleEnabled > 0;
        steps.add(step("schedules", "配置调度并运行", scheduleOk ? "DONE" : "TODO",
                scheduleOk ? null : "暂无启用调度，可配置 CRON 定时运行",
                "/schedules", "去配置", true));

        return steps;
    }

    /**
     * 构建工作台引导步骤对象。
     * 
     * @param key 步骤标识。
     * @param title 步骤标题。
     * @param status 状态值。
     * @param message 提示信息。
     * @param actionPath 跳转路径。
     * @param actionLabel 操作文案。
     * @param writeAction 是否可写操作。
     * @return 引导步骤定义。
     */
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
