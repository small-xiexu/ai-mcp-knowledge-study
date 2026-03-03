package com.xbk.knowledge.domain.agent.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.domain.agent.model.entity.Agent;
import com.xbk.knowledge.domain.agent.model.entity.AgentVersion;
import com.xbk.knowledge.domain.agent.model.entity.PromptTemplate;
import com.xbk.knowledge.domain.agent.model.valobj.AgentClientProfileStep;
import com.xbk.knowledge.domain.agent.model.valobj.AgentCodeQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentPlanningConfig;
import com.xbk.knowledge.domain.agent.model.valobj.AgentVersionIdQuery;
import com.xbk.knowledge.domain.agent.model.valobj.AgentVersionPageQuery;
import com.xbk.knowledge.domain.agent.model.valobj.PromptTemplateIdQuery;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentRepository;
import com.xbk.knowledge.domain.agent.adapter.repository.AgentVersionRepository;
import com.xbk.knowledge.domain.agent.adapter.repository.PromptTemplateRepository;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.client.adapter.repository.ClientProfileRepository;
import com.xbk.knowledge.domain.client.model.entity.ClientProfile;
import com.xbk.knowledge.domain.client.model.entity.ClientProfileStep;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.llm.service.IModelConfigService;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowVersionRepository;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowVersionIdQuery;
import com.xbk.knowledge.domain.agent.service.IAgentVersionService;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import com.xbk.knowledge.types.json.JsonMapUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AgentVersion 领域服务实现。
 *
 * 说明：P0 优先保证草稿/发布/回滚闭环与发布快照固化。
 *
 * @author sxie
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentVersionServiceImpl implements IAgentVersionService {
    /**
     * Prompt 模板占位符匹配规则（示例{{var}}）。
     */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_\\-.]+)\\s*}}");

    /**
     * Agent 仓储，负责 Agent 主体数据访问。
     */
    private final AgentRepository agentRepository;

    /**
     * AgentVersion 仓储，负责版本草稿/发布态持久化。
     */
    private final AgentVersionRepository agentVersionRepository;

    /**
     * PromptTemplate 仓储，负责模板查询与版本固化读取。
     */
    private final PromptTemplateRepository promptTemplateRepository;

    /**
     * Workflow 版本仓储，用于校验绑定的工作流版本是否存在。
     */
    private final WorkflowVersionRepository workflowVersionRepository;

    /**
     * ClientProfile 仓储，用于读取客户端链路配置。
     */
    private final ClientProfileRepository clientProfileRepository;

    /**
     * 模型配置领域服务，用于校验模型可用性与状态。
     */
    private final IModelConfigService modelConfigService;

    /**
     * JSON 序列化组件，用于处理规划/链路配置 JSON 解析。
     */
    private final ObjectMapper objectMapper;

    /**
     * 查询Agent 版本。
     *
     * @param query 分页查询条件
     * @return AgentVersion 分页数据
     */
    @Override
    public PageResult<AgentVersion> queryPage(AgentVersionPageQuery query) {
        if (query == null || query.getAgentId() == null) {
            throw new IllegalArgumentException("agentId 不能为空");
        }
        int offset = query.getOffset() == null ? 0 : query.getOffset();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        AgentVersionPageQuery normalized = new AgentVersionPageQuery(
                query.getAgentId(),
                offset,
                pageSize
        );
        List<AgentVersion> records = agentVersionRepository.findPage(normalized);
        long total = agentVersionRepository.count(normalized);
        int pageNum = (offset / pageSize) + 1;
        return PageResult.of(records, total, pageNum, pageSize);
    }

    /**
     * 查询Agent 版本。
     *
     * @param query 主键查询条件
     * @return AgentVersion 详情
     */
    @Override
    public AgentVersion queryById(AgentVersionIdQuery query) {
        if (query == null || query.getId() == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        return agentVersionRepository
                .findById(query)
                .orElseThrow(() -> new NotFoundException("AgentVersion 不存在，id: " + query.getId()));
    }

    /**
     * 创建并持久化Agent 版本数据。
     *
     * @param draft 草稿版本实体
     * @return 创建后的 AgentVersion 信息
     */
    @Override
    public AgentVersion createDraft(AgentVersion draft) {
        if (draft == null || draft.getAgentId() == null) {
            throw new IllegalArgumentException("agentId 不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        draft.setState("DRAFT");
        if (!StringUtils.hasText(draft.getOutputContractVersion())) {
            draft.setOutputContractVersion("v1");
        }
        if (!StringUtils.hasText(draft.getRagMode())) {
            draft.setRagMode("OPTIONAL");
        }
        if (draft.getTemperature() == null) {
            draft.setTemperature(new BigDecimal("0.70"));
        }
        if (draft.getRepairRetryTimes() == null) {
            draft.setRepairRetryTimes(2);
        }
        if (draft.getTimeoutMs() == null) {
            draft.setTimeoutMs(60000);
        }
        if (draft.getMaxTurns() == null) {
            draft.setMaxTurns(20);
        }

        Integer versionNo = draft.getVersionNo();
        if (versionNo == null) {
            Integer max = agentVersionRepository.findMaxVersionNo(draft.getAgentId());
            versionNo = max == null ? 1 : (max + 1);
        } else {
            if (agentVersionRepository.existsByAgentIdAndVersionNo(draft.getAgentId(), versionNo)) {
                throw new BusinessException("versionNo 已存在" + versionNo);
            }
        }
        draft.setVersionNo(versionNo);
        draft.setCreatedAt(now);
        draft.setUpdatedAt(now);
        return agentVersionRepository.insert(draft);
    }

    /**
     * 更新Agent 版本数据。
     *
     * @param draft 草稿版本实体
     * @return 更新后的 AgentVersion 信息
     */
    @Override
    public AgentVersion updateDraft(AgentVersion draft) {
        if (draft == null || draft.getId() == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        AgentVersion existed = queryById(new AgentVersionIdQuery(draft.getId()));
        if (!"DRAFT".equalsIgnoreCase(existed.getState())) {
            throw new BusinessException("仅 DRAFT 版本允许编辑");
        }

        existed.setChangeSummary(draft.getChangeSummary());
        existed.setPromptTemplateId(draft.getPromptTemplateId());
        existed.setTemplateParamsJson(draft.getTemplateParamsJson());
        existed.setOutputContractVersion(defaultIfBlank(draft.getOutputContractVersion(), existed.getOutputContractVersion()));
        existed.setOutputContractOptionsJson(draft.getOutputContractOptionsJson());
        existed.setRagMode(defaultIfBlank(draft.getRagMode(), existed.getRagMode()));
        existed.setDefaultRagTagsJson(draft.getDefaultRagTagsJson());
        existed.setAllowedRagTagsJson(draft.getAllowedRagTagsJson());
        existed.setAllowedToolKeysJson(draft.getAllowedToolKeysJson());
        existed.setClientProfileId(draft.getClientProfileId());
        existed.setClientChainJson(draft.getClientChainJson());
        existed.setPlanningConfigJson(draft.getPlanningConfigJson());
        existed.setTimeoutMs(draft.getTimeoutMs());
        existed.setMaxTurns(draft.getMaxTurns());
        existed.setTemperature(draft.getTemperature());
        existed.setRepairRetryTimes(draft.getRepairRetryTimes());
        existed.setUpdatedBy(draft.getUpdatedBy());
        existed.setUpdatedAt(LocalDateTime.now());

        int affected = agentVersionRepository.updateDraft(existed);
        if (affected <= 0) {
            throw new BusinessException("草稿更新失败，id: " + draft.getId());
        }
        return queryById(new AgentVersionIdQuery(draft.getId()));
    }

    /**
     * 发布业务配置。
     *
     * @param agentCode Agent 编码
     * @param versionId 版本 ID
     * @param operatorId 操作人 ID
     * @return 发布后的 AgentVersion 信息
     */
    @Override
    public AgentVersion publish(String agentCode, Long versionId, Long operatorId) {
        if (!StringUtils.hasText(agentCode) || versionId == null) {
            throw new IllegalArgumentException("agentCode/versionId 不能为空");
        }

        Agent agent = agentRepository
                .findByCode(new AgentCodeQuery(agentCode))
                .orElseThrow(() -> new NotFoundException("Agent 不存在，agentCode: " + agentCode));
        if (!"ENABLED".equalsIgnoreCase(agent.getStatus())) {
            throw new BusinessException("Agent 已禁用，不可发布/生效，agentCode: " + agentCode);
        }

        AgentVersion version = queryById(new AgentVersionIdQuery(versionId));
        if (!agent.getId().equals(version.getAgentId())) {
            throw new BusinessException("版本不属于目标 Agent");
        }
        if (!"DRAFT".equalsIgnoreCase(version.getState())) {
            throw new BusinessException("仅 DRAFT 版本允许发布");
        }

        // 1、 校验发布前置
        AgentPlanningConfig planningConfig = parsePlanningConfig(version.getPlanningConfigJson());
        if (Boolean.TRUE.equals(planningConfig.getEnabled())) {
            validatePlanningConfig(planningConfig);
            if (version.getWorkflowVersionId() != null
                    || version.getClientProfileId() != null
                    || StringUtils.hasText(version.getClientChainJson())) {
                throw new BusinessException("Planning 模式与 Workflow/ClientChain 互斥，请仅保留一种运行模式");
            }
            // Planning 可选绑定 Prompt 模板，作为规划阶段系统提示词。
            if (version.getPromptTemplateId() != null) {
                PromptTemplate template = resolveTemplateForPublish(version.getPromptTemplateId());
                String rendered = renderTemplate(template.getContent(), version.getTemplateParamsJson());
                version.setPromptTemplateVersionNo(template.getVersionNo());
                version.setSystemPromptSnapshot(rendered);
            } else {
                version.setPromptTemplateVersionNo(null);
                version.setSystemPromptSnapshot(null);
            }
        }
        // 优先Workflow 绑定模式（不要求 promptTemplate）
        else if (version.getWorkflowVersionId() != null) {
            // P0仅要求 workflowVersionId 非空；状态校验放宽（允许先发布 Agent，再逐步完善 Workflow 发布流程）
            // 若你希望更严格可在这里要求 workflowVersion.state == PUBLISHED
            workflowVersionRepository.findById(
                    WorkflowVersionIdQuery.builder()
                            .id(version.getWorkflowVersionId())
                            .build()
            ).orElseThrow(() -> new BusinessException("发布前要求绑定的 workflowVersionId 存在，id=" + version.getWorkflowVersionId()));
            version.setPromptTemplateVersionNo(null);
            version.setSystemPromptSnapshot(null);
        } else if (version.getClientProfileId() != null) {
            validateClientProfile(version.getClientProfileId());
            version.setPromptTemplateVersionNo(null);
            version.setSystemPromptSnapshot(null);
        } else if (StringUtils.hasText(version.getClientChainJson())) {
            validateClientChain(version.getClientChainJson());
            version.setPromptTemplateVersionNo(null);
            version.setSystemPromptSnapshot(null);
        } else {
            // Prompt 模板模式校验模板并生成发布快照
            Long templateId = version.getPromptTemplateId();
            if (templateId == null) {
                throw new BusinessException("发布前必须配置 promptTemplateId / workflowVersionId / clientProfileId / clientChainJson 之一");
            }
            PromptTemplate template = resolveTemplateForPublish(templateId);
            String rendered = renderTemplate(template.getContent(), version.getTemplateParamsJson());
            version.setPromptTemplateVersionNo(template.getVersionNo());
            version.setSystemPromptSnapshot(rendered);
        }

        // 2、 固化模板版本号与快照，并将状态切换为 PUBLISHED
        int affected = agentVersionRepository.publish(version.getId(),
                version.getPromptTemplateVersionNo(),
                version.getTemplateParamsJson(),
                version.getSystemPromptSnapshot(),
                operatorId
        );
        if (affected <= 0) {
            throw new BusinessException("版本发布失败（状态更新失败），id: " + version.getId());
        }
        // 发布后将 agent.current_published_version_id 切到该版本
        Agent toUpdate = Agent.builder()
                .agentCode(agentCode)
                .agentName(agent.getAgentName())
                .description(agent.getDescription())
                .status(agent.getStatus())
                .currentPublishedVersionId(version.getId())
                .updatedBy(operatorId)
                .updatedAt(LocalDateTime.now())
                .build();
        agentRepository.updateByCode(toUpdate);

        return queryById(new AgentVersionIdQuery(versionId));
    }

    /**
     * 回滚业务配置。
     *
     * @param agentCode Agent 编码
     * @param targetVersionId 目标版本 ID
     * @param operatorId 操作人 ID
     * @return 回滚后的 AgentVersion 信息
     */
    @Override
    public AgentVersion rollback(String agentCode, Long targetVersionId, Long operatorId) {
        if (!StringUtils.hasText(agentCode) || targetVersionId == null) {
            throw new IllegalArgumentException("agentCode/targetVersionId 不能为空");
        }
        Agent agent = agentRepository
                .findByCode(new AgentCodeQuery(agentCode))
                .orElseThrow(() -> new NotFoundException("Agent 不存在，agentCode: " + agentCode));
        AgentVersion target = queryById(new AgentVersionIdQuery(targetVersionId));
        if (!agent.getId().equals(target.getAgentId())) {
            throw new BusinessException("回滚目标版本不属于该 Agent");
        }
        if (!"PUBLISHED".equalsIgnoreCase(target.getState())) {
            throw new BusinessException("只能回滚到历史 PUBLISHED 版本");
        }
        Agent toUpdate = Agent.builder()
                .agentCode(agentCode)
                .agentName(agent.getAgentName())
                .description(agent.getDescription())
                .status(agent.getStatus())
                .currentPublishedVersionId(target.getId())
                .updatedBy(operatorId)
                .updatedAt(LocalDateTime.now())
                .build();
        int affected = agentRepository.updateByCode(toUpdate);
        if (affected <= 0) {
            throw new BusinessException("回滚失败（切换发布版本失败）");
        }
        return target;
    }

    private PromptTemplate resolveTemplateForPublish(Long templateId) {
        PromptTemplate template = promptTemplateRepository
                .findById(new PromptTemplateIdQuery(templateId))
                .orElseThrow(() -> new NotFoundException("模板不存在，id: " + templateId));
        if (!"PUBLISHED".equalsIgnoreCase(template.getState())) {
            throw new BusinessException("发布 AgentVersion 时要求模板必须为 PUBLISHED");
        }
        return template;
    }

    /**
     * 按变量参数渲染模板文本。
     *
     * @param content 用户输入内容
     * @param paramsJson 模板参数 JSON
     * @return 渲染后的模板文本
     */
    private String renderTemplate(String content, String paramsJson) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        Map<String, Object> params = parseParams(paramsJson);
        Set<String> requiredKeys = extractPlaceholderKeys(content);
        for (String key : requiredKeys) {
            if (!params.containsKey(key)) {
                throw new BusinessException("模板变量缺失" + key);
            }
        }

        Matcher matcher = PLACEHOLDER.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = params.get(key);
            String replacement = value == null ? "" : String.valueOf(value);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private Set<String> extractPlaceholderKeys(String content) {
        Set<String> keys = new TreeSet<>();
        Matcher matcher = PLACEHOLDER.matcher(content);
        while (matcher.find()) {
            keys.add(matcher.group(1));
        }
        return keys;
    }

    /**
     * 解析模板参数。
     * 
     * @param json JSON 字符串。
     */
    private Map<String, Object> parseParams(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return JsonMapUtils.readMap(objectMapper, json);
        } catch (Exception e) {
            log.warn("解析 templateParamsJson 失败，将按空对象处理");
            return Map.of();
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private void validateClientChain(String clientChainJson) {
        List<AgentClientProfileStep> steps = parseClientChain(clientChainJson);
        if (steps.isEmpty()) {
            throw new BusinessException("clientChainJson 至少需要配置一个步骤");
        }

        for (AgentClientProfileStep step : steps) {
            if (step.getModelId() == null) {
                throw new BusinessException("clientChainJson 存在未配置 modelId 的步骤");
            }
            ModelConfig model = modelConfigService.queryModelConfigById(new IdQuery(step.getModelId()));
            if (!Boolean.TRUE.equals(model.getEnabled())) {
                throw new BusinessException("clientChainJson 绑定模型未启用，modelId=" + step.getModelId());
            }
        }
    }

    private void validateClientProfile(Long clientProfileId) {
        ClientProfile profile = clientProfileRepository.findById(clientProfileId)
                .orElseThrow(() -> new NotFoundException("clientProfileId 不存在，id=" + clientProfileId));
        if (!"ENABLED".equalsIgnoreCase(profile.getStatus())) {
            throw new BusinessException("clientProfileId 未启用，id=" + clientProfileId);
        }
        List<ClientProfileStep> steps = clientProfileRepository.listSteps(clientProfileId);
        if (steps == null || steps.isEmpty()) {
            throw new BusinessException("clientProfileId 未配置步骤，id=" + clientProfileId);
        }
        for (ClientProfileStep step : steps) {
            if (step == null || step.getModelId() == null) {
                throw new BusinessException("clientProfileId 存在未配置 modelId 的步骤，id=" + clientProfileId);
            }
            ModelConfig model = modelConfigService.queryModelConfigById(new IdQuery(step.getModelId()));
            if (!Boolean.TRUE.equals(model.getEnabled())) {
                throw new BusinessException("clientProfileId 绑定模型未启用，modelId=" + step.getModelId());
            }
        }
    }

    /**
     * 解析客户端链路。
     *
     * @param clientChainJson 客户端链路 JSON
     * @return 解析后的客户端链路步骤
     */
    private List<AgentClientProfileStep> parseClientChain(String clientChainJson) {
        if (!StringUtils.hasText(clientChainJson)) {
            return List.of();
        }
        try {
            List<AgentClientProfileStep> raw = objectMapper.readValue(clientChainJson, new TypeReference<List<AgentClientProfileStep>>() {});
            if (raw == null || raw.isEmpty()) {
                return List.of();
            }
            List<AgentClientProfileStep> normalized = new ArrayList<>();
            int seq = 0;
            for (AgentClientProfileStep step : raw) {
                if (step == null) {
                    continue;
                }
                seq++;
                if (step.getSequence() == null) {
                    step.setSequence(seq);
                }
                if (!StringUtils.hasText(step.getStepName())) {
                    step.setStepName("步骤-" + step.getSequence());
                }
                normalized.add(step);
            }
            normalized.sort(Comparator.comparingInt(s -> s.getSequence() == null ? Integer.MAX_VALUE : s.getSequence()));
            return normalized;
        } catch (Exception e) {
            throw new BusinessException("clientChainJson 解析失败，请检查 JSON 结构");
        }
    }

    /**
     * 解析规划配置。
     * 
     * @param json JSON 字符串。
     * @return 解析后的规划配置。
     */
    private AgentPlanningConfig parsePlanningConfig(String json) {
        if (!StringUtils.hasText(json)) {
            return AgentPlanningConfig.builder().build();
        }
        try {
            AgentPlanningConfig config = objectMapper.readValue(json, AgentPlanningConfig.class);
            if (config == null) {
                return AgentPlanningConfig.builder().build();
            }
            if (config.getEnabled() == null) {
                config.setEnabled(false);
            }
            if (config.getRequireHumanConfirm() == null) {
                config.setRequireHumanConfirm(true);
            }
            if (config.getMaxPlanSteps() == null) {
                config.setMaxPlanSteps(6);
            }
            if (config.getReplanMaxTimes() == null) {
                config.setReplanMaxTimes(1);
            }
            if (config.getStepTimeoutMs() == null) {
                config.setStepTimeoutMs(60000);
            }
            if (config.getApprovalExpireMinutes() == null) {
                config.setApprovalExpireMinutes(120);
            }
            return config;
        } catch (Exception e) {
            throw new BusinessException("planningConfigJson 解析失败，请检查 JSON 结构");
        }
    }

    /**
     * 校验规划配置。
     * 
     * @param config 规划配置。
     */
    private void validatePlanningConfig(AgentPlanningConfig config) {
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            return;
        }
        int maxPlanSteps = config.getMaxPlanSteps() == null ? 6 : config.getMaxPlanSteps();
        if (maxPlanSteps < 1 || maxPlanSteps > 20) {
            throw new BusinessException("planning.maxPlanSteps 取值范围为 [1,20]");
        }
        int replanMaxTimes = config.getReplanMaxTimes() == null ? 1 : config.getReplanMaxTimes();
        if (replanMaxTimes < 0 || replanMaxTimes > 3) {
            throw new BusinessException("planning.replanMaxTimes 取值范围为 [0,3]");
        }
        int stepTimeoutMs = config.getStepTimeoutMs() == null ? 60000 : config.getStepTimeoutMs();
        if (stepTimeoutMs < 1000 || stepTimeoutMs > 600000) {
            throw new BusinessException("planning.stepTimeoutMs 取值范围为 [1000,600000]");
        }
        int approvalExpireMinutes = config.getApprovalExpireMinutes() == null ? 120 : config.getApprovalExpireMinutes();
        if (approvalExpireMinutes < 5 || approvalExpireMinutes > 1440) {
            throw new BusinessException("planning.approvalExpireMinutes 取值范围为 [5,1440]");
        }
        if (config.getPlannerModelId() != null) {
            ModelConfig model = modelConfigService.queryModelConfigById(new IdQuery(config.getPlannerModelId()));
            if (!Boolean.TRUE.equals(model.getEnabled())) {
                throw new BusinessException("planning.plannerModelId 未启用，id=" + config.getPlannerModelId());
            }
        }
    }
}
