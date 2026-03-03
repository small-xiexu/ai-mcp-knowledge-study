package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.GatewayManageAppService;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpGatewayAuthRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpGatewayRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolBindingRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolMappingRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolRegistryRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolSchemaRepository;
import com.xbk.knowledge.domain.llm.adapter.repository.ModelConfigRepository;
import com.xbk.knowledge.domain.gateway.model.entity.McpGateway;
import com.xbk.knowledge.domain.gateway.model.entity.McpGatewayAuth;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolBinding;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolMapping;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolRegistry;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.common.model.valobj.EnabledQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayIdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.GatewayPageQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolMappingQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolBindingQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolRegistryPageQuery;
import com.xbk.knowledge.types.common.PageParamUtils;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.enums.ToolBindType;
import com.xbk.knowledge.types.exception.BusinessException;
import com.xbk.knowledge.types.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Gateway 管理应用服务实现。
 *
 * 职责：统一网关资产删除时的级联清理逻辑，确保无外键场景下数据一致性。
 *
 * @author sxie
 */
@Service
@RequiredArgsConstructor
public class GatewayManageAppServiceImpl implements GatewayManageAppService {

    /**
     * 默认网关标识。
     */
    private static final String DEFAULT_GATEWAY_ID = "default_gateway";

    /**
     * 网关实例仓储。
     */
    private final McpGatewayRepository gatewayRepository;

    /**
     * 网关鉴权仓储。
     */
    private final McpGatewayAuthRepository gatewayAuthRepository;

    /**
     * 工具注册仓储。
     */
    private final McpToolRegistryRepository toolRegistryRepository;

    /**
     * 工具映射仓储。
     */
    private final McpToolMappingRepository toolMappingRepository;

    /**
     * 工具绑定仓储。
     */
    private final McpToolBindingRepository toolBindingRepository;

    /**
     * 工具 Schema 仓储。
     */
    private final McpToolSchemaRepository toolSchemaRepository;

    /**
     * 模型配置仓储。
     */
    private final ModelConfigRepository modelConfigRepository;

    /**
     * 确保网关存在，不存在时抛出异常。
     *
     * @param gatewayId 网关 ID。
     * @return 已存在的网关实例。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpGateway ensureGatewayExists(String gatewayId) {
        String resolvedGatewayId = resolveGatewayId(gatewayId);
        return gatewayRepository.findByGatewayId(new GatewayIdQuery(resolvedGatewayId))
                .orElseThrow(() -> new NotFoundException("网关不存在: " + resolvedGatewayId));
    }

    /**
     * 查询网关实例分页数据。
     *
     * @param pageNum 页码（从 1 开始）。
     * @param pageSize 每页大小。
     * @return 网关分页结果。
     */
    @Override
    public PageResult<McpGateway> queryGatewayInstancePage(Integer pageNum, Integer pageSize) {
        int safePageNum = PageParamUtils.normalizePageNum(pageNum);
        int safePageSize = PageParamUtils.normalizePageSize(pageSize, 10);
        int offset = PageParamUtils.pageNumToOffset(safePageNum, safePageSize);

        List<McpGateway> gateways = gatewayRepository.findPage(new GatewayPageQuery(offset, safePageSize));
        long total = gatewayRepository.countAll();
        return PageResult.of(gateways, total, safePageNum, safePageSize);
    }

