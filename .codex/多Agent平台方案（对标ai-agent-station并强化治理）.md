# 多 Agent 平台方案（对标 ai-agent-station 并强化治理）

## 1. 目标与边界

### 1.1 目标

在 `ai-mcp-knowledge-study` 现有能力基础上，建设“后者形态”的平台：

1. 平台内可创建多个 Agent，每个 Agent 有独立配置与独立运行入口（按 `agentId/agentCode` 路由）。
2. 同时支持两类运行态：
   - 对话运行态：同步 + SSE 流式
   - 任务运行态：异步长任务 + XXL 调度触发
3. 具备控制面能力：草稿、发布、回滚（发布可审计、可追溯）。
4. 单租户，但提供“组织隔离”能力（组织/项目维度的权限边界）。
5. 默认允许工具调用，但必须受“Agent 允许工具集合 + 权限校验 + 风险门禁”约束。
6. 输出强制结构化，平台保证上游永远拿到结构化结果（失败自动修复重试）。

### 1.2 非目标（本阶段不强求）

1. 不要求一开始就落地 DAG/状态机可视化编排（先覆盖线性流水线，底层可扩展）。
2. 不要求马上建设完整评测/灰度/A-B 平台（先做最小发布与回滚闭环）。
3. 不强求多租户（保持单租户 + 组织隔离）。

## 2. 现有能力盘点（ai-mcp-knowledge-study 已具备）

以下能力已在工程中存在，可直接复用：

1. 对话入口：`/api/ai/chat`、`/api/ai/stream`（流式支持 usage 事件回传）。
2. 会话与消息：`/api/ai/sessions/*`，表 `ai_chat_session`、`ai_chat_message`。
3. RAG 异步任务：`/api/ai/rag/task/*`（list/progress/cancel/retry），表 `ai_rag_task`。
4. MCP 平台化：
   - 对外 Gateway：`/api/gateway/{gatewayId}/mcp/sse`、`/api/gateway/{gatewayId}/mcp/message`
   - 管理面：`/api/mcp/servers`、`/api/gateway/manage`
5. 调度中心：`/api/xxl/*`（任务列表、触发、日志等）。
6. 身份与权限：Sa-Token + `@SaCheckPermission`，表 `sys_user/sys_role/sys_permission/sys_org/...`。
7. 审计：审计查询接口 + AOP 审计切面（身份域、网关域、模型/任务类型等配置域）。

本方案的核心工作是：补齐“Agent 控制面”并将上述能力收敛到统一产品模型中。

## 3. 关键决策（已确认）

1. 平台形态：多 Agent（后者形态）。
2. 模型选择：默认按 `TaskType` 策略推荐/降级；允许 Agent 特例“固定模型”。
3. RAG：Agent 绑定默认标签集合 + 允许覆盖白名单；未命中允许常识回答，但必须标注不确定。
4. 工具：工具集合绑定到 Agent（能力声明）；运行时按模型能力与风险策略过滤；默认允许工具。
5. Prompt：采用“系统 Prompt 模板”资产化；所有模板都强制结构化输出。
6. 发布：需要草稿/发布/回滚。
7. 调度：需要对接 XXL，支持 Agent 被调度执行。

## 4. 总体方案概览

### 4.1 核心思想

1. 新增“Agent”一等对象与“AgentVersion（版本）”对象。
2. 运行入口只依赖 `agentId`，由控制面解析当前已发布版本并执行。
3. 对话运行态与任务运行态共用“同一套 AgentVersion 配置”，避免两套配置分叉。
4. 将系统提示词做成“模板资产”，AgentVersion 引用模板并填入参数；发布时固化快照。
5. 将工具集合做成“AgentVersion 能力声明”，由运行时装配到 ChatClient/MCP 调用链路。
6. 强制结构化输出：模型输出不符合结构时，平台自动触发“修复重试”以保证稳定协议。

### 4.2 线性流水线与可扩展性

短期按线性流水线覆盖 `ai-agent-station` 的“client 顺序执行”心智；中长期将线性流程视为 DAG 的子集，运行内核设计预留：

1. 节点级超时/重试/补偿
2. 幂等与断点恢复
3. 人工审批节点（工具风险升级时可用）

## 5. 核心对象模型（建议）

### 5.1 Agent

字段建议：

1. `id`
2. `agentCode`（唯一、用于路由与外部集成）
3. `agentName`
4. `description`
5. `orgId`（归属组织，用于隔离）
6. `status`（启用/禁用）
7. `currentPublishedVersionId`（指向当前已发布版本）

### 5.2 AgentVersion（草稿/发布/历史）

字段建议：

