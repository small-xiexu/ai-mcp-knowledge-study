package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.api.dto.gateway.GatewayAuthListRequest;
import com.xbk.knowledge.api.dto.gateway.GatewayInstanceRequest;
import com.xbk.knowledge.api.dto.gateway.GatewayMetricsQueryRequest;
import com.xbk.knowledge.api.dto.gateway.ModelBindingQueryRequest;
import com.xbk.knowledge.api.dto.gateway.SaveGatewayAuthRequest;
import com.xbk.knowledge.api.dto.gateway.SaveModelBindingRequest;
import com.xbk.knowledge.api.dto.gateway.SaveToolRequest;
import com.xbk.knowledge.api.dto.gateway.ToolDebugRequest;
import com.xbk.knowledge.api.dto.gateway.ToolListRequest;
import com.xbk.knowledge.types.common.PageRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;

import java.util.List;
import java.util.Map;

/**
 * 网关管理服务接口
 * 定义 MCP 网关治理能力的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IGatewayManageService {

    /**
     * 分页查询网关实例列表。
     *
     * @param request 网关管理分页查询参数。
     * @return 返回网关实例分页视图数据。
     */
    Result<PageResult<Map<String, Object>>> listGatewayInstances(PageRequest request);

    /**
     * 保存网关实例。
     *
     * @param request 网关管理保存参数。
     * @return 返回保存后的网关实例视图数据。
     */
    Result<Map<String, Object>> saveGatewayInstance(GatewayInstanceRequest request);

    /**
     * 删除网关实例。
     *
     * @param request 网关管理删除参数。
     * @return 返回网关实例删除状态。
     */
    Result<Void> deleteGatewayInstance(IdRequest request);

    /**
     * 分页查询网关凭证列表。
     *
     * @param request 网关管理分页查询参数。
     * @return 返回网关凭证分页视图数据。
     */
    Result<PageResult<Map<String, Object>>> listGatewayAuth(GatewayAuthListRequest request);

    /**
     * 保存网关凭证。
     *
     * @param request 网关管理保存参数。
     * @return 返回保存后的网关凭证视图数据。
     */
    Result<Map<String, Object>> saveGatewayAuth(SaveGatewayAuthRequest request);

    /**
     * 启用网关凭证。
     *
     * @param request 网关管理启停参数。
     * @return 返回网关凭证启用状态。
     */
    Result<Void> enableGatewayAuth(IdRequest request);

    /**
     * 禁用网关凭证。
     *
     * @param request 网关管理启停参数。
     * @return 返回网关凭证禁用状态。
     */
    Result<Void> disableGatewayAuth(IdRequest request);

    /**
     * 分页查询工具列表。
     *
     * @param request 网关管理分页查询参数。
     * @return 返回工具分页视图数据。
     */
    Result<PageResult<Map<String, Object>>> listTools(ToolListRequest request);

    /**
     * 查询工具详情。
     *
     * @param request 网关管理查询参数。
     * @return 返回工具详情视图数据。
     */
    Result<Map<String, Object>> getTool(IdRequest request);

    /**
     * 保存工具配置。
     *
     * @param request 网关管理保存参数。
     * @return 返回保存后的工具视图数据。
     */
    Result<Map<String, Object>> saveTool(SaveToolRequest request);

    /**
     * 删除工具。
     *
     * @param request 网关管理删除参数。
     * @return 返回工具删除状态。
     */
    Result<Void> deleteTool(IdRequest request);

    /**
     * 启用工具。
     *
     * @param request 网关管理启停参数。
     * @return 返回工具启用状态。
     */
    Result<Void> enableTool(IdRequest request);

    /**
     * 禁用工具。
     *
     * @param request 网关管理启停参数。
     * @return 返回工具禁用状态。
     */
    Result<Void> disableTool(IdRequest request);

    /**
     * 调试工具调用。
     *
     * @param request 网关管理调用参数。
     * @return 返回工具调试执行结果数据。
     */
    Result<Map<String, Object>> debugTool(ToolDebugRequest request);

    /**
     * 查询模型绑定关系。
     *
     * @param request 网关管理查询参数。
     * @return 返回模型工具绑定视图数据。
     */
    Result<Map<String, Object>> getModelBindings(ModelBindingQueryRequest request);

    /**
     * 保存模型绑定关系。
     *
     * @param request 网关管理保存参数。
     * @return 返回模型工具绑定保存状态。
     */
    Result<Void> saveModelBindings(SaveModelBindingRequest request);

    /**
     * 查询全部已启用工具。
     *
     * @return 返回已启用工具视图列表数据。
     */
    Result<List<Map<String, Object>>> allEnabledTools();

    /**
     * 查询已启用模型列表。
     *
     * @return 返回已启用模型视图列表数据。
     */
    Result<List<Map<String, Object>>> enabledModels();

    /**
     * 查询网关监控指标。
     *
     * @param request 网关管理查询参数。
     * @return 返回网关监控指标视图数据。
     */
    Result<Map<String, Object>> queryGatewayMetrics(GatewayMetricsQueryRequest request);
}
