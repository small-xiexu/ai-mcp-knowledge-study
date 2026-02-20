package com.xbk.knowledge.domain.gateway.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MCP 工具参数映射实体
 * 对应数据库表：mcp_tool_mapping
 *
 * 职责：领域实体，承载 MCP 工具参数与 HTTP 请求参数之间的映射关系
 * @author sxie
 */
@TableName("mcp_tool_mapping")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolMapping {

    /**
     * 主键ID
     *
     * 为什么：用于持久化唯一标识
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * scopeId。
     */

    /**
     * 网关唯一标识
     *
     * 为什么：关联所属网关，支持多网关映射隔离
     */
    private String gatewayId;

    /**
     * 工具ID
     *
     * 为什么：关联所属工具注册记录
     */
    private Long toolId;

    /**
     * 映射类型（INPUT/OUTPUT）
     *
     * 为什么：区分入参映射与出参映射，支持双向转换
     */
    private String mappingType;

    /**
     * 父节点ID
     *
     * 为什么：支持嵌套对象结构的树形参数映射
     */
    private Long parentId;

    /**
     * 字段名称
     *
     * 为什么：MCP 协议中参数的字段标识
     */
    private String fieldName;

    /**
     * MCP 类型（string/number/boolean/object/array）
     *
     * 为什么：定义 MCP 协议中参数的 JSON Schema 类型
     */
    private String mcpType;

    /**
     * MCP 字段描述
     *
     * 为什么：供 LLM 理解参数含义，影响参数填充质量
     */
    private String mcpDesc;

    /**
     * 是否必填
     *
     * 为什么：约束 LLM 必须提供的参数，避免缺失关键入参
     */
    private Boolean isRequired;

    /**
     * 数组元素类型
     *
     * 为什么：当 mcpType 为 array 时，定义元素的基础类型
     */
    private String itemType;

    /**
     * 数组元素引用ID
     *
     * 为什么：当数组元素为 object 时，引用子映射定义
     */
    private Long itemRefId;

    /**
     * HTTP 路径表达式
     *
     * 为什么：定义参数在 HTTP 请求中的取值/赋值路径
     */
    private String httpPath;

    /**
     * HTTP 参数位置（query/body/header/path）
     *
     * 为什么：决定参数在 HTTP 请求中的承载位置
     */
    private String httpLocation;

    /**
     * 排序序号
     *
     * 为什么：控制参数在 Schema 中的展示顺序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     *
     * 为什么：用于审计与排序
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     *
     * 为什么：用于审计与变更追踪
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