1. `id`
2. `agentId`
3. `versionNo`（递增或语义化）
4. `state`（DRAFT、PUBLISHED、ARCHIVED）
5. `systemPromptTemplateId` + `templateParams`（JSON）
6. `outputSchemaId` 或 `outputContract`（结构化输出契约）
7. `modelStrategy`：
   - `TASK_TYPE_POLICY`：`taskTypeCode`
   - `FIXED_MODEL`：`modelId`
8. `ragMode`（DISABLED/OPTIONAL/REQUIRED）
9. `defaultRagTags`（JSON）
10. `allowedRagTags`（JSON）
11. `toolSet`（允许的工具集合，按你现有网关工具/注册工具模型引用）
12. `runtimeConfig`（超时、最大轮次、温度等）
13. `createdAt/updatedAt`

### 5.3 SystemPromptTemplate（模板资产）

字段建议：

1. `id`
2. `templateCode`（唯一）
3. `templateName`
4. `content`（包含占位符）
5. `variableSpec`（变量契约：变量名、类型、必填、默认值、说明）
6. `versionNo`、`state`（草稿/发布/归档）
7. `createdAt/updatedAt`

### 5.4 AgentSchedule（调度配置，XXL 关联）

字段建议：

1. `id`
2. `agentId`
3. `agentVersionId`（建议只允许绑定已发布版本；回滚要可控）
4. `jobId`（xxl job id）
5. `cron`
6. `enabled`
7. `payloadTemplate`（调度执行入参模板）
8. `createdAt/updatedAt`

### 5.5 AgentRun（执行实例，贯穿可观测/审计/成本）

字段建议：

1. `runId`（全局唯一，贯穿链路）
2. `agentId`
3. `agentVersionId`
4. `runType`（CHAT_SYNC/CHAT_STREAM/JOB_SCHEDULE/RAG_TASK等）
5. `requestId`（HTTP 请求级 ID，若有）
6. `operatorId`（触发人；调度触发可记录系统账号）
7. `orgId`
8. `status`（RUNNING/SUCCESS/FAILED/CANCELLED）
9. `startedAt/endedAt`
10. `cost`（token、工具调用次数等汇总）
11. `error`（摘要）

说明：你已有 `ai_call_log`、`sys_audit_event` 等表，本对象可先逻辑存在，再逐步整合/落表。

## 6. 运行态设计

### 6.1 对话运行态（同步/流式）

建议新增“按 Agent 路由”的统一入口（内部复用现有 `AiChatAppService`）：

1. `POST /api/agents/{agentId}/chat`
2. `POST /api/agents/{agentId}/stream`

核心流程：

1. 校验调用权限（组织隔离 + `agent:invoke` 类权限码）。
2. 解析 `agentId` 的当前已发布版本 `agentVersionId`。
3. 构造运行上下文（runId、orgId、userId、sessionId、模型策略、RAG 策略、工具集合、输出契约）。
4. 装配 ChatClient（复用现有 `ChatClientEnhancer`），并把 `agentId/agentVersionId/runId` 注入上下文（用于工具治理与审计关联）。
5. 执行对话：
   - RAG：按策略检索；未命中允许常识回答但需填 `uncertainty`。
   - Tool：默认允许，但仅限 AgentVersion 的 toolSet 且通过权限/风险门禁。
6. 输出结构化校验：
   - 解析失败触发“修复重试”（最多 N 次）
   - 最终保证返回结构化 JSON 响应
7. 写入执行记录与成本统计，并与 `ai_call_log`、`sys_audit_event` 做关联（通过 runId）。

### 6.2 任务运行态（异步/调度）

调度执行建议统一走：

1. XXL Job 触发 -> 平台接收 -> 生成 runId -> 按 agentId + 已发布版本执行“任务型入口”
2. 任务型入口可以复用同一套 Prompt 模板与工具集合，但输入来自调度参数模板与外部数据源
3. 落库：run 记录 + 日志摘要 + 审计事件（“由哪个 job/谁发布的版本触发”）

与你现有 XXL 对接的整合点：

1. 发布时如果 AgentSchedule 启用，则同步 job 配置到 XXL（创建/更新）
2. 回滚时：
   - 要么仅切换 agent 的 publishedVersion 指针，job 仍按 agentId 执行（自动拿最新发布版本）
   - 要么强约束 job 绑定固定 versionId（更可控，回滚显式更新绑定）
建议优先采用“job 按 agentId 执行并读取当前发布版本”，以减少同步复杂度；但要确保回滚影响可解释且可审计。

## 7. Prompt 模板与结构化输出

### 7.1 模板类型（首批全量内置）

1. 通用对话助手（可工具）
2. 企业知识库问答（RAG 强约束）
3. 工具型执行助手（强制结构化、步骤化、可工具）
4. 定时任务执行模板（强制结构化、可审计、适配调度入参）

