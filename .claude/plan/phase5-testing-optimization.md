# 阶段 5：测试与优化 - 详细实施计划

> **创建时间**：2026-01-27
> **测试策略**：完整模式（覆盖率 > 90%）
> **状态**：待执行

---

## 📋 任务概览

| 任务 | 描述 | 预计工作量 | 优先级 |
|------|------|-----------|--------|
| 5.1 | 单元测试 | 2 天 | P0 |
| 5.2 | 集成测试 | 2 天 | P0 |
| 5.3 | 性能优化 | 1 天 | P1 |
| 5.4 | 文档编写 | 1 天 | P1 |

---

## 🧪 任务 5.1：单元测试

### 目标
- 测试覆盖率 > 90%
- 覆盖所有核心业务逻辑
- 使用 JUnit 5 + Mockito

### 测试范围

#### 1. Service 层测试（优先级 P0）

**AIModelServiceImpl 测试**
- [ ] `testCallModel_Success` - 正常调用成功
- [ ] `testCallModel_WithFallback` - 主模型失败，降级成功
- [ ] `testCallModel_AllModelsFailed` - 所有模型失败
- [ ] `testCallModel_CircuitBreakerOpen` - 熔断器打开状态
- [ ] `testCallModel_WithRetry` - 重试机制测试
- [ ] `testCallModel_TokenLimitExceeded` - Token 超限处理

**ModelSelector 测试**
- [ ] `testSelectModel_QualityFirst` - 质量优先策略
- [ ] `testSelectModel_SpeedFirst` - 速度优先策略
- [ ] `testSelectModel_CostFirst` - 成本优先策略
- [ ] `testSelectModel_ByTaskType` - 按任务类型选择
- [ ] `testSelectModel_NoAvailableModel` - 无可用模型

**FallbackHandler 测试**
- [ ] `testHandleFallback_Success` - 降级成功
- [ ] `testHandleFallback_NoFallbackModel` - 无降级模型
- [ ] `testHandleFallback_FallbackFailed` - 降级失败
- [ ] `testHandleFallback_MaxRetriesExceeded` - 超过最大重试次数

**CircuitBreaker 测试**
- [ ] `testCircuitBreaker_Closed` - 关闭状态
- [ ] `testCircuitBreaker_Open` - 打开状态
- [ ] `testCircuitBreaker_HalfOpen` - 半开状态
- [ ] `testCircuitBreaker_StateTransition` - 状态转换
- [ ] `testCircuitBreaker_FailureThreshold` - 失败阈值触发

**ModelConfigService 测试**
- [ ] `testCreateModel` - 创建模型配置
- [ ] `testUpdateModel` - 更新模型配置
- [ ] `testDeleteModel` - 删除模型配置
- [ ] `testEnableModel` - 启用模型
- [ ] `testDisableModel` - 禁用模型
- [ ] `testGetAvailableModels` - 获取可用模型列表

**TaskTypeService 测试**
- [ ] `testCreateTaskType` - 创建任务类型
- [ ] `testUpdateTaskType` - 更新任务类型
- [ ] `testGetRecommendedModel` - 获取推荐模型

**MetricsService 测试**
- [ ] `testGetCallMetrics` - 获取调用统计
- [ ] `testGetSuccessRate` - 获取成功率
- [ ] `testGetResponseTime` - 获取响应时间
- [ ] `testGetModelUsage` - 获取模型使用分布

#### 2. Repository 层测试（优先级 P1）

**ModelConfigRepository 测试**
- [ ] `testFindByModelName` - 按名称查询
- [ ] `testFindByEnabledTrue` - 查询启用的模型
- [ ] `testFindByModelType` - 按类型查询

**TaskTypeRepository 测试**
- [ ] `testFindByTaskType` - 按任务类型查询
- [ ] `testFindWithModels` - 关联查询模型

**CallLogRepository 测试**
- [ ] `testFindByTraceId` - 按 TraceId 查询
- [ ] `testCountByStatus` - 按状态统计
- [ ] `testFindByTimeRange` - 按时间范围查询
- [ ] `testAggregateMetrics` - 聚合查询测试

#### 3. Provider 层测试（优先级 P1）

**OpenAIProvider 测试**
- [ ] `testCall_Success` - 正常调用
- [ ] `testCall_ApiKeyInvalid` - API Key 无效
- [ ] `testCall_RateLimitExceeded` - 速率限制
- [ ] `testCall_Timeout` - 超时处理

