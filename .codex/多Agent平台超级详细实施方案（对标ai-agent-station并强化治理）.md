# 多 Agent 平台超级详细实施方案（对标 ai-agent-station 并强化治理）

文档目标：在 `ai-mcp-knowledge-study` 现有能力上，建设“多 Agent 平台（后者形态）”，并显著强化治理：组织隔离、发布可控、工具风险门禁、结构化输出稳定协议、全链路可追溯。

适用范围：后端（Spring Boot + Spring AI + MCP/Gateway）、前端管理台（Vue3）、数据库（MySQL 8）。

更新时间：2026-02-14

---

## 0. 已确认决策（必须遵守）

1. Agent 路由主键：使用 `agentCode`（对外稳定标识）
2. PromptTemplate：先“单表 + version/state 字段”，不拆模板版本子表
3. 输出协议：先统一“平台标准结构”（Platform Contract）
4. 高风险工具：默认“生成审批单”，通过后采用“方式 B：自动继续运行”产出最终结构化结果
5. 组织隔离：所有新增业务表必须带 `org_id`，并做强制校验
6. 调度（XXL Job）：执行时“取当前发布版本”（`Agent.current_published_version_id`）
7. 平台标准结构：必须包含 `meta` 字段（建议项已确认）
8. 模板作用域：支持 GLOBAL 模板（平台内置），org 可引用但不可编辑
9. 超级管理员跨 org 管理：必须显式选择目标 org（不允许默认全局），并对跨 org 操作强审计

---

## 1. 现状与复用点（当前项目能力映射）

### 1.1 工程与分层（DDD 基座已具备）

1. Trigger 层：HTTP Controller、Filter/Aspect、XXL Job 入口（`ai-mcp-knowledge-trigger`）
2. Application 层：用例编排、ChatClient 组装、运行上下文（`ai-mcp-knowledge-application`）
3. Domain 层：实体、聚合、仓储接口（`ai-mcp-knowledge-domain`）
4. Infrastructure 层：MyBatis Mapper、仓储实现、MCP/Gateway 工具适配、审计落库（`ai-mcp-knowledge-infrastructure`）
5. App 模块：启动与 Advisor（trace/logging 等）（`ai-mcp-knowledge-app`）

### 1.2 可直接复用的关键机制

1. ChatClient 装配集中：`ChatClientAssemblyService` + `ChatClientEnhancer`，可统一注入 Advisors 与 ToolCallbackProvider
2. 工具来源已合并：动态 MCP 工具 + Gateway HTTP 工具，统一通过 ToolCallbackProvider 暴露给模型
3. 工具可见性过滤已有 ThreadLocal：`GatewayToolBindingContextHolder` 可扩展承载 AgentVersion 上下文
4. 审计两条线已具备：
   1) 配置审计：`ai_config_audit`
   2) 统一事件审计：`sys_audit_event`
5. 链路追踪已具备：HTTP Filter 与 ChatClient Advisor（建议将 runId 直接复用为 traceId）

### 1.3 与目标方案的主要差距

1. 缺少 Agent/AgentVersion/PromptTemplate 的控制面数据模型与接口
2. 运行入口当前是通用对话（按 model/session/rag tags），不是按 agentCode + 发布版本执行
3. 工具绑定维度只有 MODEL/SESSION，缺少 AGENT_VERSION 维度与风险门禁
4. RAG 标签没有白名单治理
5. 输出无结构化契约与平台兜底（解析失败修复重试）
6. org 隔离未进入业务资源模型（新增对象需从第一天强制 org_id）

---

## 2. 目标能力清单（对标 + 强治理增强点）

### 2.1 对标 ai-agent-station 的“后者形态”能力

1. 多 Agent：可创建多个 Agent，每个 Agent 独立配置、独立运行入口（按 agentCode 路由）
2. 双运行态：对话运行态（同步 + SSE）与任务运行态（XXL 调度触发）共用同一套 AgentVersion
3. 控制面闭环：草稿、发布、回滚（可审计、可追溯）

### 2.2 强化治理（平台差异化优势）

1. 组织隔离：Agent/Template/Version/Schedule/Run/Approval 全部 org_id 强制边界
2. 工具治理：默认允许但受控（AgentVersion allowlist + RBAC + 风险门禁 + 审批）
3. 输出协议稳定：平台标准结构 + 解析校验 + 修复重试 + 终态兜底结构（保证上游永远拿到结构化结果）
4. 全链路可追溯：runId=traceId 贯穿模型调用、RAG、工具、审批、调度

