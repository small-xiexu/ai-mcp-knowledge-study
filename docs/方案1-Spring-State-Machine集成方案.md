# 方案 1：Spring State Machine 集成方案

## 一、方案概述

使用 Spring State Machine 为 AI 任务编排提供状态管理和流程控制能力。

## 二、依赖引入

```xml
<dependency>
    <groupId>org.springframework.statemachine</groupId>
    <artifactId>spring-statemachine-core</artifactId>
    <version>3.2.1</version>
</dependency>
```

## 三、状态定义

```java
/**
 * AI 任务状态枚举
 * @author xiexu
 */
public enum AiTaskState {
    INIT,              // 初始化
    RAG_RETRIEVAL,     // RAG 检索
    MODEL_SELECTION,   // 模型选择
    PRIMARY_CALL,      // 主模型调用
    TOOL_CALLING,      // 工具调用
    FALLBACK_CALL,     // 备用模型调用
    VALIDATION,        // 结果验证
    LOGGING,           // 日志记录
    COMPLETED,         // 完成
    FAILED             // 失败
}
```

## 四、事件定义

```java
/**
 * AI 任务事件枚举
 * @author xiexu
 */
public enum AiTaskEvent {
    START,                  // 开始任务
    NEED_RAG,              // 需要 RAG 检索
    SKIP_RAG,              // 跳过 RAG
    RAG_COMPLETED,         // RAG 完成
    MODEL_SELECTED,        // 模型已选择
    CALL_SUCCESS,          // 调用成功
    CALL_FAILED,           // 调用失败
    NEED_TOOL,             // 需要工具调用
    TOOL_COMPLETED,        // 工具调用完成
    VALIDATION_PASSED,     // 验证通过
    VALIDATION_FAILED,     // 验证失败
    LOGGING_COMPLETED,     // 日志记录完成
    RETRY,                 // 重试
    GIVE_UP                // 放弃
}
```

## 五、状态机配置

```java
/**
 * AI 任务状态机配置
 * @author xiexu
 */
@Configuration
@EnableStateMachine
public class AiTaskStateMachineConfig extends StateMachineConfigurerAdapter<AiTaskState, AiTaskEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<AiTaskState, AiTaskEvent> states) throws Exception {
        states
            .withStates()
            .initial(AiTaskState.INIT)
            .state(AiTaskState.RAG_RETRIEVAL)
            .state(AiTaskState.MODEL_SELECTION)
            .state(AiTaskState.PRIMARY_CALL)
            .state(AiTaskState.TOOL_CALLING)
            .state(AiTaskState.FALLBACK_CALL)
            .state(AiTaskState.VALIDATION)
            .state(AiTaskState.LOGGING)
            .end(AiTaskState.COMPLETED)
            .end(AiTaskState.FAILED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<AiTaskState, AiTaskEvent> transitions) throws Exception {
        transitions
            // 初始化 -> RAG 检索 或 模型选择
            .withExternal()
                .source(AiTaskState.INIT).target(AiTaskState.RAG_RETRIEVAL)
                .event(AiTaskEvent.NEED_RAG)
            .and()
            .withExternal()
                .source(AiTaskState.INIT).target(AiTaskState.MODEL_SELECTION)
                .event(AiTaskEvent.SKIP_RAG)
            .and()
            // RAG 检索 -> 模型选择
            .withExternal()
                .source(AiTaskState.RAG_RETRIEVAL).target(AiTaskState.MODEL_SELECTION)
                .event(AiTaskEvent.RAG_COMPLETED)
            .and()
            // 模型选择 -> 主模型调用
            .withExternal()
                .source(AiTaskState.MODEL_SELECTION).target(AiTaskState.PRIMARY_CALL)
                .event(AiTaskEvent.MODEL_SELECTED)
            .and()
            // 主模型调用 -> 工具调用 或 验证
            .withExternal()
                .source(AiTaskState.PRIMARY_CALL).target(AiTaskState.TOOL_CALLING)
                .event(AiTaskEvent.NEED_TOOL)
            .and()
            .withExternal()
                .source(AiTaskState.PRIMARY_CALL).target(AiTaskState.VALIDATION)
                .event(AiTaskEvent.CALL_SUCCESS)
            .and()
            // 主模型调用失败 -> 备用模型
            .withExternal()
                .source(AiTaskState.PRIMARY_CALL).target(AiTaskState.FALLBACK_CALL)
                .event(AiTaskEvent.CALL_FAILED)
            .and()
            // 工具调用 -> 主模型调用（继续对话）
            .withExternal()
                .source(AiTaskState.TOOL_CALLING).target(AiTaskState.PRIMARY_CALL)
                .event(AiTaskEvent.TOOL_COMPLETED)
            .and()
            // 备用模型 -> 验证
            .withExternal()
                .source(AiTaskState.FALLBACK_CALL).target(AiTaskState.VALIDATION)
                .event(AiTaskEvent.CALL_SUCCESS)
            .and()
            // 备用模型失败 -> 失败
            .withExternal()
                .source(AiTaskState.FALLBACK_CALL).target(AiTaskState.FAILED)
                .event(AiTaskEvent.GIVE_UP)
            .and()
            // 验证 -> 日志记录
            .withExternal()
                .source(AiTaskState.VALIDATION).target(AiTaskState.LOGGING)
                .event(AiTaskEvent.VALIDATION_PASSED)
            .and()
            // 日志记录 -> 完成
            .withExternal()
                .source(AiTaskState.LOGGING).target(AiTaskState.COMPLETED)
                .event(AiTaskEvent.LOGGING_COMPLETED);
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<AiTaskState, AiTaskEvent> config) throws Exception {
        config
            .withConfiguration()
            .autoStartup(true)
            .listener(new AiTaskStateMachineListener());
    }
}
```