**AnthropicProvider 测试**
- [ ] `testCall_Success` - 正常调用
- [ ] `testCall_StreamingMode` - 流式响应
- [ ] `testCall_ErrorHandling` - 错误处理

**GeminiProvider 测试**
- [ ] `testCall_Success` - 正常调用
- [ ] `testCall_MultiModal` - 多模态输入
- [ ] `testCall_SafetySettings` - 安全设置

---

## 🔗 任务 5.2：集成测试

### 目标
- 测试完整的业务流程
- 使用 TestContainers 启动真实 MySQL
- 测试降级重试和熔断机制

### 测试范围

#### 1. API 集成测试（优先级 P0）

**ModelConfigController 集成测试**
- [ ] `testCreateModel_Integration` - 创建模型完整流程
- [ ] `testUpdateModel_Integration` - 更新模型完整流程
- [ ] `testDeleteModel_Integration` - 删除模型完整流程
- [ ] `testEnableDisableModel_Integration` - 启用/禁用流程

**AICallController 集成测试**
- [ ] `testChat_Success` - 正常对话流程
- [ ] `testChat_WithFallback` - 降级流程
- [ ] `testChat_CircuitBreakerTriggered` - 熔断触发
- [ ] `testChatByTaskType_Success` - 按任务类型调用

**MetricsController 集成测试**
- [ ] `testGetCallMetrics_Integration` - 统计数据查询
- [ ] `testGetSuccessRate_Integration` - 成功率查询
- [ ] `testGetResponseTime_Integration` - 响应时间查询

#### 2. 降级重试机制测试（优先级 P0）

**FallbackHandler 集成测试**
- [ ] `testFallback_PrimaryFails_SecondarySucceeds` - 主模型失败，备用成功
- [ ] `testFallback_AllModelsFail` - 所有模型失败
- [ ] `testFallback_RetryWithExponentialBackoff` - 指数退避重试
- [ ] `testFallback_MaxRetriesReached` - 达到最大重试次数

#### 3. 熔断机制测试（优先级 P0）

**CircuitBreaker 集成测试**
- [ ] `testCircuitBreaker_OpenAfterFailures` - 失败后打开熔断器
- [ ] `testCircuitBreaker_HalfOpenRecovery` - 半开状态恢复
- [ ] `testCircuitBreaker_ResetAfterSuccess` - 成功后重置
- [ ] `testCircuitBreaker_MultipleModels` - 多模型熔断器隔离

#### 4. 端到端测试（优先级 P1）

**完整业务流程测试**
- [ ] `testE2E_CreateModelAndCall` - 创建模型并调用
- [ ] `testE2E_TaskTypeConfiguration` - 任务类型配置流程
- [ ] `testE2E_MetricsCollection` - 指标收集流程
- [ ] `testE2E_AuditLogging` - 审计日志记录

---

## ⚡ 任务 5.3：性能优化

### 目标
- 优化数据库查询性能
- 实现缓存策略
- 并发性能测试

### 优化范围

#### 1. 数据库查询优化（优先级 P0）

**N+1 查询优化**
- [ ] 检查并优化 TaskType 关联查询（使用 JOIN FETCH）
- [ ] 检查并优化 ModelConfig 关联查询
- [ ] 添加必要的索引

**索引优化**
- [ ] `model_config` 表：`idx_model_name`, `idx_enabled`, `idx_model_type`
- [ ] `call_log` 表：`idx_trace_id`, `idx_request_time`, `idx_status`
- [ ] `task_type` 表：`idx_task_type`

**查询优化**
- [ ] 优化 MetricsService 的聚合查询
- [ ] 优化 CallLogRepository 的分页查询
- [ ] 添加查询结果缓存

#### 2. 缓存策略实现（优先级 P1）

**Spring Cache 集成**
- [ ] 配置 Caffeine 缓存
- [ ] 缓存模型配置列表（TTL: 5分钟）
- [ ] 缓存任务类型配置（TTL: 10分钟）
- [ ] 缓存统计数据（TTL: 1分钟）

**缓存失效策略**
- [ ] 模型配置变更时清除缓存
- [ ] 任务类型变更时清除缓存
- [ ] 定时刷新统计数据缓存

#### 3. 并发性能测试（优先级 P1）

**压力测试**
- [ ] 使用 JMeter 测试 AI 调用接口（100 并发）
- [ ] 测试熔断器在高并发下的表现
- [ ] 测试数据库连接池配置

**性能基准**
- [ ] AI 调用接口：P95 < 2s
- [ ] 模型配置查询：P95 < 100ms
- [ ] 统计数据查询：P95 < 500ms

---

## 📚 任务 5.4：文档编写