---

## 3. 总体架构（控制面 / 运行面分离）

### 3.1 控制面（Control Plane）

1. Agent：平台一等对象（org 归属，启停用，指向当前发布版本）
2. AgentVersion：配置快照载体（草稿/发布/归档），发布后不可变
3. PromptTemplate：模板资产化（单表版本字段），支持 GLOBAL/ORG 两种作用域
4. AgentSchedule：XXL 调度配置（执行时取当前发布版本）
5. 审计：发布/回滚/审批/关键配置变更必须写 `sys_audit_event` 与配置审计

### 3.2 运行面（Runtime Plane）

1. 按 agentCode 路由统一入口：chat/stream/xxlInvoke
2. 运行时装配：
   1) 模型策略：任务类型策略或固定模型
   2) RAG 策略：mode + default/allowed tags
   3) 工具策略：allowlist + 风险门禁 + 审批
   4) 输出：平台标准结构 v1 + 修复重试
3. 运行记录：落 `agent_run`，并与审计事件、调用日志串联

---

## 4. 数据模型设计（字段级清单）

说明：以下为字段清单与约束建议，不提供 DDL。所有新增表必须有 `org_id` 且所有查询/变更必须校验 org 边界。

### 4.1 Agent（平台一等对象）

表：`agent`

1. `id`
2. `org_id`
3. `agent_code`：对外唯一标识（建议唯一约束：`uk_org_agent_code (org_id, agent_code)`）
4. `agent_name`
5. `description`
6. `status`：ENABLED/DISABLED
7. `current_published_version_id`：当前发布版本（可空，未发布时为空）
8. `created_by`、`updated_by`
9. `created_at`、`updated_at`

规则：

1. DISABLED 的 Agent 不可 invoke（HTTP/XXL 均不可）
2. agentCode 一旦对外集成后不建议修改（可仅允许管理员改且强审计）

### 4.2 AgentVersion（配置快照载体）

表：`agent_version`

基础字段：

1. `id`
2. `org_id`
3. `agent_id`
4. `version_no`：递增整数（每个 agent 内递增）
5. `state`：DRAFT/PUBLISHED/ARCHIVED
6. `change_summary`：变更摘要（发布审计与运营必需）

Prompt（模板引用 + 发布快照）：

1. `prompt_template_id`
2. `prompt_template_version_no`：发布时固化；草稿可变
3. `template_params_json`：发布时固化；草稿可变
4. `system_prompt_snapshot`：发布时生成并固化；发布后不可变（用于回放与审计）

输出契约（平台标准结构）：

1. `output_contract_version`：固定 v1（后续升级 v2/v3）
2. `output_contract_options_json`：可选（例如：是否强制 citations/tool_calls 字段存在、最大长度等）

模型策略：

1. `model_strategy_type`：TASK_TYPE_POLICY/FIXED_MODEL
2. `task_type_code`：当 TASK_TYPE_POLICY 时必填
3. `fixed_model_id`：当 FIXED_MODEL 时必填

RAG 策略：

1. `rag_mode`：DISABLED/OPTIONAL/REQUIRED
2. `default_rag_tags_json`
3. `allowed_rag_tags_json`

工具策略：

1. `tool_policy_mode`：固定 ALLOWLIST_ONLY
2. `allowed_tool_keys_json`：允许工具集合（toolKey 列表）
3. `tool_risk_policy_json`：可选（例如 HIGH 是否需要审批、审批超时等；P1 可先默认）

运行参数：

1. `timeout_ms`
2. `max_turns`
3. `temperature`
4. `repair_retry_times`：结构化修复重试次数（建议默认 2）

审计字段：

1. `created_by`、`updated_by`
2. `created_at`、`updated_at`

规则：

1. PUBLISHED 版本不可编辑；仅允许归档为 ARCHIVED
2. 同一 Agent 可存在多个历史 PUBLISHED，但 `Agent.current_published_version_id` 仅指向一个“当前生效版本”

### 4.3 PromptTemplate（单表版本字段 + 作用域）

表：`prompt_template`

1. `id`
2. `scope`：GLOBAL/ORG
3. `org_id`：
   1) scope=ORG：必填
   2) scope=GLOBAL：固定为 0（或 NULL，推荐 0）
