# AI 多模型编排系统 - 开发计划

> **最后更新**：2026-01-28
> **状态**：进行中

---

## 任务 2：ModelType → ProviderType 重命名重构

> **开始时间**：2026-01-28 11:00
> **完成时间**：2026-01-28 11:16
> **状态**：✅ 已完成

### 背景

`ModelType` 枚举命名不准确，容易让人误解为具体的模型类型（如 GPT-4、Claude-3.5），实际上它表示的是 API 提供商/协议类型（如 OpenAI API、Anthropic API）。

### 目标

将 `ModelType` 重命名为 `ProviderType`，并添加详细注释说明：
- 这是 API 协议类型，而非具体模型名称
- OPENAI 类型可以对接多个模型（GPT-4、DeepSeek、智谱等）
- 通过 baseUrl 和 modelName 配置灵活对接不同服务

### 实施步骤

- [x] 创建新的 ProviderType.java，包含详细文档注释
- [x] 更新 ModelConfig 实体的字段名和注释
- [x] 批量替换所有文件中的 import 语句
- [x] 修复 ModelProviderFactory 的 Map 声明
- [x] 修复 Provider 实现类的返回语句
- [x] 更新 ModelConfigRequest DTO
- [x] 更新 ModelConfigResponse DTO
- [x] 更新 ModelInfo DTO
- [x] 修复 ModelConfigController 的 builder 调用
- [x] 修复 AIModelServiceImpl 的 builder 调用
- [x] 编译验证通过

### 验收标准

- ✅ 所有文件使用 ProviderType 替代 ModelType
- ✅ 添加了详细的文档注释说明 API 协议类型的概念
- ✅ 编译通过，无错误
- ✅ 所有 DTO 和实体类字段名统一为 providerType

### 影响范围

**修改的文件**：
- `ModelConfig.java` - 实体类字段重命名
- `ModelConfigRequest.java` - DTO 字段重命名
- `ModelConfigResponse.java` - DTO 字段重命名
- `ModelInfo.java` - DTO 字段重命名
- `ModelProvider.java` - 接口方法重命名
- `ModelProviderFactory.java` - Map 类型更新
- `OpenAIModelProvider.java` - 返回类型更新
- `AnthropicModelProvider.java` - 返回类型更新
- `GeminiModelProvider.java` - 返回类型更新
- `ModelConfigController.java` - Builder 方法调用更新
- `AIModelServiceImpl.java` - Builder 方法调用更新
- `ModelConfigRepository.java` - 查询方法参数类型更新

**新增的文件**：
- `ProviderType.java` - 新的枚举类，包含详细文档

**删除的文件**：
- `ModelType.java` - 旧的枚举类

---

## 任务 1：MCP 工具集成重构计划

> **开始时间**：2026-01-28
> **任务名称**：集成 MCP 工具到编排层
> **状态**：进行中

---

## 背景

当前架构存在问题：
- 编排层（orchestration）和 MCP 工具层是分离的
- 测试代码为了使用 MCP 工具，不得不绕过编排层直接使用底层 ChatModel
- 无法同时享受模型编排能力（选择、降级、重试）和 MCP 工具调用能力

## 目标

将 MCP 工具集成到编排层，使得通过 `AIModelService` 创建的 `ChatClient` 自动支持：
- ✅ 模型选择和降级
- ✅ MCP 工具调用
- ✅ 链路追踪（TraceIdAdvisor）
- ✅ 其他 Advisors

## 实施方案：方案 A - 统一注入

### 架构设计

```
业务代码
   ↓
AIModelService (编排层)
   ↓
ModelProvider (创建 ChatClient)
   ↓
ChatClientEnhancer (统一增强器)
   ↓
自动注入: ToolCallbackProvider + Advisors
   ↓
ChatClient (完整能力)
```

---

## 实施步骤

### 步骤 1：创建 ChatClientEnhancer 工具类
- [ ] 创建 `ai-mcp-knowledge-orchestration/src/main/java/com/xbk/knowledge/orchestration/config/ChatClientEnhancer.java`
- [ ] 实现统一的 ChatClient 增强逻辑
- [ ] 通过构造器注入 `ToolCallbackProvider` 和 `List<CallAdvisor>`
- [ ] 提供 `enhance(ChatModel)` 方法

**关键代码**：
```java
@Component
@RequiredArgsConstructor
public class ChatClientEnhancer {
    private final ToolCallbackProvider tools;
    private final List<CallAdvisor> advisors;

    public ChatClient enhance(ChatModel chatModel) {
        var builder = ChatClient.builder(chatModel)
            .defaultToolCallbacks(tools);

        if (advisors != null && !advisors.isEmpty()) {
            builder.defaultAdvisors(advisors.toArray(new CallAdvisor[0]));
        }

        return builder.build();
    }
}
```

