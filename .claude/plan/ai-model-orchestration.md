# AI 多模型编排系统 - 后端核心模块实施计划

> **方案**：方案A - 轻量自研编排层 + Spring AI
> **设计者**：xiexu
> **日期**：2026-01-27
> **Codex Session**：019bfddb-c7b9-75c1-84fd-40af75e6d73c

---

## 📋 架构概览

### 核心设计原则
- **DDD 分层架构**：domain（实体+仓储）→ service（业务编排）→ provider（模型适配）→ fallback（稳定性治理）
- **轻量自研**：熔断器、重试机制、指标采集均自研，避免额外依赖
- **质量优先**：模型选择策略默认按质量评分排序
- **异步日志**：CallLog 和 ConfigAudit 异步落库，避免阻塞主流程

### 技术栈
- Spring Boot 3.4.3
- Spring AI 1.1.2（OpenAI、Anthropic、Google GenAI）
- Spring Data JPA + MySQL
- Lombok

---

## 🗂️ 包结构设计

```
ai-mcp-knowledge-orchestration/
└── src/main/java/com/xbk/knowledge/orchestration/
    ├── domain/
    │   ├── entity/
    │   │   ├── ModelConfig.java          # 模型配置实体
    │   │   ├── ModelCapability.java      # 模型能力实体
    │   │   ├── TaskType.java             # 任务类型实体
    │   │   ├── CallLog.java              # 调用日志实体
    │   │   └── ConfigAudit.java          # 配置审计实体
    │   └── repository/
    │       ├── ModelConfigRepository.java
    │       ├── ModelCapabilityRepository.java
    │       ├── TaskTypeRepository.java
    │       ├── CallLogRepository.java
    │       └── ConfigAuditRepository.java
    ├── model/
    │   ├── dto/
    │   │   ├── AIRequest.java            # 统一请求对象
    │   │   ├── AIResponse.java           # 统一响应对象
    │   │   ├── ModelInfo.java            # 模型元信息
    │   │   └── ModelSelectionResult.java # 模型选择结果
    │   └── enums/
    │       ├── ModelType.java            # 模型类型枚举
    │       ├── TaskTypeEnum.java         # 任务类型枚举
    │       └── CallStatus.java           # 调用状态枚举
    ├── provider/
    │   ├── ModelProvider.java            # 模型提供者接口
    │   ├── OpenAIModelProvider.java      # OpenAI 实现
    │   ├── AnthropicModelProvider.java   # Anthropic 实现
    │   ├── GeminiModelProvider.java      # Gemini 实现
    │   └── ModelProviderFactory.java     # 工厂类
    ├── service/
    │   ├── AIModelService.java           # 统一服务接口
    │   ├── AIModelServiceImpl.java       # 服务实现
    │   └── ModelSelector.java            # 模型选择器
    ├── fallback/
    │   ├── FallbackHandler.java          # 降级处理器
    │   ├── CircuitBreaker.java           # 熔断器
    │   └── MetricsCollector.java         # 指标收集器
    └── config/
        └── AsyncConfig.java              # 异步配置
```

---

## 🏗️ 核心类设计

### 1. Domain 层

#### ModelConfig（模型配置实体）
```
@Entity
@Table(name = "ai_model_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

字段：
- id: Long (主键)
- modelName: String (模型名称)
- modelType: ModelType (模型类型枚举)
- apiKey: String (API密钥)
- baseUrl: String (API地址)
- enabled: Boolean (是否启用)
- priority: Integer (优先级)
- createdAt: LocalDateTime
- updatedAt: LocalDateTime

关系：
- @OneToOne(mappedBy = "modelConfig") ModelCapability capability
```

#### ModelCapability（模型能力实体）
```
@Entity
@Table(name = "ai_model_capability")
@Data
@Builder

字段：
- id: Long
- modelId: Long (外键)
- maxInputTokens: Integer
- maxOutputTokens: Integer
- supportFunctionCalling: Boolean
- supportVision: Boolean
- supportStreaming: Boolean
- qualityScore: Integer (质量评分 1-100)
- createdAt: LocalDateTime
- updatedAt: LocalDateTime

关系：
- @ManyToOne ModelConfig modelConfig
```

#### TaskType（任务类型实体）
```
@Entity
@Table(name = "ai_task_type")
@Data

字段：
- id: Long
- taskName: String
- taskCode: String (唯一)
- description: String
- preferredModelId: Long (首选模型ID)
- fallbackModelIds: String (备用模型ID列表，逗号分隔)
- createdAt: LocalDateTime
- updatedAt: LocalDateTime
```