    /**
     * 创建或更新网关实例。
     *
     * @param id 主键（为空表示创建）。
     * @param gatewayId 网关 ID。
     * @param gatewayName 网关名称。
     * @param gatewayDesc 网关描述。
     * @param gatewayVersion 网关版本。
     * @param gatewayInstructions 网关说明。
     * @param status 状态。
     * @return 保存后的网关实例。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpGateway saveGatewayInstance(Long id,
                                          String gatewayId,
                                          String gatewayName,
                                          String gatewayDesc,
                                          String gatewayVersion,
                                          String gatewayInstructions,
                                          Integer status) {
        if (!StringUtils.hasText(gatewayName)) {
            throw new IllegalArgumentException("gatewayName 不能为空");
        }
        String resolvedGatewayId = resolveGatewayId(gatewayId);

        McpGateway gateway;
        if (id == null) {
            gateway = gatewayRepository.findByGatewayId(new GatewayIdQuery(resolvedGatewayId)).orElse(null);
            if (gateway == null) {
                gateway = new McpGateway();
                gateway.setCreatedAt(LocalDateTime.now());
            }
        } else {
            gateway = gatewayRepository.findById(new IdQuery(id)).orElse(new McpGateway());
            gateway.setId(id);
        }

        gateway.setGatewayId(resolvedGatewayId);
        gateway.setGatewayName(gatewayName);
        gateway.setGatewayDesc(gatewayDesc);
        gateway.setGatewayVersion(gatewayVersion);
        gateway.setGatewayInstructions(gatewayInstructions);
        gateway.setStatus(status == null ? 1 : status);
        gateway.setUpdatedAt(LocalDateTime.now());
        return gatewayRepository.save(gateway);
    }

    /**
     * 统计网关下工具数量。
     *
     * @param gatewayId 网关 ID。
     * @return 工具数量。
     */
    @Override
    public long countToolsByGatewayId(String gatewayId) {
        if (!StringUtils.hasText(gatewayId)) {
            return 0L;
        }
        return toolRegistryRepository.countByGatewayId(new GatewayIdQuery(gatewayId));
    }

    /**
     * 查询网关凭证分页数据。
     *
     * @param gatewayId 网关 ID。
     * @param status 状态过滤条件。
     * @param apiKeyKeyword API Key 关键字。
     * @param pageNum 页码（从 1 开始）。
     * @param pageSize 每页大小。
     * @return 网关凭证分页结果。
     */
    @Override
    public PageResult<McpGatewayAuth> queryGatewayAuthPage(String gatewayId,
                                                           Integer status,
                                                           String apiKeyKeyword,
                                                           Integer pageNum,
                                                           Integer pageSize) {
        int safePageNum = PageParamUtils.normalizePageNum(pageNum);
        int safePageSize = PageParamUtils.normalizePageSize(pageSize, 10);

        List<McpGatewayAuth> authList = gatewayAuthRepository.findByGatewayId(new GatewayIdQuery(gatewayId));
        List<McpGatewayAuth> filtered = filterGatewayAuthList(authList, status, apiKeyKeyword);
        int start = Math.min((safePageNum - 1) * safePageSize, filtered.size());
        int end = Math.min(start + safePageSize, filtered.size());

        List<McpGatewayAuth> pageRecords = new ArrayList<>();
        for (int i = start; i < end; i++) {
            pageRecords.add(filtered.get(i));
        }
        return PageResult.of(pageRecords, (long) filtered.size(), safePageNum, safePageSize);
    }

    /**
     * 查询网关工具分页数据。
     *
     * @param gatewayId 网关 ID。
     * @param toolNameKeyword 工具名称关键字。
     * @param toolDescriptionKeyword 工具描述关键字。
     * @param status 状态过滤条件。
     * @param pageNum 页码（从 1 开始）。
     * @param pageSize 每页大小。
     * @return 工具分页结果。
     */
    @Override
    public PageResult<McpToolRegistry> queryToolPage(String gatewayId,
                                                     String toolNameKeyword,
                                                     String toolDescriptionKeyword,
                                                     Integer status,
                                                     Integer pageNum,
                                                     Integer pageSize) {
        if (!StringUtils.hasText(gatewayId)) {
            throw new IllegalArgumentException("gatewayId 不能为空");
        }
        int safePageNum = PageParamUtils.normalizePageNum(pageNum);
        int safePageSize = PageParamUtils.normalizePageSize(pageSize, 10);
        int offset = PageParamUtils.pageNumToOffset(safePageNum, safePageSize);

        List<McpToolRegistry> allRecords =
                toolRegistryRepository.findPage(new ToolRegistryPageQuery(gatewayId, offset, safePageSize * 10));
        List<McpToolRegistry> filtered = filterToolList(allRecords, toolNameKeyword, toolDescriptionKeyword, status);

        int fromIndex = Math.min((safePageNum - 1) * safePageSize, filtered.size());
        int toIndex = Math.min(fromIndex + safePageSize, filtered.size());
        List<McpToolRegistry> pageRecords = new ArrayList<>();
        for (int i = fromIndex; i < toIndex; i++) {
            pageRecords.add(filtered.get(i));
        }
        return PageResult.of(pageRecords, (long) filtered.size(), safePageNum, safePageSize);
    }

