# API 接口索引（方法级）

> 数据来源：基于当前代码自动扫描 `ai-mcp-knowledge-trigger/src/main/java/com/xbk/knowledge/trigger/http/*.java`。  
> 说明：本索引按“方法级”组织，便于联调、权限核查与回归测试。  
> 返回约定：绝大多数业务接口返回 `Result<T>` 包装；流式与 MCP 协议接口在文末单独列出。

## 1. 通用约定
- 鉴权方式：Sa-Token，token 默认请求头键为 `satoken`（可配置）。
- 权限注解：`@SaCheckPermission("...")`。
- 登录注解：`@SaCheckLogin`。
- 分页参数：`PageRequest`（`pageNum/pageSize`），由 `PageRequestNormalizeAdvice` 统一归一化。

## 2. 权限编码参考
- 用户与角色：`user:read` `user:write` `role:read` `role:write`
- Agent：`agent:read` `agent:write` `agent:publish` `agent:invoke`
- Workflow：`workflow:read` `workflow:write` `workflow:publish` `workflow:invoke`
- AgentEnhancer：`agent-enhancer:read` `agent-enhancer:write`
- 工具：`tool:read` `tool:write` `tool:invoke` `tool:approve`
- 审计：`audit:read`

## 3. 特殊接口说明
- SSE 流接口返回 `text/event-stream`，不走标准 `Result<T>` 包装。
- MCP Gateway 协议接口包含 JSON-RPC 请求/响应透传。
- XXL 管理接口在代码中未统一使用 `@SaCheckPermission`，实际访问控制可能由额外网关/守卫实现（例如 `XxlPermissionGuard`）。

## 4. 接口清单（按 Controller）

### AICallController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `stream` | `POST` | `/api/ai/stream` | `agent:read` |
| `getAvailableModels` | `POST` | `/api/ai/models` | `agent:read` |

### AgentEnhancerController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `disable` | `POST` | `/api/agent-enhancers/disable` | `agent-enhancer:write` |
| `enable` | `POST` | `/api/agent-enhancers/enable` | `agent-enhancer:write` |
| `get` | `POST` | `/api/agent-enhancers/get` | `agent-enhancer:read` |
| `list` | `POST` | `/api/agent-enhancers/list` | `agent-enhancer:read` |
| `listBindings` | `POST` | `/api/agent-enhancers/bindings/list` | `agent-enhancer:read` |
| `remove` | `POST` | `/api/agent-enhancers/remove` | `agent-enhancer:write` |
| `save` | `POST` | `/api/agent-enhancers/save` | `agent-enhancer:write` |
| `saveBindings` | `POST` | `/api/agent-enhancers/bindings/save` | `agent-enhancer:write` |

### AgentController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `create` | `POST` | `/api/agents/create` | `agent:write` |
| `get` | `POST` | `/api/agents/get` | `agent:read` |
| `list` | `POST` | `/api/agents/list` | `agent:read` |
| `remove` | `POST` | `/api/agents/remove` | `agent:write` |
| `update` | `POST` | `/api/agents/update` | `agent:write` |

### AgentRuntimeController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `chat` | `POST` | `/api/agents/{agentCode}/chat` | `agent:invoke` |
| `invoke` | `POST` | `/api/agents/{agentCode}/invoke` | `agent:invoke` |

### AgentScheduleController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `create` | `POST` | `/api/schedules/create` | `agent:write` |
| `disable` | `POST` | `/api/schedules/disable` | `agent:write` |
| `enable` | `POST` | `/api/schedules/enable` | `agent:write` |
| `get` | `POST` | `/api/schedules/get` | `agent:read` |
| `list` | `POST` | `/api/schedules/list` | `agent:read` |
| `remove` | `POST` | `/api/schedules/remove` | `agent:write` |
| `update` | `POST` | `/api/schedules/update` | `agent:write` |

### AgentVersionController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `get` | `POST` | `/api/agent-versions/get` | `agent:read` |
| `list` | `POST` | `/api/agent-versions/list` | `agent:read` |
| `publish` | `POST` | `/api/agent-versions/publish` | `agent:publish` |
| `rollback` | `POST` | `/api/agent-versions/rollback` | `agent:publish` |
| `saveDraft` | `POST` | `/api/agent-versions/draft/save` | `agent:write` |