4. `template_code`：唯一（建议唯一约束：`uk_scope_code (scope, template_code)` 或 `uk_org_code (org_id, template_code)` + GLOBAL 单独约束）
5. `template_name`
6. `version_no`
7. `state`：DRAFT/PUBLISHED/ARCHIVED
8. `content`：模板正文（含占位符）
9. `variable_spec_json`：变量契约（变量名、类型、必填、默认值、说明）
10. `created_by`、`updated_by`
11. `created_at`、`updated_at`

规则：

1. GLOBAL 模板：org 可引用，不可编辑；仅平台管理员可管理
2. AgentVersion 发布时必须固化 system_prompt_snapshot，避免模板发布后影响线上行为

### 4.4 AgentSchedule（XXL 调度配置）

表：`agent_schedule`

1. `id`
2. `org_id`
3. `agent_id`
4. `cron`
5. `enabled`
6. `xxl_job_id`
7. `payload_template_json`：调度入参模板
8. `created_by`、`updated_by`
9. `created_at`、`updated_at`

关键规则：

1. 调度执行时只绑定 agentCode（或 agentId），运行时取当前发布版本
2. 每次执行必须记录“实际使用的 agent_version_id”（写入 agent_run）

### 4.5 AgentRun（运行记录，强烈建议落表）

表：`agent_run`

1. `run_id`：建议直接复用 traceId（全局唯一）
2. `org_id`
3. `agent_id`
4. `agent_code`
5. `agent_version_id`
6. `run_type`：CHAT_SYNC/CHAT_STREAM/XXL_JOB
7. `trigger_source`：HTTP/XXL
8. `operator_id`
9. `operator_type`：user/system
10. `session_id`：对话场景可填
11. `status`：RUNNING/SUCCESS/FAILED/PENDING_APPROVAL/CANCELLED
12. `model_id_used`、`model_name_used`
13. `prompt_tokens`、`completion_tokens`、`total_tokens`
14. `tool_call_count`、`tool_denied_count`
15. `repair_attempts`：结构化修复次数
16. `cost_ms`
17. `error_message`
18. `started_at`、`ended_at`

### 4.6 ApprovalRequest（统一审批单，覆盖高风险工具）

表：`approval_request`

1. `id`
2. `org_id`
3. `approval_type`：TOOL_INVOKE（P0/P1 先做这一类）
4. `status`：PENDING/APPROVED/REJECTED/CANCELLED/EXPIRED
5. `run_id`
6. `agent_id`
7. `agent_version_id`
8. `requester_id`
9. `requester_type`：user/system
10. `request_reason`：可空
11. `approver_id`：可空
12. `decision_comment`：可空
13. `decided_at`：可空
14. 工具专属字段：
    1) `tool_key`
    2) `risk_level`：LOW/MEDIUM/HIGH
    3) `arguments_snapshot_json`：入参快照（脱敏）
    4) `arguments_digest`：列表展示摘要
15. `expire_at`
16. `created_at`、`updated_at`

规则（方式 B：审批通过后自动继续运行）：

1. HIGH 风险工具触发时：
   1) 不执行工具
   2) 生成审批单
   3) agent_run 状态置为 PENDING_APPROVAL
   4) 对外返回“待审批”的平台标准结构（含 approvalRequestId）
2. 审批通过后：
   1) 平台执行工具调用（以系统身份或具备特权的服务身份）
   2) 将工具执行结果写入“运行上下文”
   3) 自动继续运行 LLM，产出最终平台标准结构并落库（run 终态 SUCCESS/FAILED）

---

## 5. toolKey 规范（强治理的基础设施）

必须统一使用稳定 toolKey，禁止依赖自增 ID 进行 allowlist。

1. Gateway HTTP 工具：`gateway:{gatewayId}:{toolName}`
2. MCP 工具：`mcp:{serverName}:{toolName}`

派生要求：

1. 审计事件、审批单、运行看板、AgentVersion allowlist 全部使用 toolKey
2. toolKey 到具体工具实现的映射必须可解析（支持从 toolKey 定位到 MCP 或 Gateway 的源数据）

---

## 6. 平台标准输出结构（Platform Contract v1）

### 6.1 结构定义（字段与语义）

顶层对象字段：

