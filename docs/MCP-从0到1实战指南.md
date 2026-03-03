# MCP 从 0 到 1 实战指南（项目版）

> 目标：把本项目里 MCP 相关代码串成一条清晰主线，做到“知道去哪看、为什么这样设计、出问题如何排查”。
> 范围：MCP Server 动态接入、Gateway 工具、ToolCallbackProvider 合并、ChatClient 工具注入、治理链路。

## 0. 如何使用这篇文档

- 你是第一次接触项目：先看第 1-5 章，再看第 8 章实战。
- 你在排障：直接跳第 9 章排障清单，再回看第 4、5 章。
- 你要读代码：直接按第 11 章的顺序读。

### 0.1 开跑前 5 项检查（先过这个再学）
1. 服务可访问
- 后端和前端都已启动，页面能正常打开。

2. 权限准备好
- 至少有 `tool:read`、`tool:write`；要做模型侧调用最好也有 `tool:invoke`。

3. 目标地址准备好
- Dynamic：你至少有一套可连接目标（`STDIO` 命令或远程 `SSE/HTTP` endpoint）。
- Gateway：你至少有 `gatewayId`，若启用鉴权还要有 `API Key`。

4. 先跑最小链路
- Dynamic 先看 [MCP Dynamic 10 分钟最小可跑通实操](./MCP-Dynamic-10分钟最小可跑通实操.md)。
- Gateway 先看 [MCP Gateway 10 分钟最小可跑通实操](./MCP-Gateway-10分钟最小可跑通实操.md)。

5. 知道成功标准
- Dynamic：配置启用 + 刷新后 `running=true`，并且工具清单可查询。
- Gateway：能依次打通 `sse -> initialize -> tools/list -> tools/call`。

### 0.2 两大块阅读地图（新版）
本文已按两大块组织阅读：

1. Dynamic MCP（配置建连与运行时工具注入）
- 重点章节：第 1-8 章、11.1、12.1、14.1。

2. Gateway MCP（协议入口、HTTP 工具与治理）
- 重点章节：第 2.2-2.3、3.3.2、11.3、12.2、13 章、14.2-14.4。

建议顺序：
1. 先吃透 Dynamic 主链路（配置 -> 建连 -> ToolCallback 注入）。
2. 再看 Gateway 协议链路（SSE/message -> tools/list -> tools/call）。
3. 最后做综合排障（第 9、14、15 章）。

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

## 第一大块：Dynamic MCP（配置建连与运行时注入）

急用建议：
- 先看 [MCP Dynamic 10 分钟最小可跑通实操](./MCP-Dynamic-10分钟最小可跑通实操.md)，跑通后再回来看本块原理最省时间。

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

### 3.3.4（进阶，可跳过）`getToolCallbacks` 启动时序排障结论
首次阅读建议：
- 这一节是“启动时序排障专题”，不是主链路必读。
- 你如果目标只是先跑通 Dynamic，可先跳到 `3.4`，后面排查启动问题再回看。

排障结论（2026-02-27）：
- 历史上出现“`getToolCallbacks` 早于 `ApplicationRunner`”并不一定是 `McpServerRuntimeBootstrap` 导致。
- 真实提前触发点是 Spring AI 的 `ToolCallingAutoConfiguration.toolCallbackResolver(...)`：
  在创建 `ToolCallbackResolver` Bean 时，会遍历 `ToolCallbackProvider` 并调用 `getToolCallbacks()`。

触发链路拆分：
1. 框架链路（可能早于 `ApplicationRunner`）
   `ToolCallingAutoConfiguration.toolCallbackResolver` -> 遍历 `ToolCallbackProvider` -> `getToolCallbacks()`
2. 业务链路（`ApplicationRunner` 之后）
   `McpServerRuntimeBootstrap.run` -> `refreshEnabledServers` -> `refreshToolCallbacks` -> `updateClients` -> 预热 `getToolCallbacks()`