### 目标
- 完善 API 文档
- 编写部署文档
- 编写用户手册

### 文档范围

#### 1. API 文档（优先级 P0）

**Swagger/OpenAPI 文档**
- [ ] 完善所有 Controller 的 @Operation 注解
- [ ] 添加请求/响应示例
- [ ] 添加错误码说明
- [ ] 生成 API 文档 HTML

**API 使用示例**
- [ ] 模型配置管理示例
- [ ] AI 调用示例（curl、Java、Python）
- [ ] 任务类型配置示例
- [ ] 统计数据查询示例

#### 2. 部署文档（优先级 P0）

**部署指南**
- [ ] 环境要求（JDK 17、MySQL 8.0、Node.js 18+）
- [ ] 数据库初始化步骤
- [ ] 后端部署步骤（Maven 打包、配置文件、启动命令）
- [ ] 前端部署步骤（npm build、Nginx 配置）
- [ ] Docker 部署方案（Dockerfile、docker-compose.yml）

**配置说明**
- [ ] application.yml 配置项说明
- [ ] 环境变量配置
- [ ] API Key 配置
- [ ] 数据库连接配置

#### 3. 用户手册（优先级 P1）

**功能说明**
- [ ] 系统架构概述
- [ ] 模型配置管理
- [ ] 任务类型配置
- [ ] AI 调用接口使用
- [ ] 监控统计面板
- [ ] 审计日志查询

**最佳实践**
- [ ] 模型选择策略建议
- [ ] 降级重试配置建议
- [ ] 熔断器参数调优
- [ ] 性能优化建议

---

## 🎯 审查策略

根据任务复杂度，采用**快速模式（单层审查）**：
- **理由**：测试代码相对独立，风险较低
- **审查流程**：Claude 架构审查（使用 /code-review）

---

## ✅ 验收标准

### 任务 5.1：单元测试
- [ ] 测试覆盖率 > 90%
- [ ] 所有测试通过（mvn test）
- [ ] 无测试代码异味（重复代码、魔法数字）

### 任务 5.2：集成测试
- [ ] 所有集成测试通过
- [ ] 降级重试机制验证通过
- [ ] 熔断机制验证通过

### 任务 5.3：性能优化
- [ ] 数据库查询性能提升 > 50%
- [ ] 缓存命中率 > 80%
- [ ] 并发测试通过（100 并发无错误）

### 任务 5.4：文档编写
- [ ] API 文档完整且可访问
- [ ] 部署文档可执行（按步骤可成功部署）
- [ ] 用户手册清晰易懂

---

## 📊 执行计划

### 第 1 天：单元测试（上午）
1. 创建测试基础设施（BaseTest、TestConfig）
2. 编写 AIModelServiceImpl 测试
3. 编写 ModelSelector 测试
4. 编写 FallbackHandler 测试

### 第 1 天：单元测试（下午）
5. 编写 CircuitBreaker 测试
6. 编写 ModelConfigService 测试
7. 编写 Provider 层测试
8. 运行测试并检查覆盖率

### 第 2 天：集成测试（上午）
1. 配置 TestContainers
2. 编写 API 集成测试
3. 编写降级重试集成测试

### 第 2 天：集成测试（下午）
4. 编写熔断机制集成测试
5. 编写端到端测试
6. 运行所有测试并验证

### 第 3 天：性能优化
1. 数据库查询优化（索引、JOIN FETCH）
2. 实现缓存策略（Spring Cache + Caffeine）
3. 并发性能测试（JMeter）
4. 性能基准验证

### 第 4 天：文档编写
1. 完善 Swagger API 文档
2. 编写部署文档
3. 编写用户手册
4. 文档审查和发布

---

## 🔧 技术栈

- **测试框架**：JUnit 5、Mockito、AssertJ
- **集成测试**：Spring Boot Test、TestContainers、MockMvc
- **性能测试**：JMeter、Spring Boot Actuator
- **缓存**：Spring Cache、Caffeine
- **文档**：Swagger/OpenAPI、Markdown
- **CI/CD**：Maven Surefire Plugin、JaCoCo

---

## 📝 注意事项

1. **测试隔离**：每个测试方法必须独立，不依赖其他测试
2. **Mock 使用**：合理使用 Mock，避免过度 Mock
3. **测试数据**：使用 @BeforeEach 准备测试数据，@AfterEach 清理
4. **异常测试**：必须测试异常场景和边界条件
5. **性能测试**：在独立环境运行，避免干扰
6. **文档更新**：代码变更时同步更新文档