1. `meta`：必填（平台建议项已确认）
2. `answer`：必填（最终答案文本，可为空字符串但必须存在）
3. `uncertainty`：必填（无不确定可为空或固定值；RAG 未命中/推断时必须写明）
4. `citations`：必填（数组，可空；引用 RAG 证据或外部来源摘要）
5. `tool_calls`：必填（数组，可空；工具调用摘要）
6. `actions_next`：可选（数组，可空；下一步建议）
7. `status`：必填（SUCCESS/FAILED/PENDING_APPROVAL）
8. `error`：可选（当 FAILED 时填结构化错误信息）

`meta` 建议字段（至少包含）：

1. `runId`：本次执行唯一 ID（建议等于 traceId）
2. `agentCode`
3. `agentVersionNo`
4. `agentVersionId`
5. `orgId`
6. `modelUsed`：模型名称或标识
7. `costMs`
8. `repairAttempts`：结构化修复次数

当 `status=PENDING_APPROVAL` 时必须包含：

1. `meta.approvalRequestId`
2. `meta.pendingToolKey`
3. `meta.riskLevel`

### 6.2 平台兜底策略（保证永远可解析）

1. 运行时对模型输出做严格解析与校验
2. 解析失败触发“修复重试”，最多 `repair_retry_times`
3. 终态失败也必须返回契约结构（`status=FAILED`），不得返回不可解析文本

### 6.3 SSE 输出策略（解决流式与结构化冲突）

SSE 采用事件分型（建议）：

1. `delta`：用于前端展示的增量文本（不作为最终可信结果）
2. `final`：流结束时发送完整 Platform Contract v1（唯一可信结果）
3. 高风险工具触发审批：直接发送 `final`（status=PENDING_APPROVAL）并结束连接

---

## 7. 权限模型与 org 隔离（强制执行）

### 7.1 权限码建议

1. `agent:read`：查看 Agent/Version/Template（可按资源细分，P0 可先粗）
2. `agent:write`：编辑 Agent/草稿版本
3. `agent:publish`：发布/回滚
4. `agent:invoke`：调用运行入口（必须新增，区分 read 与 invoke）
5. `tool:invoke`：允许调用工具（低/中风险）
6. `tool:approve`：审批高风险工具（若不新增可临时复用 `release:approve`，但不推荐长期混用）
7. `audit:read`：审计与运行记录查看

### 7.2 org 隔离硬规则

1. 对所有控制面资源（Agent/Version/Template/Schedule/Approval/Run）：
   1) 资源 `org_id` 必须等于当前 org
   2) 跨 org 访问建议返回 404（减少枚举）
2. 平台管理员（super admin）可跨 org，但必须：
   1) 显式选择目标 org（例如请求头/请求字段携带 `targetOrgId` 或管理端先切换“当前管理 org”）
   2) 强审计（写 sys_audit_event），并同时记录：
      1) `operator_org_id`：操作者所属 org（责任归属）
      2) `resource_org_id`：被操作资源 org（影响范围）
   3) 响应中 meta 标记 `crossOrg=true`（可选）

### 7.3 跨 org 管理交互口径（已确认）

目标：避免超级管理员“无意间在错误的 org 下创建/修改资源”，同时保证审计可追溯。

规则：

1. 超级管理员进入管理台后必须先“选择目标组织（targetOrgId）”，后续所有管理操作默认作用于该 targetOrgId。
2. 禁止“默认全局”：
   1) 未选择 targetOrgId 时，禁止进行写操作（create/update/publish/rollback/approve 等）
   2) 读操作可按产品选择：建议也要求选择 targetOrgId，避免误读与数据量过大
3. 所有跨 org 管理写操作必须写审计，且审计中必须包含 operator_org_id 与 resource_org_id。

实现建议（不限定具体技术形态）：

1. 管理端采用“组织切换器”，将 targetOrgId 固化在请求上下文（请求头/请求体/会话均可）。
2. 服务端统一从上下文获取“当前 org”：
   1) 普通用户：currentOrgId = operatorOrgId
   2) 超级管理员：currentOrgId = targetOrgId（必须显式提供）

---

## 8. 运行流程（按 agentCode 路由）

### 8.1 同步对话（/agents/{agentCode}/chat）