    /**
     * 查询单个工具详情及映射配置。
     *
     * @param toolId 工具主键。
     * @return 工具详情。
     */
    @Override
    public ToolDetail queryToolDetail(Long toolId) {
        if (toolId == null) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        McpToolRegistry tool = toolRegistryRepository.findById(new IdQuery(toolId)).orElse(null);
        if (tool == null) {
            throw new NotFoundException("工具不存在");
        }
        List<McpToolMapping> requestMappings = toolMappingRepository.findByToolIdAndMappingType(
                new ToolMappingQuery(tool.getId(), "request")
        );
        List<McpToolMapping> responseMappings = toolMappingRepository.findByToolIdAndMappingType(
                new ToolMappingQuery(tool.getId(), "response")
        );
        return ToolDetail.builder()
                .tool(tool)
                .requestMappings(requestMappings)
                .responseMappings(responseMappings)
                .build();
    }

    /**
     * 创建或更新工具配置。
     *
     * @param command 保存命令。
     * @return 保存后的工具实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpToolRegistry saveTool(ToolSaveCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        if (!StringUtils.hasText(command.getToolName())) {
            throw new IllegalArgumentException("toolName 不能为空");
        }
        if (!StringUtils.hasText(command.getGatewayId())) {
            throw new IllegalArgumentException("gatewayId 不能为空");
        }

        McpToolRegistry tool;
        if (command.getId() == null) {
            tool = new McpToolRegistry();
            tool.setCreatedAt(LocalDateTime.now());
        } else {
            tool = toolRegistryRepository.findById(new IdQuery(command.getId())).orElse(new McpToolRegistry());
            tool.setId(command.getId());
        }

        tool.setGatewayId(command.getGatewayId().trim());
        tool.setToolName(command.getToolName());
        tool.setToolDescription(command.getToolDescription());
        tool.setHttpMethod(command.getHttpMethod());
        tool.setHttpUrl(command.getHttpUrl());
        tool.setHttpHeaders(command.getHttpHeaders());
        tool.setTimeout(command.getTimeout() == null ? 30000 : command.getTimeout());
        tool.setRetryTimes(command.getRetryTimes() == null ? 0 : command.getRetryTimes());
        tool.setStatus(command.getStatus() == null ? 1 : command.getStatus());
        tool.setUpdatedAt(LocalDateTime.now());

        McpToolRegistry saved = toolRegistryRepository.save(tool);
        if (saved == null || saved.getId() == null) {
            throw new BusinessException("工具保存失败：未生成 toolId");
        }
        toolMappingRepository.deleteByToolId(saved.getId());
        toolSchemaRepository.deleteByToolId(saved.getId());
        saveMappings(saved.getId(), saved.getGatewayId(), command.getRequestMappings(), "request");
        saveMappings(saved.getId(), saved.getGatewayId(), command.getResponseMappings(), "response");
        return saved;
    }

    /**
     * 更新工具状态。
     *
     * @param id 工具主键。
     * @param status 目标状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateToolStatus(Long id, int status) {
        if (id == null) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        McpToolRegistry tool = toolRegistryRepository.findById(new IdQuery(id)).orElse(null);
        if (tool == null) {
            throw new NotFoundException("工具不存在");
        }
        tool.setStatus(status);
        tool.setUpdatedAt(LocalDateTime.now());
        toolRegistryRepository.save(tool);
    }

    /**
     * 查询模型绑定的工具列表。
     *
     * @param modelId 模型 ID。
     * @return 绑定列表。
     */
    @Override
    public List<McpToolBinding> queryModelBindings(Long modelId) {
        if (modelId == null) {
            throw new IllegalArgumentException("modelId 不能为空");
        }
        return toolBindingRepository.findByBindTypeAndTargetId(
                new ToolBindingQuery(ToolBindType.MODEL.name(), modelId)
        );
    }