## 六、状态机监听器

```java
/**
 * AI 任务状态机监听器
 * @author xiexu
 */
@Slf4j
public class AiTaskStateMachineListener extends StateMachineListenerAdapter<AiTaskState, AiTaskEvent> {

    @Override
    public void stateChanged(State<AiTaskState, AiTaskEvent> from, State<AiTaskState, AiTaskEvent> to) {
        log.info("状态变更: {} -> {}",
            from != null ? from.getId() : "null",
            to.getId());
    }

    @Override
    public void eventNotAccepted(Message<AiTaskEvent> event) {
        log.warn("事件未被接受: {}", event.getPayload());
    }

    @Override
    public void stateMachineError(StateMachine<AiTaskState, AiTaskEvent> stateMachine, Exception exception) {
        log.error("状态机错误", exception);
    }
}
```

## 七、状态机服务

```java
/**
 * AI 任务状态机服务
 * @author xiexu
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiTaskStateMachineService {

    private final StateMachine<AiTaskState, AiTaskEvent> stateMachine;
    private final RagAppService ragAppService;
    private final ModelSelector modelSelector;
    private final ModelProviderFactory modelProviderFactory;

    /**
     * 执行 AI 任务
     */
    public String executeTask(AICallCommand command) {
        // 启动状态机
        stateMachine.start();

        try {
            // 1. 判断是否需要 RAG
            if (command.getRagTags() != null && !command.getRagTags().isEmpty()) {
                stateMachine.sendEvent(AiTaskEvent.NEED_RAG);
                // 执行 RAG 检索
                List<String> contexts = ragAppService.search(command.getRagTags(), command.getPrompt());
                command.setRagContexts(contexts);
                stateMachine.sendEvent(AiTaskEvent.RAG_COMPLETED);
            } else {
                stateMachine.sendEvent(AiTaskEvent.SKIP_RAG);
            }

            // 2. 模型选择
            ModelSelectionResult selection = modelSelector.selectModel(command.getTaskType());
            stateMachine.sendEvent(AiTaskEvent.MODEL_SELECTED);

            // 3. 主模型调用
            try {
                String result = callModel(selection.getPrimaryModel(), command);
                stateMachine.sendEvent(AiTaskEvent.CALL_SUCCESS);

                // 4. 结果验证
                stateMachine.sendEvent(AiTaskEvent.VALIDATION_PASSED);

                // 5. 日志记录
                stateMachine.sendEvent(AiTaskEvent.LOGGING_COMPLETED);

                return result;
            } catch (Exception e) {
                log.error("主模型调用失败", e);
                stateMachine.sendEvent(AiTaskEvent.CALL_FAILED);

                // 尝试备用模型
                for (ModelConfig fallback : selection.getFallbackModels()) {
                    try {
                        String result = callModel(fallback, command);
                        stateMachine.sendEvent(AiTaskEvent.CALL_SUCCESS);
                        stateMachine.sendEvent(AiTaskEvent.VALIDATION_PASSED);
                        stateMachine.sendEvent(AiTaskEvent.LOGGING_COMPLETED);
                        return result;
                    } catch (Exception ex) {
                        log.error("备用模型调用失败: {}", fallback.getModelName(), ex);
                    }
                }

                stateMachine.sendEvent(AiTaskEvent.GIVE_UP);
                throw new RuntimeException("所有模型调用失败");
            }
        } finally {
            stateMachine.stop();
        }
    }

    private String callModel(ModelConfig model, AICallCommand command) {
        ModelProvider provider = modelProviderFactory.getProvider(model.getModelType());
        return provider.call(model, command);
    }
}
```

## 八、优势

1. **状态可视化**：清晰的状态流转图
2. **状态持久化**：可以保存状态到数据库，支持中断恢复
3. **事件驱动**：解耦状态转换逻辑
4. **易于扩展**：新增状态和转换规则简单

## 九、不足

1. **复杂度增加**：引入状态机框架，学习成本
2. **性能开销**：状态机本身有一定开销
3. **调试困难**：状态流转问题不易排查

## 十、适用场景

- 中等复杂度的 AI 工作流（3-10 个步骤）
- 需要状态持久化和中断恢复
- 需要可视化状态流转
- 团队对状态机模式熟悉