本项目修正方式：
1. 增加 `LazyToolCallbackResolverConfig`，用 `@Primary` 覆盖默认 resolver。
2. 将“启动时全量展开 provider”改为“按工具名惰性解析时再访问 provider”。
3. 修正后观测：`getToolCallbacks` 不再早于 `ApplicationRunner`，启动时序与预期一致。

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
- `McpServerRuntimeServiceImpl.buildClient`：仅负责按 `McpServerType` 路由策略（不再内置各协议构建细节）
- `McpClientBuildStrategyFactory.getStrategy`：按 `McpClientBuildStrategyType` 获取策略实现
- 协议策略实现（重点看 `build`）
- `StdioMcpClientBuildStrategy`
- `SseMcpClientBuildStrategy`
- `HttpMcpClientBuildStrategy`
- `WebsocketMcpClientBuildStrategy`
- 统一公共能力在 `AbstractMcpClientBuildStrategy`
- `buildSyncClient`
- `applyHeaders`
- `parseStringList` / `parseStringMap`
- `getTimeout`

### 5.4 最后看辅助函数
- `closeQuietly`
- `refreshToolCallbacks`
- `buildRuntimeConfigSnapshot`

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

### 6.3 什么是“工具回调数组”（`ToolCallback[]`）
先说结论：
- 它就是“当前这次可用的工具清单”，只是用 Java 数组来承载。

大白话理解：
1. `ToolCallback[]` 里每个元素都代表一个可调用工具。
2. 每个 `ToolCallback` 都带有工具定义（`ToolDefinition`），例如工具名、描述、入参 schema，用来告诉模型“这工具是啥、怎么传参”。
3. 每个 `ToolCallback` 还带有执行入口（`call(...)`），模型一旦决定调用工具，框架就会触发这里。
4. 本项目里，这个数组通常来自 `CompositeToolCallbackProvider`，即合并 `Dynamic MCP` 和 `Gateway` 两路工具，再做去重/过滤后返回。

一句话：
- 这不是业务数据数组，而是“可调用能力数组”。

### 6.4 为什么叫 Callback（回调）
核心原因：
- 工具实现是先注册给框架，运行时再由框架反向调用，所以叫 callback。

在本项目中的语义：
1. 业务先把工具能力提供给 `ToolCallbackProvider`。
2. 当模型在推理时决定要调用某个工具，Spring AI 才会回过头触发对应 `ToolCallback` 的执行入口。
3. 因此它不是普通 DTO，也不是静态配置，而是“运行时可执行句柄”。

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

### 8.3 例子 C：接入 `mcp-tool-weixin`（HTTP 服务）
先说结论：
- `mcp-tool-weixin` 是 HTTP 服务，不是标准 MCP Server。
- 应该走「HTTP 工具配置（Gateway）」接入，不走「MCP 工具配置（Dynamic）」。

推荐配置值：
1. 工具名：`sendWeixinNotice`
2. 工具描述：`微信公众号回调工具`
3. 方法：`POST`
4. URL：`http://127.0.0.1:8104/api/weixin/notice/send`
5. 请求参数：`platform/subject/description/jumpUrl`（都放 `body`）

字段语义建议（写给人和 AI 一起看）：
1. `platform`：发送来源平台/系统名。
2. `subject`：消息标题。
3. `description`：消息正文（主要内容）。
4. `jumpUrl`：点击后跳转地址（完整 URL）。

说明：
- AI 会综合工具名、工具描述、字段语义来决定“要不要调用”以及“参数怎么填”。
- 所以字段含义写清楚，实际效果会明显更稳定。

完整操作步骤见：
- [MCP Gateway 10 分钟最小可跑通实操](./MCP-Gateway-10分钟最小可跑通实操.md) 第 7 节「接入 mcp-tool-weixin（HTTP 工具）」。

## 第二大块：Gateway MCP（协议入口与工具治理）

急用建议：
- 先看 [MCP Gateway 10 分钟最小可跑通实操](./MCP-Gateway-10分钟最小可跑通实操.md)，跑通后再回来看本章原理会轻松很多。

### Gateway 在 DDD 架构中的位置（先看懂这张图）