    /**
     * 保存模型绑定工具列表（覆盖）。
     *
     * @param modelId 模型 ID。
     * @param toolIds 工具 ID 列表。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveModelBindings(Long modelId, List<Long> toolIds) {
        if (modelId == null) {
            throw new IllegalArgumentException("modelId 不能为空");
        }
        List<McpToolBinding> existing = toolBindingRepository.findByBindTypeAndTargetId(
                new ToolBindingQuery(ToolBindType.MODEL.name(), modelId)
        );
        for (McpToolBinding binding : existing) {
            if (binding != null && binding.getId() != null) {
                toolBindingRepository.deleteById(binding.getId());
            }
        }

        if (CollectionUtils.isEmpty(toolIds)) {
            return;
        }
        for (Long toolId : toolIds) {
            if (toolId == null) {
                continue;
            }
            McpToolRegistry tool = toolRegistryRepository.findById(new IdQuery(toolId)).orElse(null);
            if (tool == null) {
                continue;
            }
            McpToolBinding binding = McpToolBinding.builder()
                    .gatewayId(tool.getGatewayId())
                    .toolId(toolId)
                    .bindType(ToolBindType.MODEL.name())
                    .bindTargetId(modelId)
                    .enabled(Boolean.TRUE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            toolBindingRepository.save(binding);
        }
    }

    /**
     * 查询所有已启用工具。
     *
     * @return 工具列表。
     */
    @Override
    public List<McpToolRegistry> listAllEnabledTools() {
        List<McpToolRegistry> result = new ArrayList<>();
        List<McpGateway> gateways = gatewayRepository.findAllEnabled();
        for (McpGateway gateway : gateways) {
            result.addAll(toolRegistryRepository.findEnabledByGatewayId(new GatewayIdQuery(gateway.getGatewayId())));
        }
        return result;
    }

    /**
     * 查询所有已启用模型。
     *
     * @return 模型列表。
     */
    @Override
    public List<ModelConfig> listEnabledModels() {
        return modelConfigRepository.findByEnabled(new EnabledQuery(true));
    }

    /**
     * 查询工具刷新目标列表。
     *
     * @param gatewayId 网关 ID。
     * @param toolId 可选工具 ID。
     * @return 待刷新工具列表。
     */
    @Override
    public List<McpToolRegistry> queryToolsForRefresh(String gatewayId, Long toolId) {
        if (!StringUtils.hasText(gatewayId)) {
            throw new IllegalArgumentException("gatewayId 不能为空");
        }
        if (toolId != null) {
            McpToolRegistry tool = toolRegistryRepository.findById(new IdQuery(toolId)).orElse(null);
            if (tool == null || !gatewayId.equals(tool.getGatewayId())) {
                throw new NotFoundException("工具不存在或不属于该网关");
            }
            return Collections.singletonList(tool);
        }
        return toolRegistryRepository.findByGatewayId(new GatewayIdQuery(gatewayId));
    }