1. 解析 org + operator（用户身份）+ 权限校验（`agent:invoke`）
2. 加载 Agent（org_id + agentCode），校验 status=ENABLED
3. 加载当前发布版本 `agent.current_published_version_id`，校验版本 state=PUBLISHED
4. 生成 runId（建议直接复用 traceId），创建 agent_run（RUNNING）
5. 装配运行上下文：
   1) 模型策略（taskType policy 或 fixed）
   2) RAG 策略（mode/default/allowed）
   3) 工具 allowlist（toolKey 列表）
   4) 输出契约 v1 参数（含 meta）
6. 构造 Prompt：
   1) system_prompt_snapshot
   2) tool 使用说明（仅限 allowlist 后的工具集）
   3) RAG 上下文注入（按策略）
7. 执行 LLM：
   1) 工具调用时：RBAC + 风险门禁
   2) HIGH 风险：生成审批单并中断执行，返回 PENDING_APPROVAL 结构
8. 结构化解析校验：
   1) 失败则修复重试
   2) 终态失败返回 FAILED 结构
9. 更新 agent_run（终态 + 成本 + 工具计数 + 修复次数）
10. 写审计事件：
   1) agent invoke
   2) tool invoke/deny/approval_requested

### 8.2 流式对话（/agents/{agentCode}/stream）

在 8.1 基础上：

1. `delta` 事件用于逐步展示 answer（或展示模型原始输出的可视部分）
2. `final` 事件在结束时必须发送完整平台结构
3. 若触发审批：直接发 `final`（PENDING_APPROVAL）并结束

### 8.3 XXL 调度执行（取当前发布版本）

1. XXL handler 入参：agentCode + payload
2. 运行时取 current_published_version_id
3. run_type=XXL_JOB、operator_type=system（或配置特定系统账号）
4. 仍执行同一套治理（工具审批仍生效，HIGH 风险会生成审批单）
5. 每次执行必须落 agent_run，并记录 agent_version_id

---

## 9. 工具治理与审批（方式 B：审批后自动继续运行）

### 9.1 风险分级最小策略

1. LOW：自动执行
2. MEDIUM：自动执行 + 强审计
3. HIGH：默认生成审批单（不执行）

风险等级来源（建议顺序）：

1. toolKey 对应配置（平台工具风险配置表或字段扩展）
2. 未配置时默认 MEDIUM（建议）

### 9.2 审批后自动继续运行（方式 B）的实现要点

关键要求：审批通过后，必须能“拿回当时的上下文”继续生成最终结构化输出。

建议采用“可恢复运行上下文快照”设计（P1 最小可用）：

1. 在触发 HIGH 风险审批时，平台保存一份 `run_context_snapshot`（可落在 agent_run 扩展字段或独立表）：
   1) agent/version 信息
   2) 原始用户输入
   3) 已完成的 RAG 结果摘要（可选）
   4) 计划调用的 toolKey 与 arguments（脱敏）
   5) 当前对话/步骤状态（最小可为“重新跑一次 LLM，但强制注入 tool 结果”）
2. 审批通过后执行工具，得到 toolResult，然后触发“继续运行”：
   1) 将 toolResult 作为额外上下文注入（系统消息或工具结果消息）
   2) 再次调用 LLM，要求输出 Platform Contract v1
3. 继续运行也必须走结构化校验与修复重试
4. 审批通过/拒绝/执行结果必须写 sys_audit_event，并更新 approval_request 与 agent_run

### 9.3 审批 SLA 与过期

1. HIGH 审批必须支持过期时间 `expire_at`
2. 过期后：
   1) 审批单置为 EXPIRED
   2) 对应 run 置为 FAILED 或 CANCELLED（建议 FAILED 并给出可解释 error）

---

## 10. 发布/回滚治理（最小闭环 + 可扩展审批）

### 10.1 AgentVersion 状态机

1. DRAFT：可编辑，不影响线上
2. PUBLISHED：线上生效，不可编辑
3. ARCHIVED：历史版本，不可编辑

### 10.2 发布流程（最小闭环）

1. 校验草稿版本：
   1) 模板变量齐全（template_params 与 variable_spec 对齐）
   2) 生成并固化 system_prompt_snapshot
   3) 输出契约 v1 已绑定
   4) allowed_tool_keys 合法（toolKey 格式可解析）
   5) RAG tags 合法且是 org 可用集合的子集
   6) FIXED_MODEL 时模型可用且启用；TASK_TYPE_POLICY 时 task_type_code 存在
2. 写审计：
   1) ai_config_audit（配置审计）
   2) sys_audit_event（发布事件）
