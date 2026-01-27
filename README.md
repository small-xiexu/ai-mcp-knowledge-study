# ai-mcp-knowledge-study

> 基于 DDD 分层的知识型 AI 工程示例，包含任务调度、模型调用与 MCP 工具协同。

## 分层与模块职责（DDD）

- Interface Adapters（接口适配层）
  - 对应模块：`ai-mcp-knowledge-trigger`
  - 职责：协议转换、参数校验、调用应用层，屏蔽外部技术细节。

- Application（应用层）
  - 对应模块：`ai-mcp-knowledge-app`
  - 职责：用例编排、事务边界、跨聚合协调、领域服务调用、系统装配与启动。

- Domain（领域层）
  - 对应模块：`ai-mcp-knowledge-domain`
  - 职责：领域模型、聚合、实体、值对象、领域服务、领域事件与仓储接口定义。
  - 当前状态：待补充。

- Infrastructure（基础设施层）
  - 对应模块：`ai-mcp-knowledge-infrastructure`
  - 职责：仓储实现、消息中间件、缓存、外部系统适配与技术实现细节。
  - 当前状态：待补充。

- Shared Kernel（共享内核/通用类型）
  - 对应模块：`ai-mcp-knowledge-types`
  - 职责：跨层复用的类型、DTO、通用工具与契约边界。

## 已存在模块

- `ai-mcp-knowledge-app`
- `ai-mcp-knowledge-trigger`
- `ai-mcp-knowledge-types`

## 变更记录

- 将共享内核模块从 `ai-mcp-knowledge-common` 更名为 `ai-mcp-knowledge-types`。

