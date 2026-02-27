# MCP 从 0 到 1 实战指南（项目版）

> 目标：把本项目里 MCP 相关代码串成一条清晰主线，做到“知道去哪看、为什么这样设计、出问题如何排查”。
> 范围：MCP Server 动态接入、Gateway 工具、ToolCallbackProvider 合并、ChatClient 工具注入、治理链路。

## 0. 如何使用这篇文档

- 你是第一次接触项目：先看第 1-5 章，再看第 8 章实战。
- 你在排障：直接跳第 9 章排障清单，再回看第 4、5 章。
- 你要读代码：直接按第 11 章的顺序读。

## 1. 基础概念（先统一术语）

### 1.1 MCP 是什么
- 可以把 MCP 理解为“大模型调用外部能力”的统一协议。
- 模型本身只负责生成文本，真正调用系统能力靠 Tool。

### 1.2 项目内工具来源
- 来源 A：外部 `MCP Server` 动态接入的工具。
- 来源 B：`HTTP Gateway` 配置出来的工具。

### 1.3 ToolCallbackProvider 是什么
- 可以理解成“工具清单供应商”。
- 模型能看到哪些工具，最终由它决定。

### 1.4 名词对照（避免混淆）
- `MCP 工具配置`：管理外部 MCP Server 连接。
- `HTTP 工具配置`：把内部 HTTP API 包装成工具。
- `MCP Gateway 协议入口`：给外部 MCP 客户端连入的协议端点，不是管理页。

## 2. 模块分工（你要知道每层干什么）

### 2.1 MCP 工具配置（外部服务接入）
- 前端：`ai-mcp-knowledge-web/src/views/mcp/index.vue`
- 后端接口：`/api/mcp/servers/*`
- 典型操作：新增配置、启用/禁用、单条刷新、全量刷新。

### 2.2 HTTP 工具配置（内部 API 工具化）
- 前端：`ai-mcp-knowledge-web/src/views/gateway/index.vue`、`.../gateway/tools/index.vue`
- 后端接口：`/api/gateway/manage/*`

### 2.3 MCP Gateway 协议入口（对外协议）
- 接口：`/api/gateway/{gatewayId}/mcp/sse`、`/api/gateway/{gatewayId}/mcp/message`
- 控制器：`McpGatewayController`

### 2.4 运行时三本 Registry（重点）
位置：`McpServerRuntimeServiceImpl`

- `clientRegistry`：`configId -> McpSyncClient`，记录真实连接对象。
- `metaRegistry`：`configId -> McpServerMeta`，记录服务元信息（当前主要是 `serverName`）。
- `configSnapshotRegistry`：`configId -> RuntimeConfigSnapshot`，记录上次生效配置，用于判定是否需要重连。

一句话：
- 数据库是“配置态”。
- Registry 是“运行态”。

## 3. 从配置到工具可用的主链路

### 3.1 配置阶段（落库）
1. 页面提交 MCP Server 配置。
2. `McpServerConfigController` -> `McpServerConfigAppServiceImpl` -> 配置表落库。
3. 这一步只是保存配置，不代表已建连。

### 3.2 运行时阶段（建连/重连）
1. 用户触发 `refresh` 或 `refresh-one`。
2. 应用层调用 `McpServerRuntimeService.refresh/registerOrUpdate`。
3. 运行时按 `STDIO/HTTP/SSE` 构建 `McpSyncClient` 并 `initialize()`。
4. 连接成功后，更新三本 Registry。

### 3.3 工具阶段（对模型可见）
1. `McpServerRuntimeServiceImpl.refreshToolCallbacks()` 汇总运行中 client。
2. 调用 `DynamicMcpToolCallbackProvider.updateClients(...)`。
3. `CompositeToolCallbackProvider` 合并 Dynamic + Gateway。
4. ChatClient 装配时注入 `defaultToolCallbacks(...)`，模型才真正可调用。

### 3.3.1 为什么 `McpSyncClient` 能“直接拿到 tool”
结论先说：
- 不是本地静态拿到，而是通过 MCP 协议向对端 MCP Server 发 `tools/list` 拿到。