### 7.2 结构化输出契约（强制）

建议统一对外结构（JSON）至少包含：

1. `answer`：最终答案（可读文本）
2. `uncertainty`：不确定性声明（RAG 未命中/推断时必须填）
3. `citations`：证据列表（命中的知识条目/片段引用；无则空数组）
4. `tool_calls`：工具调用摘要（工具名、入参要点、结果要点；无则空数组）
5. `actions_next`：下一步建议（可选）

平台保障策略：

1. 模板强约束“只能输出结构化 JSON，不得输出额外文本”
2. 运行时做解析校验，失败触发“修复重试”（把解析错误原因回灌给模型）
3. 仍失败则返回标准错误结构，避免前端/调用方崩溃

## 8. 工具治理（默认允许工具，但受控）

### 8.1 绑定粒度

工具集合绑定到 AgentVersion（能力声明），而不是绑定到模型。

### 8.2 运行时门禁

调用某个工具前必须同时满足：

1. AgentVersion 声明允许该工具（toolSet）
2. 当前用户/组织有工具调用权限（如 `tool:invoke`，以及资源级权限）
3. 风险策略允许：
   - 低风险：自动执行
   - 高风险：需要审批或仅允许调度/系统账号执行

### 8.3 审计与追踪

每次工具调用必须带上：

1. `runId`
2. `agentId/agentVersionId`
3. `operatorId/orgId`
4. 输入输出摘要（敏感脱敏）

你现有网关/审计体系适合承接该要求。

## 9. RAG 策略（默认可选 + 白名单覆盖）

1. AgentVersion 定义 `defaultRagTags` 与 `allowedRagTags`。
2. 会话/请求只能在 allowed 范围内覆盖选择。
3. 未命中策略：允许常识回答，但必须在结构化输出的 `uncertainty` 字段标注不确定。

## 10. 发布/回滚（最小闭环）

### 10.1 状态机

1. DRAFT：仅可编辑，不影响线上
2. PUBLISHED：线上生效
3. ARCHIVED：历史版本，不可编辑

### 10.2 发布行为

1. 对 AgentVersion 做校验（模板变量齐全、结构化契约存在、RAG/工具白名单合法、权限合法）。
2. 生成发布记录（含操作者、时间、变更摘要）。
3. 切换 Agent 的 `currentPublishedVersionId`。
4. 记录审计事件（发布/回滚属于高风险操作）。

### 10.3 回滚行为

1. 只能回滚到历史已发布版本。
2. 切换 `currentPublishedVersionId` 并落审计。
3. 若调度执行依赖发布版本，回滚必须可被追溯到“哪次回滚影响了哪些 job 执行”。

## 11. 权限与组织隔离（单租户）

建议新增/明确三类权限码：

1. `agent:read`、`agent:write`（管理）
2. `agent:publish`（发布/回滚）
3. `agent:invoke`（调用）

组织隔离策略：

1. Agent 归属 orgId，非授权组织不可见/不可调用
2. 工具、知识库、凭证等资源同理（可复用你现有 RBAC 与审计）

## 12. 做得更好的关键点（相对 ai-agent-station）

1. 控制面：草稿/发布/回滚 + 审计
2. 组织隔离：资源级权限（Agent/Tool/RAG）而不仅是全局 CRUD
3. 工具治理：默认允许但受控（白名单、风险分级、审批扩展点）
4. 强制结构化输出：平台兜底解析与修复重试，保证协议稳定
5. 统一 runId：将模型调用、工具调用、RAG、调度执行串成可追溯链路（便于成本与故障定位）

## 13. 里程碑（建议优先级）

P0（最短闭环）：

1. SystemPromptTemplate 资产化（含变量契约、发布/回滚）
2. Agent + AgentVersion（草稿/发布/回滚）
3. 按 Agent 路由的 chat/stream 入口（复用现有对话能力）
4. 会话绑定 agentId/agentVersionId（可追溯）

P1（平台化领先）：

1. toolSet 绑定到 AgentVersion + 工具风险门禁 + 审计关联 runId
2. RAG 标签白名单覆盖
3. 结构化输出解析失败修复重试机制

P2（调度与运营）：

1. AgentSchedule 与 XXL 完整联动（发布/回滚影响可追溯）
2. 运行看板：按 Agent/组织统计成功率、耗时、token、工具调用分布

## 14. 风险与对策

1. 风险：结构化输出不稳定导致前端解析失败
   - 对策：平台侧解析校验 + 修复重试 + 标准错误结构
2. 风险：工具默认允许带来高风险误调用
   - 对策：toolSet 白名单 + 权限校验 + 风险分级 + 审计
3. 风险：回滚影响调度任务行为不可解释
   - 对策：调度执行记录必须落 `agentVersionId`，并可回放查询

