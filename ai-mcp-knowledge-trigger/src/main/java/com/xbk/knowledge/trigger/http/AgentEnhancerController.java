package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.IAgentEnhancerService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xbk.knowledge.api.dto.agentenhancer.*;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.application.service.app.AgentEnhancerAppService;
import com.xbk.knowledge.application.service.app.AgentEnhancerBindingAppService;
import com.xbk.knowledge.domain.agentenhancer.model.entity.AgentEnhancer;
import com.xbk.knowledge.domain.agentenhancer.model.valobj.AgentEnhancerBindingQuery;
import com.xbk.knowledge.domain.agentenhancer.model.valobj.AgentEnhancerBindingView;
import com.xbk.knowledge.domain.agentenhancer.model.valobj.AgentEnhancerPageQuery;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageQueryExecutor;
import com.xbk.knowledge.types.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;

/**
 * AgentEnhancer 资产与绑定管理接口。
 *
 * @author sxie
 */
@RestController
@RequestMapping("/api/agent-enhancers")
@RequiredArgsConstructor
public class AgentEnhancerController implements IAgentEnhancerService {

    /**
     * Agent 增强器应用服务。
     */
    private final AgentEnhancerAppService agentEnhancerAppService;

    /**
     * Agent 增强器绑定应用服务。
     */
    private final AgentEnhancerBindingAppService agentEnhancerBindingAppService;

    /**
     * 分页查询 Agent 增强器（AgentEnhancer）列表。
     * 流程：
     * 1. 进入接口后执行 `agent-enhancer:read` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 组装 `AgentEnhancerPageQuery` 并调用应用服务分页查询。
     * 4. 将领域分页结果转换为 `AgentEnhancerResponse` 分页结构。
     * 5. 统一封装 `Result.success` 返回。
     * 
     * @param request Agent 增强器（AgentEnhancer）查询条件。
     * @return Agent 增强器（AgentEnhancer）分页查询结果。
     */
    @PostMapping("/list")
    @SaCheckPermission("agent-enhancer:read")
    @Override
    public Result<PageResult<AgentEnhancerResponse>> list(@Valid @RequestBody AgentEnhancerQueryRequest request) {
        Integer enabled = request.getEnabled() == null ? null : (request.getEnabled() ? 1 : 0);
        return PageQueryExecutor.execute(
                request,
                (offset, pageSize) -> new AgentEnhancerPageQuery(
                        request.getKeyword(),
                        enabled,
                        request.getAgentEnhancerType(),
                        offset,
                        pageSize
                ),
                agentEnhancerAppService::queryPage,
                this::toResponse
        );
    }

    /**
     * 按主键查询 Agent 增强器（AgentEnhancer）详情。
     * 流程：
     * 1. 进入接口后执行 `agent-enhancer:read` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 调用 `agentEnhancerAppService.get` 查询实体。
     * 4. 将实体转换为 `AgentEnhancerResponse`。
     * 5. 统一封装 `Result.success` 返回。
     * 
     * @param request Agent 增强器（AgentEnhancer）详情查询参数。
     * @return Agent 增强器（AgentEnhancer）查询结果。
     */
    @PostMapping("/get")
    @SaCheckPermission("agent-enhancer:read")
    @Override
    public Result<AgentEnhancerResponse> get(@Valid @RequestBody IdRequest request) {
        AgentEnhancer agentEnhancer = agentEnhancerAppService.get(request.getId());
        return Result.success(toResponse(agentEnhancer));
    }

    /**
     * 创建或更新 Agent 增强器（AgentEnhancer）数据。
     * 流程：
     * 1. 进入接口后执行 `agent-enhancer:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 按请求组装 `AgentEnhancer` 领域对象。
     * 4. 调用 `agentEnhancerAppService.save` 执行新增或更新。
     * 5. 转换为 `AgentEnhancerResponse` 并返回“保存成功”。
     * 
     * @param request Agent 增强器（AgentEnhancer）保存参数。
     * @return Agent 增强器（AgentEnhancer）保存结果。
     */
    @PostMapping("/save")
    @SaCheckPermission("agent-enhancer:write")
    @Override
    public Result<AgentEnhancerResponse> save(@Valid @RequestBody AgentEnhancerSaveRequest request) {
        AgentEnhancer agentEnhancer = AgentEnhancer.builder()
                .id(request.getId())
                .agentEnhancerCode(request.getAgentEnhancerCode())
                .agentEnhancerName(request.getAgentEnhancerName())
                .agentEnhancerType(request.getAgentEnhancerType())
                .enabled(request.getEnabled() == null || request.getEnabled() ? 1 : 0)
                .configJson(request.getConfigJson())
                .build();
        AgentEnhancer saved = agentEnhancerAppService.save(agentEnhancer);
        return Result.success("保存成功", toResponse(saved));
    }

