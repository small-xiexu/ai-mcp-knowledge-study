# AI MCP Knowledge Study

<div align="center">

**基于 DDD 架构的企业级 AI 中台（单组织版）**

[![JDK](https://img.shields.io/badge/JDK-17-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.2-blue.svg)](https://spring.io/projects/spring-ai)

</div>

## 项目定位
`ai-mcp-knowledge-study` 是一个面向企业内部的 AI 中台项目，覆盖了从模型配置、Agent/Workflow 编排、MCP 工具接入、RAG 知识库、审批治理到审计与监控的完整链路。

当前版本已完成以下架构收敛：
- 单组织模式（已去多租户语义）
- 去除模型策略链/TaskType 能力（不再使用 strategy/taskType）
- ChatClient 采用自研 Armory 节点装配链
- 初始化 SQL 为一体化重建脚本（无外键，应用层负责级联清理）

## 核心能力
### 1. 身份与权限治理
- 用户管理、角色管理、权限分配
- 登录鉴权（Sa-Token）
- 身份审计事件查询

### 2. LLM 模型中心
- 模型增删改查、启停、连通性测试
- 激活对话模型/嵌入模型
- 支持 `completions_path`、`embeddings_path` 直连协议路径

### 3. AI 对话与会话
- 同步对话接口
- SSE 流式对话接口
- 会话管理与消息历史
- 会话绑定模型一致性控制

### 4. Agent 平台
- Agent 管理
- Agent 调用通道（`channel`）配置
- Agent 版本草稿/发布/回滚
- Agent 版本三种运行形态：`PROMPT` / `CHAIN` / `WORKFLOW`
- Agent 运行（chat/stream/invoke）
- Agent 调度（定时执行）
- Agent 删除级联清理（无外键场景下由应用层执行）

### 5. Client Profile（Client 资产层）
- Client Profile 资产管理（编码、状态、描述）
- 多步骤链路配置（模型、系统提示词、工具开关、工具白名单）
- AgentVersion 可直接引用 `client_profile_id`，并优先于 `client_chain_json`

### 6. Workflow 编排执行（自研运行时）
- Workflow 与版本管理
- 节点图保存与运行
- 运行记录与节点运行记录查询
- 审批中断后恢复执行

已支持节点类型：
- `START` / `END` / `PARALLEL` / `JOIN`
- `RAG_RETRIEVE`
- `IF`
- `TOOL_CALL`
- `LLM` / `OUTPUT`

### 7. Prompt 与 Advisor
- Prompt 模板管理（草稿、发布、归档）
- Advisor 配置与绑定（Agent/Workflow 版本）

### 8. MCP 与工具治理
- MCP Server 配置与刷新（STDIO/HTTP/SSE/WEBSOCKET）
- Gateway 实例、鉴权、工具注册、映射、Schema、模型工具绑定
- 工具审批流程（待审批/通过/拒绝）

### 9. RAG 知识库
- 文档上传（同步/异步）
- Git 仓库分析入库
- 标签检索、任务进度、取消、重试、清理

### 10. 监控与运维
- 指标统计：调用量、成功率、响应时间、模型使用分布
- 工作台聚合看板
- 预热能力（AgentVersion/WorkflowVersion）
- 定时治理任务（审批过期、聊天清理、RAG 任务治理、Workflow 运行清理等）
- XXL-Job 管理接口

## MCP 网关对外协议能力
项目不仅有管理页面，还提供对外 MCP 协议入口：
- SSE 建连：`GET /api/gateway/{gatewayId}/mcp/sse`
- JSON-RPC 消息：`POST /api/gateway/{gatewayId}/mcp/message?sessionId=...`

可用于外部 MCP 客户端接入（如支持 MCP 的 IDE/Agent 客户端）。

## 架构总览
### 分层（DDD）
- `ai-mcp-knowledge-trigger`：HTTP 接口适配层
- `ai-mcp-knowledge-application`：用例编排层
- `ai-mcp-knowledge-domain`：领域模型与规则
- `ai-mcp-knowledge-infrastructure`：仓储与外部技术适配
- `ai-mcp-knowledge-types`：共享类型
- `ai-mcp-knowledge-api`：对外 DTO 契约
- `ai-mcp-knowledge-app`：启动与配置
- `ai-mcp-knowledge-web`：前端管理台

### 核心调用链（对话）
`Controller -> AiChatAppService -> ChatClientAssemblyService -> Armory Node Chain -> Model/Tool/Advisor -> 返回契约 + 记日志`

## 主要页面（前端路由）
参考 `ai-mcp-knowledge-web/src/router/routes.ts`，当前包含：
- 工作台、监控看板、AI 对话
- LLM 配置、Advisor 配置、MCP 配置、网关工具、凭证管理
- Client 配置（Client Profile）
- Agent 管理、Agent 版本、Agent 调度、Agent 调用
- Workflow 管理、版本、编辑器、调用、运行记录、运行详情
- Prompt 模板、工具审批
- 知识库管理、导入任务
- 用户管理、角色管理、身份审计
- XXL 任务中心

## 主要接口分组（后端）
参考 `ai-mcp-knowledge-trigger/src/main/java/com/xbk/knowledge/trigger/http`：
- `/api/auth`：登录与会话
- `/api/users` `/api/roles` `/api/permissions`：身份权限
- `/api/models`：模型中心
- `/api/ai` `/api/ai/sessions`：AI 对话与会话
- `/api/agents` `/api/agent-versions` `/api/agents/{agentCode}` `/api/schedules`：Agent 体系
- `/api/client-profiles`：Client Profile 资产
- `/api/workflows`：Workflow 管理与运行
- `/api/templates` `/api/advisors` `/api/approvals`：Prompt/Advisor/审批
- `/api/mcp/servers` `/api/gateway/manage` `/api/gateway` `/api/mcp/tools`：MCP 与网关
- `/api/ai/rag`：RAG 能力
- `/api/metrics` `/api/workbench`：监控与工作台
- `/api/xxl`：任务中心

## 数据库设计概览
初始化脚本：`sql/init-ai-model-orchestration.sql`

当前为一体化重建脚本，包含 37 张表：
- 身份与审计：6 张
- 模型/对话/RAG：7 张
- Gateway/工具：6 张
- Client/Advisor/Agent/Prompt/调度/运行：10 张
- Workflow/运行：7 张
- 审批：1 张

说明：
- 脚本为“重建导向”，会先 `DROP TABLE IF EXISTS` 再建表
- 无外键约束，由应用层实现级联清理

## 快速开始
### 环境要求
- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6+
- Node.js 18+（前端）
- PostgreSQL + pgvector（启用 RAG 向量检索建议准备）

### 1) 初始化数据库
```bash
mysql -uroot -proot < sql/init-ai-model-orchestration.sql
```

### 2) 配置后端
编辑文件：
- `ai-mcp-knowledge-app/src/main/resources/application-dev.yml`
- `ai-mcp-knowledge-app/src/main/resources/application.yml`

重点确认：
- MySQL：`spring.datasource.mysql.*`
- Redis：`spring.data.redis.*`
- pgvector：`spring.datasource.pgvector.*`（RAG）
- XXL：admin 地址与 token（如使用）

### 3) 启动后端
```bash
mvn -DskipTests compile
mvn -pl ai-mcp-knowledge-app spring-boot:run
```

默认端口：
- 后端：`http://localhost:8090`

### 4) 启动前端
```bash
cd ai-mcp-knowledge-web
npm install
npm run dev
```

默认端口：
- 前端：`http://localhost:5173`

### 5) 默认管理员
初始化 SQL 会写入默认管理员（仅用于本地开发）：
- 用户名：`admin`
- 密码：`123456`

## 当前版本边界说明
- 已移除多租户/`org` 业务边界，按单组织运行。
- 已移除模型策略链、TaskType 映射、fallback 策略实现。
- 对话主链路以 `AiChatAppService` + Armory 组装为准。

## 开发常用命令
```bash
# 编译
mvn -DskipTests compile

# 单模块编译（示例）
mvn -pl ai-mcp-knowledge-app -DskipTests compile

# 前端构建
cd ai-mcp-knowledge-web && npm run build
```

## 参考文档
- `docs/任务编排入门指南.md`
- `docs/Spring-AI任务编排落地方案综合对比.md`
- `docs/RAG文档处理优化完整方案.md`
- `.codex/DDD.md`

---

如果你要继续做“对标 ai-agent-station 的下一步”，建议优先顺序：
1. Workflow 节点能力继续扩展（并行聚合、异常补偿、重试策略）
2. MCP 网关的配额与熔断治理加强
3. RAG 分层存储与检索策略优化（冷热分层 + 标签治理）