```mermaid
flowchart TD
    subgraph Trigger["Trigger 层"]
        direction TB
        A1["McpGatewayController<br/>• establishSseConnection<br/>• handleMessage"]
        A2["GatewaySessionService<br/>会话管理 + API Key 鉴权"]
        A3["GatewayMessageService<br/>消息路由 method → Handler"]
        A4["Handler 实现<br/>• GatewayInitializeHandler<br/>• GatewayToolsListHandler<br/>• GatewayToolsCallHandler"]
    end

    subgraph Domain["Domain 层"]
        direction TB
        B1["GatewayToolService 接口<br/>• listTools<br/>• callTool<br/>• initialize"]
        B2["实体 Entity<br/>• McpGateway<br/>• McpGatewayAuth<br/>• McpToolRegistry<br/>• McpToolMapping<br/>• McpToolSchema<br/>• McpToolBinding"]
        B3["仓储接口 Repository"]
    end

    subgraph Infra["Infrastructure 层"]
        direction TB
        C1["GatewayToolServiceImpl<br/>• WebClient HTTP 调用<br/>• 参数映射<br/>• 响应提取<br/>• 指标记录"]
        C2["Repository 实现<br/>MyBatis-Plus 实现"]
    end

    subgraph DB["Database 层"]
        direction TB
        D1["MySQL 表<br/>• mcp_gateway<br/>• mcp_gateway_auth<br/>• mcp_tool_registry<br/>• mcp_tool_mapping<br/>• mcp_tool_schema<br/>• mcp_tool_binding"]
    end

    Trigger -->|调用 GatewayToolService 接口 | Domain
    Domain -->|调用 Repository 实现 | Infra
    Infra -->|数据持久化 | DB
```

### Gateway 与项目其他模块的集成点

```mermaid
flowchart TD
    A["Sa-Token 权限系统"] -->|@SaCheckPermission| B1["McpGatewayController<br/>对外 MCP 协议入口"]
    A -->|@SaCheckPermission| B2["GatewayManageController<br/>管理态配置入口"]
    B1 -->|调用 | C["GatewayToolService"]
    B2 -->|调用 | C
    C -->|调用 | D["GatewayToolCallbackProvider<br/>将 Gateway 工具转为 ToolCallback"]
    D -->|合并 | E["CompositeToolCallbackProvider @Primary<br/>合并 Dynamic MCP + Gateway 两路工具"]
    E -->|注入 | F["ChatClient 装配链<br/>AiChatAppServiceImpl → armory nodes<br/>defaultToolCallbacks"]
```

### Gateway MCP 学习路径（小白友好版）
先说人话：Gateway MCP 就像“工具接待前台”。
- 外部客户端先来前台报到（`sse` 建连）。
- 然后按前台给的地址发请求（`message`）。
- 前台把请求转给对应处理器（`initialize/tools/list/tools/call`）。
- 处理完再把结果推回客户端（SSE `message` 事件）。

先记住这 3 句话，再看代码就不容易晕：
1. `sse` 是“先打通电话线”（长连接）。
2. `message` 是“在线路上说话”（发 JSON-RPC 消息）。
3. `sessionId` 是“这通电话的编号”（保证回复推回正确连接）。

### 一张图看懂全流程（总览）
```mermaid
flowchart LR
    A[外部 MCP 客户端] -->|GET /api/gateway/:gatewayId/mcp/sse| B[McpGatewayController]
    B --> C[GatewaySessionService<br/>建会话+鉴权]
    C --> D[SSE 推送 endpoint + ping]
    A -->|POST /api/gateway/:gatewayId/mcp/message?sessionId=...| B
    B --> E[GatewayMessageService]
    E --> F{按 method 路由}
    F -->|initialize| G[GatewayInitializeHandler]
    F -->|tools/list| H[GatewayToolsListHandler]
    F -->|tools/call| I[GatewayToolsCallHandler]
    I --> J[GatewayToolServiceImpl.callTool]
    J --> K[调用下游 HTTP API]
    G --> L[GatewaySessionService.publishResponse]
    H --> L
    I --> L
    L -->|event=message| A
```

### 一次 `tools/call` 的时序图（你最需要先看这个）
```mermaid
sequenceDiagram
    participant C as Client
    participant GC as McpGatewayController
    participant SS as GatewaySessionService
    participant MS as GatewayMessageService
    participant H as GatewayToolsCallHandler
    participant TS as GatewayToolServiceImpl
    participant API as 下游HTTP接口

    C->>GC: GET /api/gateway/:gatewayId/mcp/sse
    GC->>SS: establishSseConnection()
    SS-->>C: endpoint + ping
    C->>GC: POST /api/gateway/:gatewayId/mcp/message?sessionId=xxx (tools/call)
    GC->>MS: process()
    MS->>H: handle(request)
    H->>TS: callTool(name, arguments)
    TS->>API: HTTP 调用
    API-->>TS: 响应
    TS-->>H: 工具结果/错误
    H-->>MS: JSON-RPC Response
    MS->>SS: publishResponse(sessionId, response)
    SS-->>C: SSE event=message
```