本项目实际链路：
1. `McpServerRuntimeServiceImpl.registerOrUpdateInternal` 先创建 `McpSyncClient`，再执行 `client.initialize()` 完成握手。
2. `refreshToolCallbacks()` 把运行中的 `McpSyncClient` 组装成 `McpClientDescriptor`，传给 `DynamicMcpToolCallbackProvider.updateClients(...)`。
3. `DynamicMcpToolCallbackProvider.buildCallbacks()` 调 `SyncMcpToolCallbackProvider.syncToolCallbacks(...)`。
4. `SyncMcpToolCallbackProvider` 内部会对每个 client 执行 `mcpClient.listTools().tools()`，并转换成 `ToolCallback`。
5. 项目再把这些回调包装成 `GovernedToolCallback`，补 `toolKey/functionName` 做治理与命名。

前提条件：
- 连接已成功初始化（`initialize` 成功）。
- 对端 MCP Server 实现了 tools 能力。

失败时常见表现：
- `listTools` 返回空：通常是对端无工具或当前上下文被过滤。
- `listTools` 抛错：通常是连接、鉴权或服务端能力异常。

### 3.3.2 HTTP 网关工具是怎么进来的
结论先说：
- HTTP 网关工具不是通过 `McpSyncClient.listTools()` 拉远端，而是从本地 Gateway 配置与定义中构建。

本项目实际链路：
1. `GatewayToolCallbackProvider.getToolCallbacks()` 查询已启用网关与已启用工具注册项。
2. 通过 `loadToolDefinitions(...)` 拉取每个网关的工具定义（名称、描述、入参 schema 等）。
3. 组装 `ToolCandidate`，再应用可见性过滤（allowlist / 绑定关系）。
4. 对 `functionName` 做去重后，构建 `FunctionToolCallback` 并包装为 `GovernedToolCallback`。
5. `CompositeToolCallbackProvider` 将 Gateway 与 Dynamic MCP 两路工具合并后返回给 ChatClient 注入。

前提条件：
- 网关与工具在管理端为启用状态。
- 当前上下文允许该工具可见（未被 allowlist/绑定规则过滤）。

失败时常见表现：
- 工具不出现：多见于网关未启用、工具未启用、或被可见性规则过滤。
- 工具名冲突：同名 `functionName` 后续项会被跳过并记录告警日志。

### 3.3.3 Dynamic MCP vs HTTP Gateway 对照
| 维度 | Dynamic MCP 工具 | HTTP Gateway 工具 |
| --- | --- | --- |
| 工具来源 | 外部 MCP Server | 本地网关配置（`mcp_gateway` + `mcp_tool_registry`） |
| 发现方式 | `McpSyncClient.listTools()` -> `tools/list` | `GatewayToolService.listTools(...)` + 注册表构建 |
| 关键 Provider | `DynamicMcpToolCallbackProvider` | `GatewayToolCallbackProvider` |
| 可见性过滤 | allowlist（`toolKey`） | allowlist + 绑定关系（MODEL/SESSION/AGENT_VERSION） |
| 命名冲突处理 | `toolKey/functionName` 包装治理 | `functionName` 去重，重复项跳过并告警 |
| 执行目标 | 调用外部 MCP 对端 | 调用网关配置的 HTTP 接口 |
| 合并入口 | 合并到 `CompositeToolCallbackProvider` | 合并到 `CompositeToolCallbackProvider` |

### 3.4 调用链总览（配置 -> McpSyncClient -> 工具暴露 -> Agent 调用）

```mermaid
flowchart LR
    A[配置变更<br/>MCP 管理页 /api/mcp/servers] --> B[配置落库<br/>McpServerConfigAppServiceImpl]
    B --> C[触发刷新<br/>refresh / refresh-one]
    C --> D[运行时建连<br/>McpServerRuntimeServiceImpl.registerOrUpdate]
    D --> E[按类型构建传输层<br/>STDIO / SSE / HTTP]
    E --> F[创建 McpSyncClient<br/>通过 McpClient sync 构建]
    F --> G[初始化连接<br/>执行 client initialize]
    G --> H[写入运行时 Registry<br/>client/meta/snapshot]
    H --> I[刷新工具回调<br/>refreshToolCallbacks]
    I --> J[DynamicMcpToolCallbackProvider.updateClients]
    J --> K[生成 MCP ToolCallbacks<br/>SyncMcpToolCallbackProvider]
    K --> L[合并工具提供器<br/>CompositeToolCallbackProvider]
    L --> M[ChatClient 注入工具<br/>注入 defaultToolCallbacks]
    M --> N[Agent 运行时推理]
    N --> O{模型是否选择工具?}
    O -- 否 --> P[直接生成回答]
    O -- 是 --> Q[调用 ToolCallback]
    Q --> R[通过 McpSyncClient 调用外部 MCP Tool]
    R --> S[工具结果回流模型]
    S --> P
```

