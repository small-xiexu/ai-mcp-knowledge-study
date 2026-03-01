# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

基于 DDD + Spring AI 的企业级 AI 中台（单组织版），覆盖身份权限、模型中心、AI 对话、Agent 资产与运行、Workflow 图编排、MCP Server 动态接入、Gateway HTTP 工具治理、RAG 文档处理等能力。

## 技术栈

- **后端**: Java 17, Spring Boot 3.4.3, Spring AI 1.1.2, MyBatis-Plus, Sa-Token, XXL-Job
- **前端**: Vue 3 + Vite + TypeScript, Element Plus, Pinia
- **数据库**: MySQL 8+ (业务数据), PostgreSQL + pgvector (向量存储), Redis (会话/缓存)

## 开发环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8+
- Redis 6+
- PostgreSQL + pgvector (RAG 场景)
- Node.js 18+

## 构建与运行命令

### 后端

```bash
# 全量编译
mvn -DskipTests compile

# 单模块编译
mvn -pl ai-mcp-knowledge-app -DskipTests compile

# 启动后端 (默认端口 8090)
mvn -pl ai-mcp-knowledge-app spring-boot:run

# 全量测试 (排除 integration 组)
mvn test

# 运行单个测试类
mvn -pl ai-mcp-knowledge-app test -Dtest=OpenAiTest

# 代码格式化检查
mvn -Pformat-check validate
```

### 前端

```bash
cd ai-mcp-knowledge-web

# 安装依赖
npm install

# 开发模式 (端口 3000, 已代理/api -> http://localhost:8090)
npm run dev

# 类型检查
npm run type-check

# 构建生产版本
npm run build

# 代码格式化
npm run format
```

### 一键启动

```bash
# 前端快速启动脚本
./启动前端开发.sh
```

### 数据库初始化

```bash
mysql -uroot -proot < sql/init-ai-model-orchestration.sql
```

默认管理员：`admin` / `123456`

## 模块架构 (DDD 分层)

```
ai-mcp-knowledge-study (parent)
├── ai-mcp-knowledge-types           # 公共层：Result/Page、异常、枚举、trace 工具
├── ai-mcp-knowledge-api             # 契约层：I*Service 接口定义与 DTO
├── ai-mcp-knowledge-domain          # 领域层：领域模型、业务规则、仓储接口
├── ai-mcp-knowledge-application     # 应用层：用例编排、运行时服务
├── ai-mcp-knowledge-infrastructure  # 基础设施层：DAO、仓储实现、外部适配
├── ai-mcp-knowledge-trigger         # 触发层：HTTP Controller、网关协议、XXL-Job
├── ai-mcp-knowledge-app             # 启动装配层：Spring Boot 启动、配置装配
└── ai-mcp-knowledge-web             # 前端管理台
```

### 模块依赖关系

```
types <- api <- domain <- application <- infrastructure <- trigger <- app
                                                      └─ web (依赖 trigger)
```

## 核心配置

### application.yml 关键配置项

| 配置前缀 | 作用 |
|---|---|
| `server.port` | 后端端口 (默认 8090) |
| `spring.datasource.mysql.*` | MySQL 业务库 |
| `spring.datasource.pgvector.*` | PostgreSQL 向量库 |
| `spring.data.redis.*` | Redis |
| `vector.store.*` | 向量表名配置 |
| `chat.history.*` | 聊天记忆窗口与保留周期 |
| `xxl.job.*` | XXL-Job 执行器与 Admin 对接 |
| `sa-token.*` | 登录 token 行为 |

### 环境配置

- `application-dev.yml` - 开发环境
- `application-test.yml` - 测试环境
- `application-prod.yml` - 生产环境

## 测试策略

- 各模块包含单元测试 (`*Test.java`)
- `ai-mcp-knowledge-app` 包含 `@Tag("integration")` 集成测试
- 默认测试配置排除 integration 组

## API 入口

主要 Controller 位于 `ai-mcp-knowledge-trigger` 模块:

| 接口路径 | 模块 | 核心能力 |
|---|---|---|
| `/api/auth` | 身份认证 | 登录/登出/当前用户 |
| `/api/ai` | AI 对话 | 流式对话/可用模型列表 |
| `/api/ai/rag` | RAG 知识库 | 文件上传/异步任务/Git 仓库分析 |
| `/api/models` | 模型中心 | 配置 CRUD/激活/测试/启用禁用 |
| `/api/agents` | Agent 管理 | Agent CRUD/版本管理 |
| `/api/agents/runtime` | Agent 运行 | 调用执行/对话/中断 |
| `/api/workflows` | Workflow 管理 | 工作流 CRUD/版本/画布保存 |
| `/api/workflows/runtime` | Workflow 运行 | 执行/历史/中断 |
| `/api/gateway/manage` | Gateway 工具 | 实例/凭证/工具/绑定管理 |
| `/api/mcp/servers` | MCP Server | 配置 CRUD/刷新 |
| `/api/metrics` | 监控指标 | 指标查询 |

> 详细接口文档见：[docs/API-接口索引.md](docs/API-接口索引.md)

## 参考文档

- `docs/架构说明 - 详细版.md` - 详细架构图文
- `docs/API-接口索引.md` - 方法级接口索引
- `docs/MCP-从 0 到 1 实战指南.md` - MCP/网关工具实战指南

## 已知边界

- 数据库不使用外键，一致性由应用层保证
- `McpServerType.WEBSOCKET` 枚举存在但运行时不支持
- `mcp-tool-weixin` 目录仅保留日志，非可构建模块