### 难词翻译（大白话版）
| 术语 | 大白话解释 |
| --- | --- |
| SSE | 服务端一直开着一根“推消息的网线” |
| JSON-RPC | 一种固定格式的“远程函数调用信封” |
| initialize | 第一次握手，告诉客户端“我是谁、我会什么” |
| tools/list | 问“你现在有哪些工具可用” |
| tools/call | 真的发起一次工具调用 |
| handler | 每种 method 对应的“专门处理员” |
| sessionId | 当前连接的唯一编号，防止串线 |

### 从 0 到 1 的学习顺序（照着做就行）
阶段 1：先跑通连接（30 分钟）
1. 先看 `McpGatewayController` 的两个入口：
- `GET /api/gateway/{gatewayId}/mcp/sse`
- `POST /api/gateway/{gatewayId}/mcp/message?sessionId=...`
2. 再看 `GatewaySessionService`：
- `validateGateway` / `validateApiKey`
- `establishSseConnection`（发 `endpoint` 和 `ping`）
- `publishResponse`（把响应推回客户端）

阶段 1 通过标准：
1. 你能说清：为什么先调 `sse`，再调 `message`。
2. 你能说清：`sessionId` 不传会发生什么（服务端找不到回推会话）。

阶段 2：看协议方法路由（45 分钟）
1. `GatewayMessageService`：method 路由总入口。
2. `SessionMessageHandlerMethodEnum`：method 和处理器的映射表。
3. 三个核心处理器：
- `GatewayInitializeHandler`
- `GatewayToolsListHandler`
- `GatewayToolsCallHandler`

阶段 2 通过标准：
1. 你能快速回答 `initialize/tools/list/tools/call` 走哪个类。
2. 你知道常见错误码：参数错 `-32602`、执行失败 `-32603`。

阶段 3：看真实工具调用（60 分钟）
1. `GatewayToolServiceImpl.listTools`：工具清单和 `inputSchema` 从哪里来。
2. `GatewayToolServiceImpl.callTool`：参数映射 -> HTTP 调用 -> 响应提取 -> 指标记录。
3. 重点表：
- `mcp_tool_mapping`（请求/响应字段映射）
- `mcp_tool_schema`（工具入参 schema 缓存）

阶段 3 通过标准：
1. 你能解释为什么 `tools/list` 会带 `inputSchema`。
2. 你能区分失败来源：参数问题、超时、下游接口异常。

阶段 4：接到模型工具链（45 分钟）
1. `GatewayToolCallbackProvider`：把 Gateway 工具转成 `ToolCallback`。
2. `applyVisibilityFilter`：可见性过滤（allowlist + MODEL/SESSION/AGENT_VERSION 绑定）。
3. `CompositeToolCallbackProvider`：和 Dynamic MCP 合并并按工具名去重。
4. `AiChatAppServiceImpl` + armory node：注入 `defaultToolCallbacks(...)`。

阶段 4 通过标准：
1. 你能排查“管理页看得到，模型却看不到”的原因。
2. 你能定位同名工具冲突是谁被保留（看合并顺序 + 去重规则）。

推荐断点（最实用）
1. `GatewaySessionService.establishSseConnection`
2. `GatewayMessageService.process`
3. `GatewayToolsListHandler.handle`
4. `GatewayToolsCallHandler.handle`
5. `GatewayToolServiceImpl.callTool`
6. `GatewayToolCallbackProvider.getToolCallbacks`

推荐自测动作（按顺序）
1. 先调一次 `sse`，确认收到 `endpoint` 和 `ping`。
2. 再发一次 `initialize`，确认握手响应字段完整。
3. 再发 `tools/list`，确认能看到至少一个工具。
4. 最后发 `tools/call`，验证成功路径和失败路径各一条。
5. 进入聊天调用，验证 Gateway 工具是否进入模型可见集合。

## 9. 快速排障清单（小白版：现象 -> 原因 -> 解决）