    /**
     * 创建或更新网关凭证。
     *
     * @param id 凭证主键（为空表示创建）。
     * @param gatewayId 网关 ID（更新时可为空，表示沿用原网关）。
     * @param apiKey API Key（为空时更新沿用原值，创建自动生成）。
     * @param rateLimit 限流阈值。
     * @param expireTime 过期时间。
     * @param status 状态。
     * @return 保存后的网关凭证。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpGatewayAuth saveGatewayAuth(Long id,
                                          String gatewayId,
                                          String apiKey,
                                          Integer rateLimit,
                                          LocalDateTime expireTime,
                                          Integer status) {
        boolean createMode = id == null;
        McpGatewayAuth auth;
        String resolvedGatewayId;
        if (createMode) {
            if (!StringUtils.hasText(gatewayId)) {
                throw new IllegalArgumentException("gatewayId 不能为空");
            }
            auth = new McpGatewayAuth();
            auth.setCreatedAt(LocalDateTime.now());
            resolvedGatewayId = gatewayId.trim();
        } else {
            auth = gatewayAuthRepository.findById(id).orElse(null);
            if (auth == null) {
                throw new NotFoundException("凭证不存在");
            }
            auth.setId(id);
            resolvedGatewayId = StringUtils.hasText(gatewayId) ? gatewayId.trim() : auth.getGatewayId();
        }

        String resolvedApiKey = resolveApiKey(apiKey, auth.getApiKey(), createMode);
        if (!StringUtils.hasText(resolvedApiKey)) {
            throw new IllegalArgumentException("apiKey 不能为空");
        }

        List<McpGatewayAuth> existingAuthList = gatewayAuthRepository.findByGatewayId(new GatewayIdQuery(resolvedGatewayId));
        for (McpGatewayAuth existing : existingAuthList) {
            if (existing == null || existing.getId() == null || !resolvedApiKey.equals(existing.getApiKey())) {
                continue;
            }
            if (createMode || !existing.getId().equals(id)) {
                throw new IllegalArgumentException("同一网关下 API Key 已存在");
            }
        }

        auth.setGatewayId(resolvedGatewayId);
        auth.setApiKey(resolvedApiKey);
        auth.setRateLimit(resolveRateLimit(rateLimit));
        auth.setExpireTime(expireTime);
        auth.setStatus(resolveStatus(status));
        auth.setUpdatedAt(LocalDateTime.now());
        return gatewayAuthRepository.save(auth);
    }

    /**
     * 更新网关凭证状态。
     *
     * @param id 凭证主键。
     * @param status 目标状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGatewayAuthStatus(Long id, int status) {
        if (id == null) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        McpGatewayAuth auth = gatewayAuthRepository.findById(id).orElse(null);
        if (auth == null) {
            throw new NotFoundException("凭证不存在");
        }
        auth.setStatus(status);
        auth.setUpdatedAt(LocalDateTime.now());
        gatewayAuthRepository.save(auth);
    }

    /**
     * 删除网关实例并执行应用层级联清理
     * 1. 删除网关下工具映射/绑定/schema
     * 2. 删除网关工具资产
     * 3. 删除网关凭证
     * 4. 删除网关实例
     * 
     * @param query 主键查询条件。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGatewayInstance(IdQuery query) {
        if (query == null || query.getId() == null) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        McpGateway gateway = gatewayRepository.findById(query).orElse(null);
        if (gateway == null) {
            throw new NotFoundException("网关不存在");
        }
        GatewayIdQuery gatewayIdQuery = new GatewayIdQuery(gateway.getGatewayId());
        List<McpToolRegistry> tools = toolRegistryRepository.findByGatewayId(gatewayIdQuery);
        for (McpToolRegistry tool : tools) {
            if (tool == null || tool.getId() == null) {
                continue;
            }
            deleteToolCascade(tool.getId());
        }
        gatewayAuthRepository.deleteByGatewayId(gatewayIdQuery);
        gatewayRepository.deleteById(query);
    }

    /**
     * 删除工具并执行应用层级联清理
     * 1. 删除 request/response 参数映射
     * 2. 删除工具绑定
     * 3. 删除工具 schema
     * 4. 删除工具资产
     * 
     * @param query 主键查询条件。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTool(IdQuery query) {
        if (query == null || query.getId() == null) {
            throw new IllegalArgumentException("ID 不能为空");
        }
        Long toolId = query.getId();
        McpToolRegistry tool = toolRegistryRepository.findById(query).orElse(null);
        if (tool == null) {
            throw new NotFoundException("工具不存在");
        }
        deleteToolCascade(toolId);
    }

    private void deleteToolCascade(Long toolId) {
        toolMappingRepository.deleteByToolId(toolId);
        toolBindingRepository.deleteByToolId(toolId);
        toolSchemaRepository.deleteByToolId(toolId);
        toolRegistryRepository.deleteById(new IdQuery(toolId));
    }

    /**
     * 按查询条件过滤工具列表。
     *
     * @param toolList 原始工具列表。
     * @param toolNameKeyword 名称关键字。
     * @param toolDescriptionKeyword 描述关键字。
     * @param statusFilter 状态过滤条件。
     * @return 过滤后的工具列表。
     */
    private List<McpToolRegistry> filterToolList(List<McpToolRegistry> toolList,
                                                 String toolNameKeyword,
                                                 String toolDescriptionKeyword,
                                                 Integer statusFilter) {
        List<McpToolRegistry> filtered = new ArrayList<>();
        if (CollectionUtils.isEmpty(toolList)) {
            return filtered;
        }
        String nameKeyword = StringUtils.hasText(toolNameKeyword) ? toolNameKeyword.toLowerCase().trim() : null;
        String descriptionKeyword = StringUtils.hasText(toolDescriptionKeyword) ? toolDescriptionKeyword.toLowerCase().trim() : null;
        for (McpToolRegistry tool : toolList) {
            if (tool == null) {
                continue;
            }
            if (StringUtils.hasText(nameKeyword)) {
                String toolName = tool.getToolName();
                if (!StringUtils.hasText(toolName) || !toolName.toLowerCase().contains(nameKeyword)) {
                    continue;
                }
            }
            if (StringUtils.hasText(descriptionKeyword)) {
                String toolDescription = tool.getToolDescription();
                if (!StringUtils.hasText(toolDescription) || !toolDescription.toLowerCase().contains(descriptionKeyword)) {
                    continue;
                }
            }
            if (statusFilter != null && !statusFilter.equals(tool.getStatus())) {
                continue;
            }
            filtered.add(tool);
        }
        return filtered;
    }