## 4. 两个关键状态：enabled vs running

- `enabled`：数据库配置开关，表示“允许参与运行时”。
- `running`：运行态状态，表示“当前是否真的有连接在跑”。

常见现象：
- `enabled=true, running=false`：通常是未刷新，或刷新时初始化失败。
- `enabled=false`：会走注销，运行态连接会被清掉。

实现策略：
- 代码用 `!Boolean.TRUE.equals(enabled)`，只有显式 `true` 才保留运行时连接。
- `false/null` 都按“不应运行”处理，策略偏保守（宁可下线，不可误上线）。

## 5. `McpServerRuntimeServiceImpl` 精读顺序（最实用）

按这个顺序看，不要从文件第一行硬读到最后。

### 5.1 先看对外入口
- `refresh`
- `registerOrUpdate`
- `unregister`
- `isRunning`
- `shutdown`

### 5.2 再看核心状态迁移
- `registerOrUpdateInternal`：判定是否重连 -> 回收旧连接 -> 建新连接 -> 回填 registry。
- `unregisterInternal`：注销与清理。
- `buildRuntimeConfigSnapshot`：无效重连判定依据。

### 5.3 再看建连策略
- `buildClient`（按 `STDIO/SSE/HTTP` 分派）
- `buildStdioClient`
- `buildSseClient`
- `buildHttpClient`
- `buildSyncClient`

### 5.4 最后看辅助函数
- `applyHeaders`
- `parseStringList`
- `parseStringMap`
- `getTimeout`
- `closeQuietly`
- `refreshToolCallbacks`

### 5.5 读完后的掌握检查
1. 你是否能说清“什么时候跳过重连”（快照相同且运行态完整）。
2. 你是否能描述三本 registry 在注册/更新/注销时的变化。
3. 你是否能说明 `enabled` 与 `running` 的边界。
4. 你是否能解释 `refreshToolCallbacks` 为什么只在变更后触发。

## 6. ToolCallbackProvider 关系（工具供应层）

### 6.1 三个 Provider 的职责
- `DynamicMcpToolCallbackProvider`：管理来自 MCP Server 的工具。
- `GatewayToolCallbackProvider`：管理来自 Gateway 的 HTTP 工具。
- `CompositeToolCallbackProvider`：合并两者，作为统一入口（`@Primary`）。

### 6.2 callback 与 Tool、MCP Server 的关系
- callback 是按 Tool 维度构建的，不是按 MCP Server。
- 一个 MCP Server 暴露多个 Tool，就会对应多个 callback。

## 7. ChatClient 装配与流式返回

### 7.1 工具注入发生在装配链，不在 Controller
关键路径：
- `AiChatAppServiceImpl.streamChat`
- `AiClientToolNode`
- `AiClientNode`
- `builder.defaultToolCallbacks(provider)`

### 7.2 SSE 分流：thinking 与 message
- `thinking`：思考分片，前端用于折叠展示。
- `message`：最终回答分片，用于主回答区渲染与落库。

当前实现要点：
- 思考分片不写入会话记忆。
- 最终回答才进入会话记忆，避免污染后续多轮上下文。

## 8. 两个实战例子

### 8.1 例子 A：接一个外部 MCP Server
1. 新增一条 STDIO/HTTP/SSE 配置。
2. 启用配置。
3. 触发刷新（单条或全量）。
4. 运行时建连并初始化。
5. Provider 更新 callback。
6. 聊天请求进来时，模型可见并可调用这些工具。

### 8.2 例子 B：把内部 HTTP API 包装成工具
1. 新建 gateway。
2. 配 `tool registry`（工具名、风险等级）。
3. 配参数映射。
4. 可选：做模型/会话/版本绑定。
5. 聊天时通过 `GatewayToolCallbackProvider` 参与合并注入。

## 9. 快速排障清单（建议按顺序）