3. 切换 `agent.current_published_version_id`

### 10.3 回滚流程

1. 只能回滚到历史 PUBLISHED 版本
2. 写审计（同发布）
3. 切换 `current_published_version_id`

扩展（P3）：发布审批可复用 approval_request（approval_type=AGENT_RELEASE），或单独 release_request 表。

---

## 11. 审计事件字典（建议统一规范）

所有关键动作必须写 `sys_audit_event`，建议约定：

1. 事件类型（event_type）：
   1) `AGENT_MANAGEMENT`
   2) `AGENT_RELEASE`
   3) `AGENT_INVOKE`
   4) `TOOL_INVOKE`
   5) `TOOL_APPROVAL`
   6) `TEMPLATE_MANAGEMENT`
   7) `SCHEDULE_MANAGEMENT`
2. 资源类型（resource_type）：
   1) `agent`
   2) `agent_version`
   3) `prompt_template`
   4) `tool`
   5) `approval_request`
   6) `agent_run`
3. 动作（action）示例：
   1) create/update/enable/disable
   2) publish/rollback/archive
   3) invoke
   4) approval_requested/approved/rejected/expired
4. resource_id 建议：
   1) agent：agentCode
   2) tool：toolKey
   3) run：runId

注意：审计事件必须包含 request_id（建议等于 runId/traceId），以便跨系统串联。

---

## 12. API 设计清单（字段级契约，不含示例代码）

### 12.1 控制面：Agent

1. 创建：`POST /api/agents`
   1) 入参：agentCode、agentName、description、status
   2) 出参：Agent 基础信息（含 orgId）
2. 列表：`POST /api/agents/list`
   1) 入参：分页 + keyword + status
   2) 出参：分页 records（含 currentPublishedVersionId/versionNo）
3. 详情：`POST /api/agents/detail`
   1) 入参：agentCode
4. 更新：`POST /api/agents/update`
   1) 入参：agentCode + 可改字段
5. 启用/禁用：`POST /api/agents/enable`、`POST /api/agents/disable`

权限：

1. read：agent:read
2. write：agent:write

### 12.2 控制面：PromptTemplate（GLOBAL/ORG）

1. 创建草稿：`POST /api/templates/create`
2. 更新草稿：`POST /api/templates/update`
3. 发布：`POST /api/templates/publish`
4. 归档：`POST /api/templates/archive`
5. 列表：`POST /api/templates/list`
6. 详情：`POST /api/templates/detail`

规则：

1. scope=GLOBAL 的模板仅 PLATFORM_ADMIN 可 create/update/publish/archive
2. org 只能引用 GLOBAL 模板（在 AgentVersion 里选择），不可修改其内容

### 12.3 控制面：AgentVersion

1. 创建草稿：`POST /api/agents/{agentCode}/versions/draft/create`
2. 更新草稿：`POST /api/agents/{agentCode}/versions/draft/update`
3. 版本列表：`POST /api/agents/{agentCode}/versions/list`
4. 版本详情：`POST /api/agents/{agentCode}/versions/detail`
5. 发布：`POST /api/agents/{agentCode}/versions/publish`
6. 回滚：`POST /api/agents/{agentCode}/versions/rollback`
7. 归档：`POST /api/agents/{agentCode}/versions/archive`

权限：

1. draft 管理：agent:write
2. publish/rollback/archive：agent:publish

### 12.4 运行面：Agent Runtime（按 agentCode）

1. 同步：`POST /api/agents/{agentCode}/chat`
2. 流式：`POST /api/agents/{agentCode}/stream`
3. XXL/任务调用：`POST /api/agents/{agentCode}/invoke`（供内部或调度触发）

请求入参建议：

1. `sessionId`（可选）
2. `content`（必填）
3. `ragTags`（可选，受 allowed 白名单约束）
4. `options`（可选：是否强制 RAG、是否允许常识回答等，最终由 AgentVersion 决策）

响应出参：

1. 平台标准结构 v1（含 meta）

权限：

1. invoke：agent:invoke

### 12.5 审批：ApprovalRequest（高风险工具）

1. 列表：`POST /api/approvals/list`
2. 详情：`POST /api/approvals/detail`
3. 审批通过：`POST /api/approvals/approve`
4. 审批拒绝：`POST /api/approvals/reject`

权限：

1. list/detail：audit:read（或单独 approval:read）
2. approve/reject：tool:approve