先用一句话记住排障顺序：先看“有没有连接”，再看“有没有权限”，最后看“有没有被治理规则拦截”。
说明：
- 序号 1-2 主要对应 Dynamic MCP 运行态（`ai_mcp_server_config` + runtime refresh）。
- 序号 3-11 主要对应 Gateway 协议入口与工具治理链路。

| 序号 | 你看到的现象 | 最常见原因 | 先怎么验证 | 解决动作 |
| --- | --- | --- | --- | --- |
| 1 | 管理页已启用，但工具就是不可用 | 只是 `enabled=true`，运行态没起来 | 看 `running` 是否为 true | 执行 `refresh` / `refresh-one` |
| 2 | 一直 `running=false` | 建连初始化失败（endpoint/超时/鉴权） | 查 `registerOrUpdateInternal` 日志和异常栈 | 修正 endpoint、超时、认证配置后重刷 |
| 3 | SSE 连上了但收不到工具 | `tools/list` 结果为空 | 在 `GatewayToolsListHandler.handle` 打断点看 definitions | 检查网关/工具是否启用、schema 是否存在 |
| 4 | 看得到工具名，但一调用就拒绝 | 缺少 `tool:invoke` 权限 | 看权限上下文与审计日志 | 给账号补 `tool:invoke` 或切到正确身份 |
| 5 | 某些工具在模型侧消失 | 被 `allowedToolKeys` 过滤 | 看 `applyVisibilityFilter` 输入输出数量 | 调整 allowlist 配置 |
| 6 | 管理页有工具，但模型侧没有 | 被 MODEL/SESSION/AGENT_VERSION 绑定规则排除 | 查绑定表与当前上下文是否匹配 | 补绑定或放宽绑定范围 |
| 7 | 工具触发后提示“需要审批” | 高风险工具命中审批门禁 | 查 `approval_request` 是否有 `PENDING` | 审批通过后重试，或下调风险等级 |
| 8 | `tools/call` 报参数错误 | 入参不符合 schema/映射规则 | 看 `GatewayToolsCallHandler` 返回是否 `-32602` | 修正 name/arguments 字段和类型 |
| 9 | `tools/call` 报执行失败 | 下游 HTTP 失败或超时 | 看返回是否 `-32603`，再看下游接口日志 | 修正映射、网络、下游服务状态 |
| 10 | 前端看起来“不流式” | 代理缓冲或前端整段重渲染 | 检查 Nginx `proxy_buffering` 和前端事件处理 | 关闭缓冲，按分片渲染 |
| 11 | 思考内容和正文混在一起 | 没按 `thinking/message` 分流 | 抓 SSE 事件名看是否区分 | 前后端统一事件分流策略 |

排障时建议固定 6 个断点：
1. `GatewaySessionService.establishSseConnection`
2. `GatewayMessageService.process`
3. `GatewayToolsListHandler.handle`
4. `GatewayToolsCallHandler.handle`
5. `GatewayToolServiceImpl.callTool`
6. `GatewayToolCallbackProvider.getToolCallbacks`

## 10. 常见误区

说明：
- 误区 1-3 主要是 Dynamic MCP 配置建连语境。
- 误区 4 是 Gateway 工具治理语境。

1. 误区：保存配置就等于连上。
- 实际：保存只是落库，必须刷新才会建运行时连接。

2. 误区：`enabled=true` 就一定可用。
- 实际：`enabled` 是配置态，`running` 才是运行态。

3. 误区：禁用只影响新请求，老连接还会保留。
- 实际：当前实现是禁用即注销。

4. 误区：看到工具名就一定能调用。
- 实际：还会经过权限、allowlist、绑定、审批等治理层。

## 11. 推荐阅读路径（分层）

### 11.1 先跑通主链路（Dynamic + 合并层）
1. `ai-mcp-knowledge-web/src/views/mcp/index.vue`
2. `ai-mcp-knowledge-trigger/src/main/java/.../McpServerConfigController.java`
3. `ai-mcp-knowledge-application/src/main/java/.../McpServerConfigAppServiceImpl.java`
4. `ai-mcp-knowledge-infrastructure/src/main/java/.../McpServerRuntimeServiceImpl.java`
5. `ai-mcp-knowledge-infrastructure/src/main/java/.../DynamicMcpToolCallbackProvider.java`
6. `ai-mcp-knowledge-infrastructure/src/main/java/.../CompositeToolCallbackProvider.java`