    /**
     * 保存字段映射定义。
     *
     * @param toolId 工具 ID。
     * @param gatewayId 网关 ID。
     * @param mappings 映射定义列表。
     * @param mappingType 映射类型。
     */
    private void saveMappings(Long toolId,
                              String gatewayId,
                              List<ToolMappingNode> mappings,
                              String mappingType) {
        if (CollectionUtils.isEmpty(mappings)) {
            return;
        }
        int[] sortOrder = new int[]{0};
        for (ToolMappingNode mapping : mappings) {
            saveMappingNode(toolId, gatewayId, mappingType, mapping, null, sortOrder);
        }
    }

    /**
     * 递归保存单个映射节点。
     *
     * @param toolId 工具 ID。
     * @param gatewayId 网关 ID。
     * @param mappingType 映射类型。
     * @param mapping 映射节点。
     * @param parentId 父节点 ID。
     * @param sortOrder 序号游标。
     */
    private void saveMappingNode(Long toolId,
                                 String gatewayId,
                                 String mappingType,
                                 ToolMappingNode mapping,
                                 Long parentId,
                                 int[] sortOrder) {
        if (mapping == null || !StringUtils.hasText(mapping.getFieldName())) {
            return;
        }
        McpToolMapping entity = McpToolMapping.builder()
                .gatewayId(gatewayId)
                .toolId(toolId)
                .mappingType(mappingType)
                .parentId(parentId == null ? mapping.getParentId() : parentId)
                .fieldName(mapping.getFieldName())
                .mcpType(mapping.getMcpType())
                .mcpDesc(mapping.getMcpDesc())
                .isRequired(mapping.getIsRequired())
                .itemType(mapping.getItemType())
                .itemRefId(mapping.getItemRefId())
                .httpPath(mapping.getHttpPath())
                .httpLocation(mapping.getHttpLocation())
                .sortOrder(mapping.getSortOrder() == null ? sortOrder[0] : mapping.getSortOrder())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        McpToolMapping saved = toolMappingRepository.save(entity);
        if (saved == null || saved.getId() == null) {
            throw new BusinessException("工具映射保存失败：未生成 mappingId");
        }
        sortOrder[0]++;
        if (!CollectionUtils.isEmpty(mapping.getChildren())) {
            for (ToolMappingNode child : mapping.getChildren()) {
                saveMappingNode(toolId, gatewayId, mappingType, child, saved.getId(), sortOrder);
            }
        }
    }