审批通过后的行为（方式 B）：

1. 触发“继续运行”，最终 run 产出 SUCCESS/FAILED 的平台标准结构并可在 Runs 中查询

### 12.6 运行记录：AgentRun

1. 列表：`POST /api/runs/list`（按 org、agentCode、status、runId、时间范围过滤）
2. 详情：`POST /api/runs/detail`

权限：

1. audit:read（或单独 runs:read）

### 12.7 调度：AgentSchedule

1. 创建/更新：`POST /api/schedules/save`
2. 列表：`POST /api/schedules/list`
3. 启用/禁用：`POST /api/schedules/enable`、`POST /api/schedules/disable`

权限：

1. agent:write 或 workflow:write（需统一命名；建议 agent:write）

---

## 13. 迭代路线图（可交付闭环）

## 13.1 执行进度（截至 2026-02-14）

当前开发分支：`feat/multi-agent-platform-governance`

已完成：

1. Iteration 1（P0 控制面）
   1) Agent/AgentVersion/PromptTemplate（含 GLOBAL）落库与基础管理接口
   2) 发布：固化 `system_prompt_snapshot` + 固化 `prompt_template_version_no` + 切换 `agent.current_published_version_id`
   3) org 隔离：Agent/Version/Template 强制 `org_id`
2. Iteration 2（P0 运行入口）
   1) `/agents/{agentCode}/chat`、`/stream`、`/invoke`
   2) 运行记录 `agent_run` 落库（执行时取 current published version）
   3) Platform Contract v1 已落：模型输出强制 JSON，平台解析校验 + 修复重试；同步与 SSE final 都返回可解析 v1
3. Iteration 3（P1 工具 allowlist + 审计/计数）已推进到可用状态
   1) AgentVersion allowlist（`allowed_tool_keys_json`，toolKey）已在运行时生效
   2) ToolCallback 命名冲突治理：Gateway/MCP 工具对模型暴露的 function name 均加前缀（`gw_...` / `mcp_...`）
   3) 工具调用审计：写入 `sys_audit_event`（eventType=`TOOL_INVOKE`）
   4) 工具调用计数：每次工具调用原子递增 `agent_run.tool_call_count`
   5) RBAC 工具门禁：缺少 `tool:invoke` 时工具不执行，返回 `[PERMISSION_DENIED]...`，并递增 `agent_run.tool_denied_count`
   6) 工具拒绝审计：写入 `sys_audit_event`（eventType=`TOOL_INVOKE` action=`DENIED`）
   7) Gateway/MCP 基础表查询已补强 org 隔离（避免跨部门工具泄漏）
   8) 工具目录对外协议：`/api/mcp/tools/list` 已返回 toolKey/source/inputSchema 等字段，供前端配置 allowlist
4. Iteration 4（P1 高风险审批 + 方式B 自动续跑）已打通最小闭环
   1) HIGH 风险工具触发：自动生成 `approval_request`（PENDING）并中断执行
   2) run 侧：运行入口捕获审批异常，将 `agent_run.status` 置为 `PENDING_APPROVAL`，并返回 Platform Contract v1（含 approvalRequestId/toolKey/riskLevel）
   3) 上下文：`agent_run_context` 保存可恢复快照（最小包含原始输入）
   4) 审批接口：`/api/approvals/list|get|approve|reject`
   5) 方式B：审批通过后自动执行工具并续跑生成最终 `PlatformContractV1`
5. Iteration 5（P1 RAG 白名单治理）已完成
   1) 运行入口接入向量检索：按 `rag_mode/default/allowed` 决策检索
   2) REQUIRED 未命中：平台短路返回 `SUCCESS` + `uncertainty`（不调用模型、不装懂）
   3) 非 allowed tag：剔除并在 `actionsNext` 解释
   4) citations：平台按检索结果强填/覆盖（避免模型胡编证据）
6. Iteration 6（P2 XXL Schedule）已完成
   1) agent_schedule 控制面：`/api/schedules/list|get|create|update|enable|disable|remove`
   2) 与 XXL admin 联动：创建/更新 schedule 同步创建/更新 xxl-job（handler=`agentScheduleHandler`），启停同步 start/stop
   3) 执行器 handler：按 scheduleId/orgId 执行并落 `agent_run(run_type=XXL_JOB)`；触发审批按口径视为 job 成功并打印关键字段
