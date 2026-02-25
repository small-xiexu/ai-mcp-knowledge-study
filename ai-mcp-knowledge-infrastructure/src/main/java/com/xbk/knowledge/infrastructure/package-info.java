/**
 * 基础设施层（Infrastructure Layer）。
 *
 * 职责：提供领域端口的技术实现与外部系统适配，包括数据库、网关、缓存、MCP 与模型供应商。
 *
 * 结构约定
 * - dao持久化访问接口（MyBatis）
 * - agentenhancer/agent/approval/gateway/identity/mcp/tool/workflow按业务域组织的仓储实现（Adapter）
 * - gateway/provider/protocol外部服务与模型协议适配
 * - redis/chatmemory/auth运行时基础能力适配
 *
 * @author sxie
 */
package com.xbk.knowledge.infrastructure;