#### CallLog（调用日志实体）
```
@Entity
@Table(name = "ai_call_log")
@Data

字段：
- id: Long
- modelId: Long
- taskType: String
- requestContent: String (TEXT)
- responseContent: String (TEXT)
- tokensUsed: Integer
- responseTime: Long (毫秒)
- status: CallStatus (SUCCESS/FAILED/FALLBACK)
- errorMessage: String
- createdAt: LocalDateTime
```

#### ConfigAudit（配置审计实体）
```
@Entity
@Table(name = "ai_config_audit")
@Data

字段：
- id: Long
- tableName: String
- recordId: Long
- operation: String (INSERT/UPDATE/DELETE)
- oldValue: String (JSON)
- newValue: String (JSON)
- operator: String
- createdAt: LocalDateTime
```

### 2. Model 层

#### AIRequest（统一请求对象）
```
@Data
@Builder

字段：
- content: String (请求内容)
- taskType: String (任务类型)
- systemPrompt: String (系统提示词，可选)
- parameters: Map<String, Object> (模型参数，可选)
- streaming: Boolean (是否流式输出)
```

#### AIResponse（统一响应对象）
```
@Data
@Builder

字段：
- content: String (响应内容)
- modelUsed: String (使用的模型名称)
- tokensUsed: Integer (使用的token数)
- responseTime: Long (响应时间ms)
- success: Boolean (是否成功)
- errorMessage: String (错误信息)
- fallback: Boolean (是否使用了降级模型)
- retryCount: Integer (重试次数)
```

#### ModelInfo（模型元信息）
```
@Data
@Builder

字段：
- modelId: Long
- modelName: String
- modelType: ModelType
- qualityScore: Integer
- enabled: Boolean
- capability: ModelCapability
```

### 3. Provider 层

#### ModelProvider（接口）
```
接口方法：
- ChatClient createChatClient(ModelConfig config)
- ModelType getModelType()
- boolean isHealthy(ModelConfig config)
```

#### OpenAIModelProvider
```
@Component
实现 ModelProvider

依赖：
- Spring AI OpenAI 客户端

核心逻辑：
- 根据 ModelConfig 创建 OpenAiApi
- 构建 OpenAiChatModel
- 返回 ChatClient
```

#### AnthropicModelProvider
```
@Component
实现 ModelProvider

依赖：
- Spring AI Anthropic 客户端

核心逻辑：
- 根据 ModelConfig 创建 AnthropicApi
- 构建 AnthropicChatModel
- 返回 ChatClient
```

#### GeminiModelProvider
```
@Component
实现 ModelProvider

依赖：
- Spring AI Google GenAI 客户端

核心逻辑：
- 根据 ModelConfig 创建 Google GenAI Client
- 构建 GoogleGenAiChatModel
- 返回 ChatClient
```

#### ModelProviderFactory
```
@Component

依赖：
- @Autowired List<ModelProvider> providers

核心方法：
- ModelProvider getProvider(ModelType type)
- ChatClient createChatClient(ModelConfig config)
```

### 4. Service 层

#### AIModelService（接口）
```
接口方法：
- AIResponse chat(AIRequest request)
- AIResponse chatByTaskType(String taskType, AIRequest request)
- List<ModelInfo> getAvailableModels()
- ModelInfo getRecommendedModel(String taskType)
```

#### AIModelServiceImpl
```
@Service
@Slf4j

依赖：
- ModelSelector modelSelector
- ModelProviderFactory providerFactory
- FallbackHandler fallbackHandler
- MetricsCollector metricsCollector
- ModelConfigRepository configRepository

核心逻辑：
1. 接收请求
2. 调用 ModelSelector 选择模型
3. 通过 FallbackHandler 执行带降级的调用
4. 记录指标
5. 返回响应
```

#### ModelSelector
```
@Component

依赖：
- ModelConfigRepository configRepository
- ModelCapabilityRepository capabilityRepository
- TaskTypeRepository taskTypeRepository
- CircuitBreaker circuitBreaker

核心方法：
- ModelSelectionResult selectModel(String taskType)
- ModelConfig selectByQualityPriority()

选择逻辑：
1. 根据 taskType 查询 TaskType 配置
2. 获取首选模型和备用模型列表
3. 检查熔断状态
4. 按质量评分排序
5. 返回可用的最优模型
```

### 5. Fallback 层

#### FallbackHandler
```
@Component
@Slf4j

依赖：
- CircuitBreaker circuitBreaker
- ModelProviderFactory providerFactory
- MetricsCollector metricsCollector

核心方法：
- AIResponse executeWithFallback(ModelConfig primary, List<ModelConfig> fallbacks, AIRequest request)
- AIResponse executeWithRetry(ModelConfig model, AIRequest request, int maxRetries)

降级逻辑：
1. 尝试主模型（带重试1次）
2. 主模型失败，记录失败
3. 遍历备用模型列表
4. 跳过已熔断的模型
5. 尝试备用模型
6. 返回结果（标记 fallback=true）
```