7. 前端管理台（Vue3）已完成最小可用对接
   1) Agent 管理、版本管理、Prompt 模板、工具审批、Agent 调度、Agent 调用（playground）
   2) 超级管理员目标组织选择：统一注入 `X-Target-Org-Id`，未选择时写操作会被后端拦截

未完成（下一步）：

1. 暂无（已完成文档 Iteration 1-6 + 最小前端闭环；后续可按产品需求继续增强看板、压测、灰度发布与跨线程上下文治理等）

### Iteration 0（准备，约 1 周）

1. 固化规范：
   1) Platform Contract v1（含 meta）
   2) toolKey 规范
   3) org 隔离规则
   4) 审计事件字典
   5) 超级管理员跨 org 管理交互口径（必须显式选择 targetOrgId）
2. 落开发任务拆分与验收用例清单

交付物：

1. 本文档（已完成）
2. API 草案与字段级契约确认

### Iteration 1（P0 控制面，约 1-2 周）

范围：

1. agent、agent_version、prompt_template（含 GLOBAL）落库与基础管理接口
2. 发布/回滚最小闭环（含审计）

验收：

1. 一个 org 内可创建多个 agentCode
2. 可发布与回滚，且审计可按 agentCode/versionNo 查询

### Iteration 2（P0 运行入口，约 1-2 周）

范围：

1. `/agents/{agentCode}/chat`、`/stream`、`/invoke`
2. `agent_run` 落库
3. 输出强制结构化 v1 + 修复重试（同步优先）

验收：

1. 任意调用都返回可解析的 v1 结构（失败也返回 FAILED 结构）
2. run 可追溯到 agent/version/model/cost

### Iteration 3（P1 工具 allowlist + RBAC，约 1-2 周）

范围：

1. AgentVersion allowlist 生效（toolKey）
2. 工具调用审计与 agent_run 工具计数

验收：

1. allowlist 外工具无法调用成功
2. 工具调用可按 runId 串联审计

### Iteration 4（P1 高风险审批 + 自动续跑，约 1-2 周）

范围：

1. approval_request + 审批接口
2. HIGH 风险默认生成审批单
3. 审批通过后自动继续运行产出最终 v1（方式 B）

验收：

1. HIGH 风险工具未审批不会执行
2. 审批通过后能产出最终 v1 结果并落 run 记录

### Iteration 5（P1 RAG 白名单治理，约 1 周）

范围：

1. rag_mode/default/allowed 强约束
2. REQUIRED 未命中必须 uncertainty

验收：

1. 非 allowed tag 被拒绝或剔除并解释
2. REQUIRED 无命中不允许装懂

### Iteration 6（P2 XXL Schedule，约 1-2 周）

范围：

1. agent_schedule 管理与 XXL 联动
2. 执行时取当前发布版本 + run 落库

验收：

1. 回滚后下一次调度明确使用新版本（run 记录可查）

---

## 14. 风险与对策（治理优先）

1. 风险：结构化输出不稳定导致上游解析失败
   1) 对策：平台解析校验 + 修复重试 + 终态兜底结构（强制）
2. 风险：工具默认允许导致误操作
   1) 对策：AgentVersion allowlist + RBAC + 风险门禁 + HIGH 审批（默认）
3. 风险：审批通过后续跑难以复现上下文
   1) 对策：保存 run_context_snapshot（最小可“重新跑 LLM + 强制注入 toolResult”）
4. 风险：取当前发布版本导致调度行为漂移不可解释
   1) 对策：每次 run 强制记录 agent_version_id + 审计发布/回滚影响范围

---

## 15. 测试与验收用例（建议最低集合）

1. org 隔离：跨 org 访问 Agent/Version/Template/Schedule/Approval/Run 必须不可见
2. 发布冻结：PUBLISHED 版本不可编辑（接口层 + 数据层双保险）
3. 输出结构化：
   1) 非结构化输出可修复
   2) 多次失败仍返回 FAILED 结构
4. 工具治理：
   1) allowlist 外工具不可执行
   2) 无 `tool:invoke` 权限不可执行
5. 高风险审批：
   1) HIGH 触发生成审批单
   2) 审批通过后自动续跑产出最终 v1
   3) 审批拒绝/过期的 run 终态可解释
6. XXL：
   1) 执行使用当前发布版本
   2) 发布/回滚后下一次执行版本变化可验证