### ApprovalController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `approve` | `POST` | `/api/approvals/approve` | `tool:approve` |
| `get` | `POST` | `/api/approvals/get` | `tool:approve` |
| `list` | `POST` | `/api/approvals/list` | `tool:approve` |
| `reject` | `POST` | `/api/approvals/reject` | `tool:approve` |

### AuditEventController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `list` | `POST` | `/api/audit/events/list` | `audit:read` |

### AuthController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `currentUser` | `GET` | `/api/auth/me` | `LOGIN` |
| `login` | `POST` | `/api/auth/login` | `-` |
| `logout` | `POST` | `/api/auth/logout` | `LOGIN` |

### ChatSessionController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `appendMessage` | `POST` | `/api/ai/sessions/{id}/messages` | `agent:write` |
| `createSession` | `POST` | `/api/ai/sessions` | `agent:write` |
| `deleteMessages` | `POST` | `/api/ai/sessions/messages/delete` | `agent:write` |
| `deleteSession` | `POST` | `/api/ai/sessions/delete` | `agent:write` |
| `getSession` | `POST` | `/api/ai/sessions/detail` | `agent:read` |
| `listMessages` | `POST` | `/api/ai/sessions/messages/list` | `agent:read` |
| `listSessions` | `POST` | `/api/ai/sessions/list` | `agent:read` |
| `updateSession` | `POST` | `/api/ai/sessions/update` | `agent:write` |

### ClientProfileController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `disable` | `POST` | `/api/client-profiles/disable` | `agent:write` |
| `enable` | `POST` | `/api/client-profiles/enable` | `agent:write` |
| `get` | `POST` | `/api/client-profiles/get` | `agent:read` |
| `list` | `POST` | `/api/client-profiles/list` | `agent:read` |
| `remove` | `POST` | `/api/client-profiles/remove` | `agent:write` |
| `save` | `POST` | `/api/client-profiles/save` | `agent:write` |

### GatewayManageController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `allEnabledTools` | `POST` | `/api/gateway/manage/tools/all-enabled` | `tool:read` |
| `debugTool` | `POST` | `/api/gateway/manage/tools/debug` | `tool:invoke` |
| `deleteGatewayInstance` | `POST` | `/api/gateway/manage/instances/delete` | `tool:write` |
| `deleteTool` | `POST` | `/api/gateway/manage/tools/delete` | `tool:write` |
| `disableGatewayAuth` | `POST` | `/api/gateway/manage/auth/disable` | `tool:write` |
| `disableTool` | `POST` | `/api/gateway/manage/tools/disable` | `tool:write` |
| `enableGatewayAuth` | `POST` | `/api/gateway/manage/auth/enable` | `tool:write` |
| `enableTool` | `POST` | `/api/gateway/manage/tools/enable` | `tool:write` |
| `enabledModels` | `POST` | `/api/gateway/manage/models/enabled` | `tool:read` |
| `getModelBindings` | `POST` | `/api/gateway/manage/bindings/model/get` | `tool:read` |
| `getTool` | `POST` | `/api/gateway/manage/tools/get` | `tool:read` |
| `listGatewayAuth` | `POST` | `/api/gateway/manage/auth/list` | `tool:read` |
| `listGatewayInstances` | `POST` | `/api/gateway/manage/instances/list` | `tool:read` |
| `listTools` | `POST` | `/api/gateway/manage/tools/list` | `tool:read` |
| `queryGatewayMetrics` | `POST` | `/api/gateway/manage/metrics/overview` | `tool:read` |
| `saveGatewayAuth` | `POST` | `/api/gateway/manage/auth/save` | `tool:write` |
| `saveGatewayInstance` | `POST` | `/api/gateway/manage/instances/save` | `tool:write` |
| `saveModelBindings` | `POST` | `/api/gateway/manage/bindings/model/save` | `tool:write` |
| `saveTool` | `POST` | `/api/gateway/manage/tools/save` | `tool:write` |

### McpGatewayController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `establishSseConnection` | `GET` | `/api/gateway/{gatewayId}/mcp/sse` | `-`（API Key 鉴权，SSE 长连接） |
| `handleMessage` | `POST` | `/api/gateway/{gatewayId}/mcp/message?sessionId=...` | `-`（API Key 鉴权，JSON-RPC 消息处理） |

