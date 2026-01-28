# 任务 2.3 - AIModelService 核心服务实施计划

> **方案**：方案 B - 与设计对齐的核心方案（Codex 推荐）
> **批准时间**：2026-01-27
> **状态**：执行中

---

## 一、文件结构

### 新增文件（5个）
1. `service/AIModelService.java` - 统一服务接口
2. `service/ModelSelector.java` - 模型选择器
3. `service/impl/AIModelServiceImpl.java` - 服务实现类
4. `model/enums/ModelSelectionStrategy.java` - 选择策略枚举

### 修改文件（1个）
5. `model/dto/AIRequest.java` - 补充 strategy 字段

---

## 二、类设计

### 1. AIModelService（接口）
- chat(AIRequest) → AIResponse
- chatByTaskType(String, AIRequest) → AIResponse
- getAvailableModels() → List<ModelInfo>
- getRecommendedModel(String) → ModelInfo

### 2. AIModelServiceImpl（实现类）
**依赖注入**：
- ModelSelector
- ModelProviderFactory
- CallLogRepository

**核心逻辑**：
- chat：按 strategy 选择模型，执行调用，记录日志
- chatByTaskType：按 taskType 选择主/备模型，主失败尝试备
- getAvailableModels：返回 enabled=true 的模型
- getRecommendedModel：基于 taskType 或质量优先返回
