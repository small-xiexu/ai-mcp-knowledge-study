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
import com.xbk.knowledge.api.dto.gateway.RefreshToolsRequest;
import com.xbk.knowledge.types.common.PageRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;

import java.util.List;
import java.util.Map;

/**
 * 网关管理服务接口。
 *
 * 职责：定义 MCP 网关治理能力的 API 契约。
 *
 * @author sxie
 */
public interface IGatewayManageService {

    /**
     * 分页查询网关实例列表。
     *
     * @param request 网关实例分页查询参数
     * @return 网关实例分页数据
     */
    Result<PageResult<Map<String, Object>>> listGatewayInstances(PageRequest request);

    /**
     * 保存网关实例。
     *
     * @param request 网关实例保存参数
     * @return 保存后的网关实例信息
     */
    Result<Map<String, Object>> saveGatewayInstance(GatewayInstanceRequest request);

    /**
     * 删除网关实例。
     *
     * @param request 网关实例删除参数
     * @return 删除结果
     */
    Result<Void> deleteGatewayInstance(IdRequest request);

    /**
     * 分页查询网关凭证列表。
     *
     * @param request 网关凭证分页查询参数
     * @return 网关凭证分页数据
     */
    Result<PageResult<Map<String, Object>>> listGatewayAuth(GatewayAuthListRequest request);

    /**
     * 保存网关凭证。
     *
     * @param request 网关凭证保存参数
     * @return 保存后的网关凭证信息
     */
    Result<Map<String, Object>> saveGatewayAuth(SaveGatewayAuthRequest request);

    /**
     * 启用网关凭证。
     *
     * @param request 网关凭证启用参数
     * @return 启用结果
     */
    Result<Void> enableGatewayAuth(IdRequest request);

    /**
     * 禁用网关凭证。
     *
     * @param request 网关凭证禁用参数
     * @return 禁用结果
     */
    Result<Void> disableGatewayAuth(IdRequest request);

    /**
     * 分页查询工具列表。
     *
     * @param request 工具分页查询参数
     * @return 工具分页数据
     */
    Result<PageResult<Map<String, Object>>> listTools(ToolListRequest request);

    /**
     * 查询工具详情。
     *
     * @param request 工具查询参数
     * @return 工具详情
     */
    Result<Map<String, Object>> getTool(IdRequest request);

    /**
     * 保存工具配置。
     *
     * @param request 工具配置保存参数
     * @return 保存后的工具配置
     */
    Result<Map<String, Object>> saveTool(SaveToolRequest request);

    /**
     * 删除工具。
     *
     * @param request 工具删除参数
     * @return 删除结果
     */
    Result<Void> deleteTool(IdRequest request);

    /**
     * 启用工具。
     *
     * @param request 工具启用参数
     * @return 启用结果
     */
    Result<Void> enableTool(IdRequest request);

    /**
     * 禁用工具。
     *
     * @param request 工具禁用参数
     * @return 禁用结果
     */
    Result<Void> disableTool(IdRequest request);

    /**
     * 调试工具调用。
     *
     * @param request 工具调试参数
     * @return 工具调试结果
     */
    Result<Map<String, Object>> debugTool(ToolDebugRequest request);

    /**
     * 查询模型绑定关系。
     *
     * @param request 模型绑定查询参数
     * @return 模型与工具绑定关系
     */
    Result<Map<String, Object>> getModelBindings(ModelBindingQueryRequest request);

    /**
     * 保存模型绑定关系。
     *
     * @param request 模型绑定保存参数
     * @return 保存结果
     */
    Result<Void> saveModelBindings(SaveModelBindingRequest request);

    /**
     * 查询全部已启用工具。
     *
     * @return 全部已启用工具列表
     */
    Result<List<Map<String, Object>>> allEnabledTools();

    /**
     * 查询已启用模型列表。
     *
     * @return 已启用模型列表
     */
    Result<List<Map<String, Object>>> enabledModels();

    /**
     * 查询网关监控指标。
     *
     * @param request 网关监控指标查询参数
     * @return 网关监控指标
     */
    Result<Map<String, Object>> queryGatewayMetrics(GatewayMetricsQueryRequest request);

    /**
     * 刷新工具连通性状态。
     *
     * @param request 工具刷新参数（支持指定 gatewayId 或 toolId）
     * @return 刷新结果（包含成功/失败统计）
     */
    Result<Map<String, Object>> refreshTools(RefreshToolsRequest request);
}