    /**
     * 启用 Agent 增强器（AgentEnhancer）。
     * 流程：
     * 1. 进入接口后执行 `agent-enhancer:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 调用 `agentEnhancerAppService.enable` 更新状态。
     * 4. 将更新结果转换为 `AgentEnhancerResponse`。
     * 5. 统一封装 `Result.success` 返回。
     * 
     * @param request Agent 增强器（AgentEnhancer）启用参数。
     * @return Agent 增强器（AgentEnhancer）启用结果。
     */
    @PostMapping("/enable")
    @SaCheckPermission("agent-enhancer:write")
    @Override
    public Result<AgentEnhancerResponse> enable(@Valid @RequestBody IdRequest request) {
        AgentEnhancer enabled = agentEnhancerAppService.enable(request.getId());
        return Result.success(toResponse(enabled));
    }

    /**
     * 禁用 Agent 增强器（AgentEnhancer）。
     * 流程：
     * 1. 进入接口后执行 `agent-enhancer:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 调用 `agentEnhancerAppService.disable` 更新状态。
     * 4. 将更新结果转换为 `AgentEnhancerResponse`。
     * 5. 统一封装 `Result.success` 返回。
     * 
     * @param request Agent 增强器（AgentEnhancer）禁用参数。
     * @return Agent 增强器（AgentEnhancer）禁用结果。
     */
    @PostMapping("/disable")
    @SaCheckPermission("agent-enhancer:write")
    @Override
    public Result<AgentEnhancerResponse> disable(@Valid @RequestBody IdRequest request) {
        AgentEnhancer disabled = agentEnhancerAppService.disable(request.getId());
        return Result.success(toResponse(disabled));
    }

    /**
     * 删除 Agent 增强器（AgentEnhancer）数据。
     * 流程：
     * 1. 进入接口后执行 `agent-enhancer:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 调用 `agentEnhancerAppService.remove` 执行删除。
     * 4. 应用层完成资源与绑定校验后落库删除。
     * 5. 统一封装空成功结果返回。
     * 
     * @param request Agent 增强器（AgentEnhancer）删除参数。
     * @return Agent 增强器（AgentEnhancer）删除结果。
     */
    @PostMapping("/remove")
    @SaCheckPermission("agent-enhancer:write")
    @Override
    public Result<Void> remove(@Valid @RequestBody IdRequest request) {
        agentEnhancerAppService.remove(request.getId());
        return Result.success();
    }

    /**
     * 查询绑定目标下的 Agent 增强器（AgentEnhancer）绑定列表。
     * 流程：
     * 1. 进入接口后执行 `agent-enhancer:read` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 组装 `AgentEnhancerBindingQuery` 并调用应用服务查询。
     * 4. 将查询结果逐条转换为 `AgentEnhancerBindingViewResponse`。
     * 5. 统一封装 `Result.success` 返回（空列表时返回 `List.of()`）。
     * 
     * @param request 绑定关系查询参数。
     * @return 绑定关系列表查询结果。
     */
    @PostMapping("/bindings/list")
    @SaCheckPermission("agent-enhancer:read")
    @Override
    public Result<List<AgentEnhancerBindingViewResponse>> listBindings(@Valid @RequestBody AgentEnhancerBindingGetRequest request) {
        AgentEnhancerBindingQuery query = new AgentEnhancerBindingQuery(request.getBindType(), request.getBindTargetId());
        List<AgentEnhancerBindingView> list = agentEnhancerBindingAppService.listBindings(query);
        if (CollectionUtils.isEmpty(list)) {
            return Result.success(List.of());
        }
        List<AgentEnhancerBindingViewResponse> resp = new ArrayList<>();
        for (AgentEnhancerBindingView v : list) {
            resp.add(toViewResponse(v));
        }
        return Result.success(resp);
    }

