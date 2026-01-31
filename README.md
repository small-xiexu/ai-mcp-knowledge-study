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

## 模型调用与降级设计

为降低阅读成本并便于扩展，模型调用流程采用多种设计模式分层解耦：

- 策略模式（Strategy）
  - 作用：定义候选模型顺序与降级规则
  - 关键类：`FailoverStrategy`、`PriorityFailoverStrategy`

- 模板方法（Template Method）+ 迭代器（Iterator）
  - 作用：固定降级流程骨架，并把“主/备遍历”从业务代码中隐藏
  - 关键类：`AbstractFailoverExecutor`、`DefaultFailoverExecutor`、`FailoverPlan`、`DefaultFailoverPlan`

- 责任链（Chain of Responsibility）
  - 作用：叠加重试、熔断、日志等横切能力，避免流程膨胀
  - 关键类：`ModelCallPipeline`、`ModelCallPolicy`、`RetryPolicy`、`CircuitBreakerPolicy`、`LoggingPolicy`

调用入口保持简单：
- `FallbackHandler` 只负责触发降级执行器

## 模型选择责任链（显式 next 方式）

为保证顺序清晰可读，模型选择采用“显式 next”责任链模式：

- 责任链装配集中在 `ModelSelectionChainFactory`
  - 通过枚举 `ModelSelectionHandlerOrder` 固定顺序
  - 通过 `Map<String, ModelSelectionHandler>` 获取处理器并显式挂接
- 处理器内部显式调用 `next()` 决定是否进入下一个节点
  - 命中当前节点直接处理
  - 未命中则交给下一个节点

当前顺序：
1. 显式策略处理器
2. 任务类型处理器
3. 默认兜底处理器

## 变更记录

- 将共享内核模块从 `ai-mcp-knowledge-common` 更名为 `ai-mcp-knowledge-types`。