    /**
     * 按查询条件过滤网关凭证列表。
     *
     * @param authList 网关下的原始凭证列表。
     * @param statusFilter 状态过滤条件。
     * @param apiKeyKeyword API Key 关键字。
     * @return 过滤后的凭证列表。
     */
    private List<McpGatewayAuth> filterGatewayAuthList(List<McpGatewayAuth> authList,
                                                       Integer statusFilter,
                                                       String apiKeyKeyword) {
        List<McpGatewayAuth> filtered = new ArrayList<>();
        if (CollectionUtils.isEmpty(authList)) {
            return filtered;
        }
        String keyword = StringUtils.hasText(apiKeyKeyword) ? apiKeyKeyword.trim() : null;
        for (McpGatewayAuth auth : authList) {
            if (!matchesGatewayAuthFilter(auth, statusFilter, keyword)) {
                continue;
            }
            filtered.add(auth);
        }
        return filtered;
    }

    /**
     * 判断单条凭证是否命中过滤条件。
     *
     * @param auth 凭证对象。
     * @param statusFilter 状态过滤条件。
     * @param keyword API Key 关键字。
     * @return 是否命中筛选条件。
     */
    private boolean matchesGatewayAuthFilter(McpGatewayAuth auth, Integer statusFilter, String keyword) {
        if (auth == null) {
            return false;
        }
        if (statusFilter != null && !statusFilter.equals(auth.getStatus())) {
            return false;
        }
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String currentApiKey = auth.getApiKey();
        return StringUtils.hasText(currentApiKey) && currentApiKey.contains(keyword);
    }

    /**
     * 解析限流值。
     *
     * @param rateLimit 限流值。
     * @return 解析结果。
     */
    private int resolveRateLimit(Integer rateLimit) {
        if (rateLimit == null || rateLimit <= 0) {
            return 100;
        }
        return rateLimit;
    }

    /**
     * 解析状态。
     *
     * @param status 状态值。
     * @return 解析结果。
     */
    private int resolveStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            return 1;
        }
        return status;
    }

    /**
     * 解析 API Key。
     *
     * @param inputApiKey 输入的 API Key。
     * @param existingApiKey 已存在 API Key。
     * @param createMode 创建模式。
     * @return 解析后的 API Key。
     */
    private String resolveApiKey(String inputApiKey, String existingApiKey, boolean createMode) {
        if (StringUtils.hasText(inputApiKey)) {
            return inputApiKey.trim();
        }
        if (StringUtils.hasText(existingApiKey)) {
            return existingApiKey.trim();
        }
        if (!createMode) {
            return null;
        }
        return "gk_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 解析网关 ID，为空时回退默认网关。
     *
     * @param gatewayId 网关 ID。
     * @return 解析后的网关 ID。
     */
    private String resolveGatewayId(String gatewayId) {
        if (!StringUtils.hasText(gatewayId)) {
            return DEFAULT_GATEWAY_ID;
        }
        return gatewayId.trim();
    }
}