1. MCP 配置是否 `enabled=true`。
2. `running` 是否为 true（注意它不是数据库字段）。
3. 是否执行过 `refresh` / `refresh-one`。
4. 当前账号是否有 `tool:invoke`。
5. 是否被 `allowedToolKeys` 过滤。
6. 是否被模型/会话/版本绑定规则排除。
7. 是否触发高风险审批且未通过。
8. 查看 `agent_run` / `workflow_run` / `approval_request`。
9. 流式异常时先看代理缓冲（如 Nginx `proxy_buffering`）。
10. 前端是否因整段重渲染导致“看起来不像流式”。
11. 思考内容混入正文时，检查是否启用了 `thinking/message` 事件分流。

## 10. 常见误区

1. 误区：保存配置就等于连上。
- 实际：保存只是落库，必须刷新才会建运行时连接。

2. 误区：`enabled=true` 就一定可用。
- 实际：`enabled` 是配置态，`running` 才是运行态。

3. 误区：禁用只影响新请求，老连接还会保留。
- 实际：当前实现是禁用即注销。

4. 误区：看到工具名就一定能调用。
- 实际：还会经过权限、allowlist、绑定、审批等治理层。

## 11. 推荐阅读路径（分层）

### 11.1 先跑通主链路（必读）
1. `ai-mcp-knowledge-web/src/views/mcp/index.vue`
2. `ai-mcp-knowledge-trigger/src/main/java/.../McpServerConfigController.java`
3. `ai-mcp-knowledge-application/src/main/java/.../McpServerConfigAppServiceImpl.java`
4. `ai-mcp-knowledge-infrastructure/src/main/java/.../McpServerRuntimeServiceImpl.java`
5. `ai-mcp-knowledge-infrastructure/src/main/java/.../DynamicMcpToolCallbackProvider.java`
6. `ai-mcp-knowledge-infrastructure/src/main/java/.../CompositeToolCallbackProvider.java`

### 11.2 再看聊天注入末端
1. `ai-mcp-knowledge-application/src/main/java/.../AiChatAppServiceImpl.java`
2. `ai-mcp-knowledge-application/src/main/java/com/xbk/knowledge/application/service/armory/node/AiClientToolNode.java`
3. `ai-mcp-knowledge-application/src/main/java/com/xbk/knowledge/application/service/armory/node/AiClientNode.java`

### 11.3 最后看治理层细节
1. `ai-mcp-knowledge-infrastructure/src/main/java/.../GatewayToolCallbackProvider.java`
2. `GatewayToolBindingContextHolder` 相关调用点
3. 审批与审计相关实现

## 12. 数据库映射速查

### 12.1 MCP Server 动态接入
- `ai_mcp_server_config`

### 12.2 Gateway 工具治理
- `mcp_gateway`
- `mcp_gateway_auth`
- `mcp_tool_registry`
- `mcp_tool_mapping`
- `mcp_tool_schema`
- `mcp_tool_binding`

### 12.3 审批与运行态
- `approval_request`
- `agent_run` / `workflow_run`
- `agent_run_context` / `workflow_run_context`

## 13. MCP 协议最小必知（项目映射版）

这一章只讲你在本项目开发必须知道的最小集合。

### 13.1 协议消息模型（JSON-RPC 2.0）
- 消息基类：`McpSchemaVO.JSONRPCMessage`
- 三种消息：
1. `JSONRPCRequest`：有 `method + id`，必须返回响应。
2. `JSONRPCNotification`：只有 `method`，不要求响应。
3. `JSONRPCResponse`：服务端返回，包含 `result` 或 `error`。

在本项目里的解析入口：
- `McpSchemaVO.deserializeJsonRpcMessage(...)`

### 13.2 对外 MCP 入口（SSE + message）
对应控制器：`McpGatewayController`

1. `GET /api/gateway/{gatewayId}/mcp/sse`
- 建立 SSE 长连接。
- 首包会推 `event=endpoint`，告诉客户端后续要往哪个 `message` 地址发 JSON-RPC。
- 同时每 30 秒发 `ping` 心跳。

2. `POST /api/gateway/{gatewayId}/mcp/message?sessionId=...`
- 接收 JSON-RPC 请求体。
- 路由到 `GatewayMessageService.process(...)`。
- 结果通过 `GatewaySessionService.publishResponse(...)` 回推到 SSE（`event=message`）。

### 13.3 当前支持的 3 个 MCP method
方法映射定义在 `SessionMessageHandlerMethodEnum`：

1. `initialize`
- 处理器：`GatewayInitializeHandler`
- 作用：返回 `protocolVersion`、`capabilities`、`serverInfo`、`instructions`。