    /**
     * 覆盖保存绑定目标的 Agent 增强器（AgentEnhancer）绑定链路。
     * 流程：
     * 1. 进入接口后执行 `agent-enhancer:write` 权限校验。
     * 2. Spring 完成请求体绑定与参数校验（`@Valid`）。
     * 3. Controller 将请求 items 转换为应用服务可识别的保存项。
     * 4. 调用 `agentEnhancerBindingAppService.saveBindings` 持久化绑定关系。
     * 5. 返回“保存成功”的统一结果。
     * 
     * @param request 绑定关系保存参数。
     * @return 绑定关系保存结果。
     */
    @PostMapping("/bindings/save")
    @SaCheckPermission("agent-enhancer:write")
    @Override
    public Result<Void> saveBindings(@Valid @RequestBody AgentEnhancerBindingSaveRequest request) {
        List<AgentEnhancerBindingAppService.AgentEnhancerBindingSaveItem> items = new ArrayList<>();
        if (request.getItems() != null) {
            for (AgentEnhancerBindingSaveRequest.AgentEnhancerBindingSaveItem it : request.getItems()) {
                if (it == null) {
                    continue;
                }
                AgentEnhancerBindingAppService.AgentEnhancerBindingSaveItem x = new AgentEnhancerBindingAppService.AgentEnhancerBindingSaveItem();
                x.setAgentEnhancerId(it.getAgentEnhancerId());
                x.setOrderNo(it.getOrderNo());
                x.setEnabled(it.getEnabled());
                items.add(x);
            }
        }
        agentEnhancerBindingAppService.saveBindings(request.getBindType(), request.getBindTargetId(), items);
        return Result.success("保存成功", null);
    }

    /**
     * 将输入数据转换为响应。
     * 
     * @param a 增强器实体。
     * @return 增强器响应。
     */
    private AgentEnhancerResponse toResponse(AgentEnhancer a) {
        if (a == null) {
            return null;
        }
        AgentEnhancerResponse resp = new AgentEnhancerResponse();
        resp.setId(a.getId());
        resp.setAgentEnhancerCode(a.getAgentEnhancerCode());
        resp.setAgentEnhancerName(a.getAgentEnhancerName());
        resp.setAgentEnhancerType(a.getAgentEnhancerType());
        resp.setEnabled(a.getEnabled());
        resp.setConfigJson(a.getConfigJson());
        resp.setCreatedAt(a.getCreatedAt());
        resp.setUpdatedAt(a.getUpdatedAt());
        return resp;
    }

    /**
     * 将输入数据转换为View响应。
     * 
     * @param v 增强器绑定视图。
     * @return 增强器绑定视图响应。
     */
    private AgentEnhancerBindingViewResponse toViewResponse(AgentEnhancerBindingView v) {
        if (v == null) {
            return null;
        }
        AgentEnhancerBindingViewResponse resp = new AgentEnhancerBindingViewResponse();
        resp.setBindingId(v.getBindingId());
        resp.setBindType(v.getBindType());
        resp.setBindTargetId(v.getBindTargetId());
        resp.setAgentEnhancerId(v.getAgentEnhancerId());
        resp.setOrderNo(v.getOrderNo());
        resp.setBindingEnabled(v.getBindingEnabled());
        resp.setBindingCreatedAt(v.getBindingCreatedAt());
        resp.setBindingUpdatedAt(v.getBindingUpdatedAt());
        resp.setAgentEnhancerCode(v.getAgentEnhancerCode());
        resp.setAgentEnhancerName(v.getAgentEnhancerName());
        resp.setAgentEnhancerType(v.getAgentEnhancerType());
        resp.setAgentEnhancerEnabled(v.getAgentEnhancerEnabled());
        resp.setAgentEnhancerConfigJson(v.getAgentEnhancerConfigJson());
        resp.setAgentEnhancerCreatedAt(v.getAgentEnhancerCreatedAt());
        resp.setAgentEnhancerUpdatedAt(v.getAgentEnhancerUpdatedAt());
        return resp;
    }

}
