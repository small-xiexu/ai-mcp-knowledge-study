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
import com.xbk.knowledge.types.common.PageResultConverter;
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

    private final AgentEnhancerAppService agentEnhancerAppService;
    private final AgentEnhancerBindingAppService agentEnhancerBindingAppService;

    /**
     * 根据筛选条件查询 Agent 增强器（AgentEnhancer）列表。
     *
     * @param request Agent 增强器（AgentEnhancer）查询条件。
     * @return Agent 增强器（AgentEnhancer）分页查询结果。
     */
    @PostMapping("/list")
    @SaCheckPermission("agent-enhancer:read")
    @Override
    public Result<PageResult<AgentEnhancerResponse>> list(@Valid @RequestBody AgentEnhancerQueryRequest request) {
        Integer enabled = request.getEnabled() == null ? null : (request.getEnabled() ? 1 : 0);
        AgentEnhancerPageQuery query = new AgentEnhancerPageQuery(request.getKeyword(),
                enabled,
                request.getAgentEnhancerType(),
                request.getOffset(),
                request.getPageSize()
        );
        PageResult<AgentEnhancer> page = agentEnhancerAppService.queryPage(query);
        PageResult<AgentEnhancerResponse> resp = PageResultConverter.convert(page, this::toResponse);
        return Result.success(resp);
    }

    /**
     * 查询 Agent 增强器（AgentEnhancer）。
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
     * 启用业务配置。
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
     * 禁用业务配置。
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
     * 根据筛选条件查询 Agent 增强器（AgentEnhancer）绑定列表。
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
     * 创建或更新 Agent 增强器（AgentEnhancer）绑定数据。
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