2. `tools/list`
- 处理器：`GatewayToolsListHandler`
- 作用：返回可用工具清单（`name/description/inputSchema`）。

3. `tools/call`
- 处理器：`GatewayToolsCallHandler`
- 作用：执行工具调用并返回结果。
- 参数校验失败会返回 JSON-RPC 错误码 `-32602`。
- 工具执行失败会返回 JSON-RPC 错误码 `-32603`（带业务错误码）。

### 13.4 与项目内部服务的对应关系
1. 协议层：`McpGatewayController` + `GatewayMessageService` + 各 Handler  
2. 领域能力层：`GatewayToolService`  
3. 运行时工具注入层：`DynamicMcpToolCallbackProvider` / `GatewayToolCallbackProvider` / `CompositeToolCallbackProvider`  
4. 模型执行层：`AiChatAppServiceImpl` -> armory nodes -> `defaultToolCallbacks(...)`

### 13.5 不同协议的配置参数示例（可直接套用）
下面示例以 `POST /api/mcp/servers/create` 请求体为例（更新时改成 `/update` 并补 `id`）。

通用字段说明：
- `serverName`：配置名称。
- `serverType`：协议类型（`STDIO/HTTP/SSE/WEBSOCKET`）。
- `enabled`：是否启用（启用后仍需手动 refresh 才会建连）。
- `connectTimeoutMs/requestTimeoutMs/initTimeoutMs`：超时配置（默认分别是 `10000/30000/60000` ms）。

1. `STDIO` 示例（本地进程）
```json
{
  "serverName": "filesystem-stdio",
  "serverType": "STDIO",
  "enabled": true,
  "description": "本地文件系统 MCP",
  "command": "npx",
  "args": [
    "-y",
    "@modelcontextprotocol/server-filesystem",
    "/Users/sxie/workspace"
  ],
  "env": {
    "NODE_ENV": "production"
  },
  "connectTimeoutMs": 10000,
  "requestTimeoutMs": 30000,
  "initTimeoutMs": 60000
}
```
必填核心参数：`command`。

2. `SSE` 示例（远程 SSE）
```json
{
  "serverName": "remote-sse-server",
  "serverType": "SSE",
  "enabled": true,
  "description": "远程 SSE MCP 服务",
  "endpoint": "http://127.0.0.1:8080",
  "sseEndpoint": "/sse",
  "headers": {
    "Authorization": "Bearer your-token",
    "X-Tenant-Id": "demo"
  },
  "connectTimeoutMs": 10000,
  "requestTimeoutMs": 30000,
  "initTimeoutMs": 60000
}
```
必填核心参数：`endpoint`；`sseEndpoint` 可选（不填则使用默认路径）。

3. `HTTP` 示例（Streamable HTTP）
```json
{
  "serverName": "remote-http-server",
  "serverType": "HTTP",
  "enabled": true,
  "description": "远程 HTTP MCP 服务",
  "endpoint": "http://127.0.0.1:18045/v1/messages?version=2026-01-01",
  "headers": {
    "Authorization": "Bearer your-token"
  },
  "connectTimeoutMs": 12000,
  "requestTimeoutMs": 45000,
  "initTimeoutMs": 60000
}
```
必填核心参数：`endpoint`。  
说明：当前实现会把 `endpoint` 自动拆成 `baseUri + endpointPath` 来构建传输层。

4. `WEBSOCKET` 示例（预留）
```json
{
  "serverName": "remote-ws-server",
  "serverType": "WEBSOCKET",
  "enabled": true,
  "description": "预留 WebSocket MCP 服务",
  "endpoint": "ws://127.0.0.1:9000/mcp"
}
```
当前版本说明：运行时策略会直接抛错 `当前版本暂不支持 WEBSOCKET 类型`，仅保留配置语义，暂不建议在线上启用。

## 14. 五个真实故障复盘（按排障顺序）

### 14.1 故障 A：`enabled=true` 但 `running=false`
现象：
- 配置页面显示已启用，但运行状态不在线。

最常见原因：
1. 没有执行 `refresh`/`refresh-one`。
2. 执行了刷新，但 `client.initialize()` 失败（超时、endpoint 错误、鉴权问题）。

定位顺序：
1. 看 `McpServerConfigController.refreshConfigs/refreshConfig` 是否被触发。
2. 看 `McpServerRuntimeServiceImpl.registerOrUpdateInternal` 是否执行到 `client.initialize()`。
3. 看是否执行到 `clientRegistry.put(...)` 和 `refreshToolCallbacks()`。