### 步骤 2：修改 OpenAIModelProvider
- [ ] 修改 `ai-mcp-knowledge-orchestration/src/main/java/com/xbk/knowledge/orchestration/provider/OpenAIModelProvider.java`
- [ ] 改为使用 `@RequiredArgsConstructor` 构造器注入
- [ ] 注入 `ChatClientEnhancer`
- [ ] 修改 `createChatClient` 方法使用增强器

**修改点**：
```java
@Component
@Slf4j
@RequiredArgsConstructor  // 改为构造器注入
public class OpenAIModelProvider implements ModelProvider {

    private final ChatClientEnhancer enhancer;  // 注入增强器

    @Override
    public ChatClient createChatClient(ModelConfig config) {
        ChatModel chatModel = createChatModel(config);
        return enhancer.enhance(chatModel);  // 使用增强器
    }
}
```

### 步骤 3：修改 AnthropicModelProvider
- [ ] 修改 `ai-mcp-knowledge-orchestration/src/main/java/com/xbk/knowledge/orchestration/provider/AnthropicModelProvider.java`
- [ ] 改为使用 `@RequiredArgsConstructor` 构造器注入
- [ ] 注入 `ChatClientEnhancer`
- [ ] 修改 `createChatClient` 方法使用增强器

### 步骤 4：修改 GeminiModelProvider
- [ ] 修改 `ai-mcp-knowledge-orchestration/src/main/java/com/xbk/knowledge/orchestration/provider/GeminiModelProvider.java`
- [ ] 改为使用 `@RequiredArgsConstructor` 构造器注入
- [ ] 注入 `ChatClientEnhancer`
- [ ] 修改 `createChatClient` 方法使用增强器

### 步骤 5：更新 AIModelServiceImpl（如果需要）
- [ ] 检查 `AIModelServiceImpl` 是否需要修改
- [ ] 确保使用 `createChatClient` 而非直接使用 `ChatModel`

### 步骤 6：更新测试代码
- [ ] 修改 `MCPTest.java`，添加使用 `AIModelService` 的测试方法
- [ ] 验证通过编排层调用时，MCP 工具和模型选择同时生效
- [ ] 保留原有的直接调用测试作为对比

### 步骤 7：编译验证
- [ ] 执行 `mvn clean compile -DskipTests`
- [ ] 确保所有模块编译通过
- [ ] 修复任何编译错误

---

## 验收标准

1. ✅ 所有 ModelProvider 实现类都使用 ChatClientEnhancer
2. ✅ 通过 AIModelService 创建的 ChatClient 自动支持 MCP 工具
3. ✅ 通过 AIModelService 创建的 ChatClient 自动支持 Advisors
4. ✅ 编译通过，无错误
5. ✅ 测试代码验证功能正常

---

## 技术要点

### 依赖注入
- 使用 `@RequiredArgsConstructor` 实现构造器注入
- Spring 自动装配 `ToolCallbackProvider` 和 `List<CallAdvisor>`

### 可选依赖处理
- `ToolCallbackProvider` 可能不存在（如果没有配置 MCP）
- `List<CallAdvisor>` 可能为空
- 需要在 `ChatClientEnhancer` 中做空值检查

### 向后兼容
- 不修改 `ModelProvider` 接口
- 不修改 `AIModelService` 接口
- 对现有代码影响最小

---

## 风险与注意事项

1. **依赖注入失败**：如果 `ToolCallbackProvider` 不存在，Spring 启动会失败
   - 解决：使用 `@Autowired(required = false)` 或 `Optional<ToolCallbackProvider>`

2. **循环依赖**：如果 Provider 和 Enhancer 之间存在循环依赖
   - 解决：确保依赖方向单向（Provider → Enhancer → Tools）

3. **性能影响**：每次创建 ChatClient 都会注入工具
   - 影响：可忽略，工具注入是轻量级操作

---

## 进度记录

| 时间 | 步骤 | 状态 | 备注 |
|------|------|------|------|
| 2026-01-28 02:04 | 创建计划 | ✅ 完成 | 方案 A 设计完成 |
| | 步骤 1 | ⏳ 待执行 | 创建 ChatClientEnhancer |
| | 步骤 2 | ⏳ 待执行 | 修改 OpenAIModelProvider |
| | 步骤 3 | ⏳ 待执行 | 修改 AnthropicModelProvider |
| | 步骤 4 | ⏳ 待执行 | 修改 GeminiModelProvider |
| | 步骤 5 | ⏳ 待执行 | 检查 AIModelServiceImpl |
| | 步骤 6 | ⏳ 待执行 | 更新测试代码 |
| | 步骤 7 | ⏳ 待执行 | 编译验证 |

---

## 下一步

执行步骤 1-7，使用 CCG 工具完成代码实现。