### McpServerConfigController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `createConfig` | `POST` | `/api/mcp/servers/create` | `tool:write` |
| `deleteConfig` | `POST` | `/api/mcp/servers/delete` | `tool:write` |
| `disableConfig` | `POST` | `/api/mcp/servers/disable` | `tool:write` |
| `enableConfig` | `POST` | `/api/mcp/servers/enable` | `tool:write` |
| `getConfig` | `POST` | `/api/mcp/servers/get` | `tool:read` |
| `listConfigs` | `POST` | `/api/mcp/servers/list` | `tool:read` |
| `refreshConfig` | `POST` | `/api/mcp/servers/refresh-one` | `tool:write` |
| `refreshConfigs` | `POST` | `/api/mcp/servers/refresh` | `tool:write` |
| `updateConfig` | `POST` | `/api/mcp/servers/update` | `tool:write` |

### McpToolController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `listTools` | `POST` | `/api/mcp/tools/list` | `tool:read` |

### MetricsController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `getCallMetrics` | `POST` | `/api/metrics/calls` | `audit:read` |
| `getModelUsage` | `POST` | `/api/metrics/model-usage` | `audit:read` |
| `getResponseTime` | `POST` | `/api/metrics/response-time` | `audit:read` |
| `getSuccessRate` | `POST` | `/api/metrics/success-rate` | `audit:read` |

### ModelConfigController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `activateChatModel` | `POST` | `/api/models/activate-chat` | `agent:write` |
| `activateEmbeddingModel` | `POST` | `/api/models/activate-embedding` | `agent:write` |
| `createModel` | `POST` | `/api/models/create` | `agent:write` |
| `deleteModel` | `POST` | `/api/models/delete` | `agent:write` |
| `disableModel` | `POST` | `/api/models/disable` | `agent:write` |
| `enableModel` | `POST` | `/api/models/enable` | `agent:write` |
| `getActiveChatModel` | `POST` | `/api/models/active-chat` | `agent:read` |
| `getActiveEmbeddingModel` | `POST` | `/api/models/active-embedding` | `agent:read` |
| `getModel` | `POST` | `/api/models/get` | `agent:read` |
| `listModels` | `POST` | `/api/models/list` | `agent:read` |
| `testModel` | `POST` | `/api/models/test` | `agent:write` |
| `updateModel` | `POST` | `/api/models/update` | `agent:write` |

### PermissionController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `list` | `POST` | `/api/permissions/list` | `role:read` |

### PreheatController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `preheatAgentVersion` | `POST` | `/api/preheat/agent-version` | `agent:write` |
| `preheatWorkflowVersion` | `POST` | `/api/preheat/workflow-version` | `workflow:write` |

### PromptTemplateController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `archive` | `POST` | `/api/templates/archive` | `agent:publish` |
| `create` | `POST` | `/api/templates/create` | `agent:write` |
| `get` | `POST` | `/api/templates/get` | `agent:read` |
| `list` | `POST` | `/api/templates/list` | `agent:read` |
| `publish` | `POST` | `/api/templates/publish` | `agent:publish` |
| `update` | `POST` | `/api/templates/update` | `agent:write` |

### RagController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `analyzeGitRepository` | `POST` | `/api/ai/rag/analyze` | `agent:write` |
| `cancelTask` | `POST` | `/api/ai/rag/task/cancel` | `agent:write` |
| `countTag` | `POST` | `/api/ai/rag/count` | `agent:read` |
| `deleteTag` | `POST` | `/api/ai/rag/delete` | `agent:write` |
| `listTags` | `POST` | `/api/ai/rag/tags` | `agent:read` |
| `listTasks` | `POST` | `/api/ai/rag/task/list` | `agent:read` |
| `queryTask` | `POST` | `/api/ai/rag/task/progress` | `agent:read` |
| `retryTask` | `POST` | `/api/ai/rag/task/retry` | `agent:write` |
| `uploadFile` | `POST` | `/api/ai/rag/upload` | `agent:write` |
| `uploadFileAsync` | `POST` | `/api/ai/rag/upload/async` | `agent:write` |

### RoleController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `create` | `POST` | `/api/roles/create` | `role:write` |
| `grantPermissions` | `POST` | `/api/roles/grant-permissions` | `role:write` |
| `list` | `POST` | `/api/roles/list` | `role:read` |
| `permissionIds` | `POST` | `/api/roles/permission-ids` | `role:read` |
| `update` | `POST` | `/api/roles/update` | `role:write` |