### 14.2 故障 B：`tools/list` 为空
现象：
- SSE 连上了，但拿不到工具列表。

最常见原因：
1. Gateway 下没有启用工具定义。
2. 网关鉴权未通过（API Key 缺失/无效/过期/限流）。
3. 运行时连接未成功注册到 provider。

定位顺序：
1. 看 `GatewaySessionService.validateApiKey(...)` 是否放行。
2. 看 `GatewayToolsListHandler.handle(...)` 的 definitions 是否为空。
3. 看 `DynamicMcpToolCallbackProvider.getToolCallbacks()` 或 `GatewayToolCallbackProvider.getToolCallbacks()` 返回数量。

### 14.3 故障 C：工具看得见但调用报无权限
现象：
- 模型能看到工具名，但调用时返回 `[PERMISSION_DENIED]`。

根因：
- 当前登录上下文缺少 `tool:invoke`，且未开启 bypass。

关键实现点：
- `DynamicMcpToolCallbackProvider` 与 `GatewayToolCallbackProvider` 都会做 `tool:invoke` 校验。

定位顺序：
1. 检查账号权限是否包含 `tool:invoke`。
2. 检查调用链是否在预期上下文（是否匿名/system 调用）。
3. 查看审计和计数是否记录 `DENIED`。

### 14.4 故障 D：工具调用被审批门禁拦截
现象：
- 工具被触发后中断，提示“工具调用需要审批”。

根因：
- 风险级别 `HIGH`，触发 `ApprovalRequiredException`。

定位顺序：
1. 检查工具风险级别（Gateway 工具来自 `risk_level`；MCP 动态工具当前默认 `MEDIUM`）。
2. 查看是否存在 `PENDING` 审批单。
3. 通过审批后重试，确认是否命中 `findLatestApproved(...)` 放行分支。

### 14.5 故障 E：前端看起来“不流式”
现象：
- 最终一次性蹦出整段文本，或思考内容和正文混在一起。

最常见原因：
1. 代理层缓冲未关闭。
2. 前端把所有分片当同一类事件处理。
3. 后端事件分流未按 `thinking/message` 处理。

定位顺序：
1. 确认 `AICallController.stream` 设置了 `X-Accel-Buffering=no`。
2. 确认 `sendChunk(...)` 中 thinking 分片走 `event=thinking`，正文走默认 `message`。
3. 前端分别处理 `thinking` 与 `message` 事件并分区渲染。

## 15. 动手实验清单 + 掌握度自测

### 15.1 动手实验（建议按顺序）
1. 实验 1：手工新增一个 `SSE` MCP 配置并成功刷新到 `running=true`。
2. 实验 2：验证 `tools/list` 能返回至少一个工具，并说出来源 provider。
3. 实验 3：故意传错 API Key，观察并定位失败点（鉴权分支）。
4. 实验 4：构造一次工具权限不足场景，验证 `tool:invoke` 门禁。
5. 实验 5：触发一次高风险审批链路（若环境有 HIGH 工具），观察审批前后差异。
6. 实验 6：走一遍流式对话，验证 `thinking/message/usage` 三类事件都能正确处理。

### 15.2 推荐验证入口（优先自动化）
可优先参考或运行：
- `McpGatewaySseToolsListTest`

该集成测试覆盖：
1. SSE 建连与 `tools/list`。
2. 通过模型触发 `tools/call` 的全链路验证。

### 15.3 自测打分（100 分）
1. 协议理解（20 分）：
- 你能解释 `initialize/tools/list/tools/call` 在本项目各自落在哪个类。

2. 运行时理解（30 分）：
- 你能画出三本 registry 在 refresh 前后的变化。
- 你能解释为什么快照相同会跳过重连。

3. 治理理解（25 分）：
- 你能说明权限、allowlist、绑定、审批四层谁先谁后。

4. 排障能力（25 分）：
- 给你一个“能看到工具但调用失败”的问题，你能在 10 分钟内定位到具体层级。

达标建议：
- 80 分以上：可独立承担常规 MCP 接入与排障。
- 60-79 分：可在评审支持下完成开发。
- 60 分以下：建议先补做第 15.1 的全部实验。

## 16. 一句话总结

把主线记成四段就够了：
- 配置落库 -> 运行时建连 -> Provider 合并 -> ChatClient 注入。
