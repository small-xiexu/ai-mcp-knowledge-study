package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.gateway.model.entity.McpGateway;
import com.xbk.knowledge.domain.gateway.model.entity.McpGatewayAuth;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolMapping;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolRegistry;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolBinding;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.types.common.PageResult;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Gateway 管理应用服务。
 *
 * 职责：承载网关与工具的级联清理用例，避免控制器直连多仓储。
 *
 * @author sxie
 */
public interface GatewayManageAppService {

    /**
     * 确保网关存在，不存在时抛出异常。
     *
     * @param gatewayId 网关 ID
     * @return 已存在的网关实例
     */
    McpGateway ensureGatewayExists(String gatewayId);

    /**
     * 查询网关实例分页数据。
     *
     * @param pageNum 页码（从 1 开始）
     * @param pageSize 每页大小
     * @return 网关分页结果
     */
    PageResult<McpGateway> queryGatewayInstancePage(Integer pageNum, Integer pageSize);

    /**
     * 创建或更新网关实例。
     *
     * @param id 主键（为空表示创建）
     * @param gatewayId 网关 ID
     * @param gatewayName 网关名称
     * @param gatewayDesc 网关描述
     * @param gatewayVersion 网关版本
     * @param gatewayInstructions 网关说明
     * @param status 状态
     * @return 保存后的网关实例
     */
    McpGateway saveGatewayInstance(Long id,
                                   String gatewayId,
                                   String gatewayName,
                                   String gatewayDesc,
                                   String gatewayVersion,
                                   String gatewayInstructions,
                                   Integer status);

    /**
     * 统计网关下工具数量。
     *
     * @param gatewayId 网关 ID
     * @return 工具数量
     */
    long countToolsByGatewayId(String gatewayId);

    /**
     * 查询网关凭证分页数据。
     *
     * @param gatewayId 网关 ID
     * @param status 状态过滤条件
     * @param apiKeyKeyword API Key 关键字
     * @param pageNum 页码（从 1 开始）
     * @param pageSize 每页大小
     * @return 网关凭证分页结果
     */
    PageResult<McpGatewayAuth> queryGatewayAuthPage(String gatewayId,
                                                    Integer status,
                                                    String apiKeyKeyword,
                                                    Integer pageNum,
                                                    Integer pageSize);

    /**
     * 查询网关工具分页数据。
     *
     * @param gatewayId 网关 ID
     * @param toolNameKeyword 工具名称关键字
     * @param toolDescriptionKeyword 工具描述关键字
     * @param status 工具状态
     * @param pageNum 页码（从 1 开始）
     * @param pageSize 每页大小
     * @return 工具分页结果
     */
    PageResult<McpToolRegistry> queryToolPage(String gatewayId,
                                              String toolNameKeyword,
                                              String toolDescriptionKeyword,
                                              Integer status,
                                              Integer pageNum,
                                              Integer pageSize);

    /**
     * 查询单个工具详情及映射配置。
     *
     * @param toolId 工具主键
     * @return 工具详情
     */
    ToolDetail queryToolDetail(Long toolId);

    /**
     * 创建或更新工具配置。
     *
     * @param command 保存命令
     * @return 保存后的工具实体
     */
    McpToolRegistry saveTool(ToolSaveCommand command);

    /**
     * 更新工具状态。
     *
     * @param id 工具主键
     * @param status 目标状态
     */
    void updateToolStatus(Long id, int status);

    /**
     * 查询模型绑定的工具列表。
     *
     * @param modelId 模型 ID
     * @return 绑定列表
     */
    List<McpToolBinding> queryModelBindings(Long modelId);

    /**
     * 保存模型绑定工具列表（覆盖）。
     *
     * @param modelId 模型 ID
     * @param toolIds 工具 ID 列表
     */
    void saveModelBindings(Long modelId, List<Long> toolIds);

    /**
     * 查询所有已启用工具。
     *
     * @return 工具列表
     */
    List<McpToolRegistry> listAllEnabledTools();

    /**
     * 查询所有已启用模型。
     *
     * @return 模型列表
     */
    List<ModelConfig> listEnabledModels();

    /**
     * 查询工具刷新目标列表。
     *
     * @param gatewayId 网关 ID
     * @param toolId 可选工具 ID
     * @return 待刷新工具列表
     */
    List<McpToolRegistry> queryToolsForRefresh(String gatewayId, Long toolId);

    /**
     * 创建或更新网关凭证。
     *
     * @param id 凭证主键（为空表示创建）
     * @param gatewayId 网关 ID（更新时可为空，表示沿用原网关）
     * @param apiKey API Key（为空时更新沿用原值，创建自动生成）
     * @param rateLimit 限流阈值
     * @param expireTime 过期时间
     * @param status 状态
     * @return 保存后的网关凭证
     */
    McpGatewayAuth saveGatewayAuth(Long id,
                                   String gatewayId,
                                   String apiKey,
                                   Integer rateLimit,
                                   LocalDateTime expireTime,
                                   Integer status);

    /**
     * 更新网关凭证状态。
     *
     * @param id 凭证主键
     * @param status 目标状态
     */
    void updateGatewayAuthStatus(Long id, int status);

    /**
     * 删除网关实例（应用层级联清理）。
     * 
     * @param query 网关主键
     */
    void deleteGatewayInstance(IdQuery query);

    /**
     * 删除网关工具（应用层级联清理）。
     * 
     * @param query 工具主键
     */
    void deleteTool(IdQuery query);

    /**
     * 工具详情。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ToolDetail {

        /**
         * 工具主记录。
         */
        private McpToolRegistry tool;

        /**
         * 请求映射列表。
         */
        private List<McpToolMapping> requestMappings;

        /**
         * 响应映射列表。
         */
        private List<McpToolMapping> responseMappings;
    }

    /**
     * 工具保存命令。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ToolSaveCommand {

        /**
         * 工具主键（为空表示创建）。
         */
        private Long id;

        /**
         * 网关 ID。
         */
        private String gatewayId;

        /**
         * 工具名称。
         */
        private String toolName;

        /**
         * 工具描述。
         */
        private String toolDescription;

        /**
         * HTTP 方法。
         */
        private String httpMethod;

        /**
         * HTTP 地址。
         */
        private String httpUrl;

        /**
         * HTTP 请求头。
         */
        private String httpHeaders;

        /**
         * 超时时间。
         */
        private Integer timeout;

        /**
         * 重试次数。
         */
        private Integer retryTimes;

        /**
         * 状态。
         */
        private Integer status;

        /**
         * 请求映射定义。
         */
        private List<ToolMappingNode> requestMappings;

        /**
         * 响应映射定义。
         */
        private List<ToolMappingNode> responseMappings;
    }

    /**
     * 工具映射节点。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ToolMappingNode {

        /**
         * 父节点 ID。
         */
        private Long parentId;

        /**
         * 字段名。
         */
        private String fieldName;

        /**
         * MCP 类型。
         */
        private String mcpType;

        /**
         * MCP 描述。
         */
        private String mcpDesc;

        /**
         * 是否必填。
         */
        private Boolean isRequired;

        /**
         * 子项类型。
         */
        private String itemType;

        /**
         * 子项引用 ID。
         */
        private Long itemRefId;

        /**
         * HTTP 路径。
         */
        private String httpPath;

        /**
         * HTTP 位置。
         */
        private String httpLocation;

        /**
         * 排序序号。
         */
        private Integer sortOrder;

        /**
         * 子节点。
         */
        private List<ToolMappingNode> children;
    }
}
