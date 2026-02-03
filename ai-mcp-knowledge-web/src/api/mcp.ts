import request from '@/utils/request'
import type { McpServerConfig, McpServerConfigRequest } from '@/types/entity'
import type { PageResult, PageRequest } from '@/types/api'

/**
 * 查询 MCP Server 配置列表（分页）
 */
export const getMcpServerList = (data: PageRequest) =>
  request.post<PageResult<McpServerConfig>>('/mcp/servers/list', data)

/**
 * 查询单个 MCP Server 配置
 */
export const getMcpServerById = (id: number) =>
  request.post<McpServerConfig>('/mcp/servers/get', { id })

/**
 * 创建 MCP Server 配置
 */
export const createMcpServer = (data: McpServerConfigRequest) =>
  request.post<McpServerConfig>('/mcp/servers/create', data)

/**
 * 更新 MCP Server 配置
 */
export const updateMcpServer = (data: McpServerConfigRequest) =>
  request.post<McpServerConfig>('/mcp/servers/update', data)

/**
 * 删除 MCP Server 配置
 */
export const deleteMcpServer = (id: number) =>
  request.post<void>('/mcp/servers/delete', { id })

/**
 * 启用 MCP Server 配置
 */
export const enableMcpServer = (id: number) =>
  request.post<McpServerConfig>('/mcp/servers/enable', { id })

/**
 * 禁用 MCP Server 配置
 */
export const disableMcpServer = (id: number) =>
  request.post<McpServerConfig>('/mcp/servers/disable', { id })

/**
 * 刷新 MCP Server 运行时连接
 */
export const refreshMcpServers = () =>
  request.post<void>('/mcp/servers/refresh')

/**
 * 刷新指定 MCP Server 运行时连接
 */
export const refreshMcpServer = (id: number) =>
  request.post<void>('/mcp/servers/refresh-one', { id })
