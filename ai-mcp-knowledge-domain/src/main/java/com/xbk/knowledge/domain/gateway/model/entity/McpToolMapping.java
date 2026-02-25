package com.xbk.knowledge.domain.gateway.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MCP 工具参数映射实体
 * 对应数据库表mcp_tool_mapping
 *
 * 职责：领域实体，承载 MCP 工具参数与 HTTP 请求参数之间的映射关系
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolMapping {

    /**
     * 主键ID
     *
     * 用于持久化唯一标识
     */
    private Long id;

    /**
     * 网关唯一标识
     *
     * 关联所属网关，支持多网关映射隔离
     */
    private String gatewayId;

    /**
     * 工具ID
     *
     * 关联所属工具注册记录
     */
    private Long toolId;

    /**
     * 映射类型（INPUT/OUTPUT）
     *
     * 区分入参映射与出参映射，支持双向转换
     */
    private String mappingType;

    /**
     * 父节点ID
     *
     * 支持嵌套对象结构的树形参数映射
     */
    private Long parentId;

    /**
     * 字段名称
     *
     * MCP 协议中参数的字段标识
     */
    private String fieldName;

    /**
     * MCP 类型（string/number/boolean/object/array）
     *
     * 定义 MCP 协议中参数的 JSON Schema 类型
     */
    private String mcpType;

    /**
     * MCP 字段描述
     *
     * 供 LLM 理解参数含义，影响参数填充质量
     */
    private String mcpDesc;

    /**
     * 是否必填
     *
     * 约束 LLM 必须提供的参数，避免缺失关键入参
     */
    private Boolean isRequired;

    /**
     * 数组元素类型
     *
     * 当 mcpType 为 array 时，定义元素的基础类型
     */
    private String itemType;

    /**
     * 数组元素引用ID
     *
     * 当数组元素为 object 时，引用子映射定义
     */
    private Long itemRefId;

    /**
     * HTTP 路径表达式
     *
     * 定义参数在 HTTP 请求中的取值/赋值路径
     */
    private String httpPath;

    /**
     * HTTP 参数位置（query/body/header/path）
     *
     * 决定参数在 HTTP 请求中的承载位置
     */
    private String httpLocation;

    /**
     * 排序序号
     *
     * 控制参数在 Schema 中的展示顺序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     *
     * 用于审计与排序
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     *
     * 用于审计与变更追踪
     */
    private LocalDateTime updatedAt;
}