### 11.2 补齐 Gateway 管理态链路（本章新增）
1. `ai-mcp-knowledge-web/src/views/gateway/index.vue`
2. `ai-mcp-knowledge-web/src/views/gateway/tools/index.vue`
3. `ai-mcp-knowledge-trigger/src/main/java/.../GatewayManageController.java`
4. `ai-mcp-knowledge-infrastructure/src/main/java/.../GatewayToolServiceImpl.java`
5. `ai-mcp-knowledge-app/src/main/java/com/xbk/knowledge/config/tool/LazyToolCallbackResolverConfig.java`

管理态常用接口速查：
1. 网关实例：`/api/gateway/manage/instances/list|save|delete`
2. 网关凭证：`/api/gateway/manage/auth/list|save|enable|disable`
3. 工具管理：`/api/gateway/manage/tools/list|get|save|enable|disable|delete|refresh`
4. 调试调用：`/api/gateway/manage/tools/debug`
5. 模型绑定：`/api/gateway/manage/bindings/model/get|save`

和运行态的关系（最容易混淆）：
1. `GatewayManageController` 是“管理态入口”，`McpGatewayController` 是“对外 MCP 协议入口”，两者不是同一路接口。
2. Gateway 工具配置修改后，模型侧工具解析依赖 `ToolCallbackProvider` 惰性解析，不走 Dynamic MCP 的 `refresh/refresh-one`。
3. `/api/gateway/manage/tools/refresh` 是工具 HTTP 连通性刷新，不是 MCP Server 运行时重连刷新。

### 11.3 再看聊天注入末端
1. `ai-mcp-knowledge-application/src/main/java/.../AiChatAppServiceImpl.java`
2. `ai-mcp-knowledge-application/src/main/java/com/xbk/knowledge/application/service/armory/node/AiClientToolNode.java`
3. `ai-mcp-knowledge-application/src/main/java/com/xbk/knowledge/application/service/armory/node/AiClientNode.java`

### 11.4 最后看治理层细节
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

#### 13.2.1 `API Key` 大白话（很多人会误解）

一句话：`mcp_gateway_auth` 里的 `API Key` 是“外部客户端访问网关的门禁卡”。

它不是“某个 HTTP 工具单独的密钥”，而是“网关级密钥”：
- 作用对象：一个 `gatewayId`。
- 影响范围：该网关下所有 HTTP 工具调用链路（`sse -> message -> tools/call`）。

它主要控制两件事：
1. 鉴权
- key 是否存在、是否匹配、是否过期。
2. 限流
- 每个 `gatewayId + apiKey` 在分钟窗口内最多调用次数（`rateLimit`）。

当前实现（以代码为准）：
- 外部客户端建立 SSE 时，必须在 Header 携带 `X-API-Key`（不支持通过 query 传 `apiKey`）。
- 如果该网关未启用有效凭证：仅要求 Header 非空，不做 key 值匹配校验。
- 如果该网关启用了凭证：会校验 key 是否有效、是否过期、是否超限；缺失/错误/过期/超限都会被拒绝。

完整例子（运营看板系统接入 `default_gateway`）：

1. 在后台“网关凭证”页新增一条凭证
- `gatewayId = default_gateway`
- `rateLimit = 100`（每分钟 100 次）
- `expireTime = 2026-12-31T23:59:59`
- `apiKey` 可留空，让系统自动生成。

2. 外部系统用该 Key 建立 SSE

```bash
curl -N \
  -H "Accept: text/event-stream" \
  -H "X-API-Key: sk_live_xxx" \
  "http://127.0.0.1:8091/api/gateway/default_gateway/mcp/sse"
```

成功信号：
- 收到 `event: endpoint`（包含 `sessionId`）
- 持续收到 `event: ping`

3. 外部系统继续发 `message`（`initialize/tools/list/tools/call`）
- 只要会话有效且未超限，就能正常走完整链路。

4. 典型失败
- `API Key 无效`：key 填错或状态禁用。
- `API Key 已过期`：超过 `expireTime`。
- `API Key 调用频率超限`：超过 `rateLimit`。

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