#### CircuitBreaker
```
@Component

存储：
- ConcurrentHashMap<Long, CircuitState> circuitStates

配置：
- FAILURE_THRESHOLD = 3 (连续失败3次触发熔断)
- RECOVERY_TIMEOUT = 5 * 60 * 1000 (5分钟恢复)

核心方法：
- boolean isOpen(Long modelId)
- void recordSuccess(Long modelId)
- void recordFailure(Long modelId)

状态机：
- CLOSED（正常）→ OPEN（熔断）→ HALF_OPEN（半开）→ CLOSED
```

#### MetricsCollector
```
@Component
@Slf4j

依赖：
- CallLogRepository callLogRepository
- @Async 异步执行器

核心方法：
- @Async void recordCall(CallLog callLog)
- Statistics getStatistics(StatisticsQuery query)

异步记录：
- 避免阻塞主流程
- 批量写入优化（可选）
```

---

## 🔄 核心流程

### 正常调用流程
```
1. Controller 接收 AIRequest
2. AIModelService.chat(request)
3. ModelSelector.selectModel(taskType)
   - 查询 TaskType 配置
   - 获取首选模型和备用模型
   - 检查熔断状态
   - 返回 ModelSelectionResult
4. FallbackHandler.executeWithFallback(primary, fallbacks, request)
5. ModelProviderFactory.createChatClient(primary)
6. ChatClient.prompt().user(content).call()
7. 记录 CallLog（异步）
8. 返回 AIResponse
```

### 失败降级流程
```
1. 主模型调用失败
2. CircuitBreaker.recordFailure(modelId)
3. 检查失败次数，达到阈值则 OPEN
4. 重试1次（executeWithRetry）
5. 仍失败，遍历备用模型
6. 跳过已熔断的模型
7. 调用备用模型
8. 成功则返回（fallback=true）
9. 失败则继续下一个备用模型
10. 所有模型失败，返回错误响应
```

### 熔断恢复流程
```
1. 模型进入 OPEN 状态
2. 5分钟后自动转为 HALF_OPEN
3. 允许一次探测调用
4. 成功 → CLOSED（恢复正常）
5. 失败 → OPEN（重新熔断）
```

---

## ⚠️ 关键注意事项

### 1. 安全性
- ✅ API Key 仅从配置文件读取，不落库明文
- ✅ CallLog 中的敏感内容需脱敏
- ✅ ConfigAudit 记录操作人信息

### 2. 性能优化
- ✅ CallLog 异步写入（@Async）
- ✅ 熔断状态存储在内存（ConcurrentHashMap）
- ✅ 避免频繁查询数据库（可考虑缓存）

### 3. 异常处理
- ✅ 所有 Provider 调用需 try-catch
- ✅ 熔断器异常不影响主流程
- ✅ 日志记录失败不影响响应

### 4. 扩展性
- ✅ ModelProvider 接口易于扩展新模型
- ✅ ModelSelector 可演进为策略模式
- ✅ 后续可升级到 Resilience4j

---

## 📝 实施步骤

### 步骤 1：创建枚举类（5分钟）
- ModelType.java
- TaskTypeEnum.java
- CallStatus.java

### 步骤 2：创建实体类（20分钟）
- ModelConfig.java
- ModelCapability.java
- TaskType.java
- CallLog.java
- ConfigAudit.java

### 步骤 3：创建 Repository（10分钟）
- 5个 Repository 接口

### 步骤 4：创建 DTO 类（15分钟）
- AIRequest.java
- AIResponse.java
- ModelInfo.java
- ModelSelectionResult.java

### 步骤 5：创建 Provider 层（30分钟）
- ModelProvider 接口
- OpenAIModelProvider
- AnthropicModelProvider
- GeminiModelProvider
- ModelProviderFactory

### 步骤 6：创建 Fallback 层（30分钟）
- CircuitBreaker
- FallbackHandler
- MetricsCollector

### 步骤 7：创建 Service 层（30分钟）
- ModelSelector
- AIModelService 接口
- AIModelServiceImpl

### 步骤 8：配置异步执行器（10分钟）
- AsyncConfig.java

### 步骤 9：编译验证（5分钟）
- mvn clean compile -DskipTests

**预计总时间**：2.5 小时

---

## ✅ 验收标准

1. ✅ 所有类编译通过
2. ✅ 实体类与数据库表映射正确
3. ✅ Provider 可以成功创建 ChatClient
4. ✅ 熔断器状态机正常工作
5. ✅ 异步日志记录不阻塞主流程
6. ✅ 代码符合规范（@author xiexu、中文注释、Lombok）

---

## 🎯 下一步

等待用户批准后，进入**阶段 4：执行**，开始编写代码。