### UserIdentityController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `create` | `POST` | `/api/users/create` | `user:write` |
| `grantRoles` | `POST` | `/api/users/grant-roles` | `user:write` |
| `list` | `POST` | `/api/users/list` | `user:read` |
| `resetPassword` | `POST` | `/api/users/reset-password` | `user:write` |
| `roleIds` | `POST` | `/api/users/role-ids` | `user:read` |
| `update` | `POST` | `/api/users/update` | `user:write` |

### WorkbenchController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `summary` | `POST` | `/api/workbench/summary` | `LOGIN` |

### WorkflowController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `create` | `POST` | `/api/workflows/create` | `workflow:write` |
| `createVersion` | `POST` | `/api/workflows/versions/create` | `workflow:write` |
| `get` | `POST` | `/api/workflows/get` | `workflow:read` |
| `getVersion` | `POST` | `/api/workflows/versions/get` | `workflow:read` |
| `list` | `POST` | `/api/workflows/list` | `workflow:read` |
| `listVersions` | `POST` | `/api/workflows/versions/list` | `workflow:read` |
| `publish` | `POST` | `/api/workflows/versions/publish` | `workflow:publish` |
| `saveGraph` | `POST` | `/api/workflows/versions/save-graph` | `workflow:write` |
| `update` | `POST` | `/api/workflows/update` | `workflow:write` |

### WorkflowRuntimeController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `getRun` | `POST` | `/api/workflows/runs/get` | `workflow:read` |
| `listNodeRuns` | `POST` | `/api/workflows/runs/nodes` | `workflow:read` |
| `listRuns` | `POST` | `/api/workflows/runs/list` | `workflow:read` |
| `run` | `POST` | `/api/workflows/{workflowCode}/run` | `workflow:invoke` |

### XxlAdminController.java
| 方法名 | HTTP | 路径 | 权限 |
|---|---|---|---|
| `createJob` | `POST` | `/api/xxl/jobs/create` | `-` |
| `getJobDetail` | `POST` | `/api/xxl/jobs/detail` | `-` |
| `getLogDetail` | `POST` | `/api/xxl/logs/detail` | `-` |
| `listJobOptions` | `POST` | `/api/xxl/jobs/options` | `-` |
| `listJobs` | `POST` | `/api/xxl/jobs/list` | `-` |
| `listLogs` | `POST` | `/api/xxl/logs/list` | `-` |
| `removeJob` | `POST` | `/api/xxl/jobs/remove` | `-` |
| `startJob` | `POST` | `/api/xxl/jobs/start` | `-` |
| `stopJob` | `POST` | `/api/xxl/jobs/stop` | `-` |
| `triggerJob` | `POST` | `/api/xxl/jobs/trigger` | `-` |
| `updateJob` | `POST` | `/api/xxl/jobs/update` | `-` |

## 5. 流式与协议接口（补充）
| Controller | 方法 | HTTP | 路径 | 权限/说明 |
|---|---|---|---|---|
| `AICallController` | `stream` | `POST` | `/api/ai/stream` | `agent:read`，SSE 流式对话 |
| `AgentRuntimeController` | `stream` | `POST` | `/api/agents/{agentCode}/stream` | `agent:invoke`，SSE 流式 Agent 输出 |
| `McpGatewayController` | `establishSseConnection` | `GET` | `/api/gateway/{gatewayId}/mcp/sse` | 外部 MCP Client 建连（SSE） |
| `McpGatewayController` | `handleMessage` | `POST` | `/api/gateway/{gatewayId}/mcp/message?sessionId=...` | JSON-RPC 消息处理与回推 |

## 6. 作业触发入口（XXL）
这些不是 HTTP API，而是 `@XxlJob` 处理器名称：
- `approvalExpireHandler`
- `agentScheduleHandler`
- `chatHistoryCleanupHandler`
- `mcpServerCSDNHandler`
- `ragTaskAutoRetryHandler`
- `ragTaskCleanupHandler`
- `ragTaskTimeoutHandler`
- `workflowRunCleanupHandler`

## 7. 维护建议
- 新增 Controller 接口时，建议同步更新本文件。
- 可复用脚本思路：提取 `@RequestMapping` + `@*Mapping` + `@SaCheckPermission`。
- 接口变更优先保持路径语义稳定，避免前端和自动化脚本大面积改造。
