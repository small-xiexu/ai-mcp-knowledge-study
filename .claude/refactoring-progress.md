# DDD 架构重构进度跟踪

> **开始时间**：2026-01-27 23:00
> **重构方案**：渐进式重构
> **状态**：进行中

---

## ✅ 阶段 1：创建新模块（已完成）

- [x] 创建 `ai-mcp-knowledge-domain` 模块
- [x] 创建 `ai-mcp-knowledge-infrastructure` 模块
- [x] 创建 `ai-mcp-knowledge-api` 模块
- [x] 更新根 pom.xml
- [x] 验证新模块编译通过

**完成时间**：2026-01-27 23:04

---

## 🔄 阶段 2：迁移代码（进行中）

### 2.1 迁移 DTO 到 api 模块

**源位置**：`orchestration/controller/dto/`
**目标位置**：`api/dto/`

- [ ] ModelConfigRequest.java
- [ ] ModelConfigResponse.java
- [ ] ModelCapabilityRequest.java
- [ ] TaskTypeRequest.java
- [ ] TaskTypeResponse.java
- [ ] AuditQueryRequest.java
- [ ] AuditResponse.java
- [ ] 其他 DTO 文件

### 2.2 迁移通用类型到 types 模块

**源位置**：`orchestration/controller/common/`、`orchestration/model/enums/`
**目标位置**：`types/common/`、`types/enums/`

- [ ] PageRequest.java
- [ ] PageResult.java
- [ ] Result.java
- [ ] 所有枚举类

### 2.3 迁移领域实体到 domain 模块

**源位置**：`orchestration/domain/entity/`
**目标位置**：`domain/model/entity/`

- [ ] ModelConfig.java
- [ ] ModelCapability.java
- [ ] TaskType.java
- [ ] CallLog.java
- [ ] ConfigAudit.java

### 2.4 迁移仓储接口到 domain 模块

**源位置**：`orchestration/domain/repository/`
**目标位置**：`domain/repository/`

- [ ] ModelConfigRepository.java
- [ ] TaskTypeRepository.java
- [ ] CallLogRepository.java
- [ ] ConfigAuditRepository.java

### 2.5 迁移领域服务到 domain 模块

**源位置**：`orchestration/service/`、`orchestration/fallback/`
**目标位置**：`domain/service/`

- [ ] AIModelService.java（接口）
- [ ] AIModelServiceImpl.java
- [ ] ModelSelector.java
- [ ] FallbackHandler.java
- [ ] CircuitBreaker.java
- [ ] ModelConfigService.java
- [ ] TaskTypeService.java
- [ ] MetricsService.java

### 2.6 迁移 Provider 到 infrastructure 模块

**源位置**：`orchestration/provider/`
**目标位置**：`infrastructure/provider/`

- [ ] ModelProvider.java（接口）
- [ ] OpenAIModelProvider.java
- [ ] AnthropicModelProvider.java
- [ ] GeminiModelProvider.java
- [ ] ModelProviderFactory.java

### 2.7 迁移审计功能到 infrastructure 模块

**源位置**：`orchestration/audit/`
**目标位置**：`infrastructure/audit/`

- [ ] ConfigAuditAspect.java
- [ ] 其他审计相关类

### 2.8 迁移 Controller 到 trigger 模块

**源位置**：`orchestration/controller/`
**目标位置**：`trigger/http/`

- [ ] AICallController.java
- [ ] ModelConfigController.java
- [ ] TaskTypeController.java
- [ ] MetricsController.java
- [ ] AuditController.java
- [ ] GlobalExceptionHandler.java

### 2.9 迁移配置类

**源位置**：`orchestration/config/`
**目标位置**：`infrastructure/config/` 或 `app/config/`

- [ ] SwaggerConfig.java
- [ ] 其他配置类

---

## 🧪 阶段 3：验证与清理（待执行）

- [ ] 运行所有测试
- [ ] 验证所有功能正常
- [ ] 删除 `ai-mcp-knowledge-orchestration` 模块
- [ ] 更新根 pom.xml（移除 orchestration 模块）
- [ ] 最终编译验证

---

## 📊 进度统计

- **总任务数**：约 40 个文件需要迁移
- **已完成**：3 个模块创建
- **进行中**：代码迁移
- **待执行**：验证与清理

---

## 🎯 下一步行动

1. 按依赖关系从下往上迁移代码
2. 每迁移一批文件后编译验证
3. 逐步删除 orchestration 模块中的文件
