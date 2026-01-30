package com.xbk.knowledge.infrastructure.audit;

import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.api.dto.model.ModelConfigResponse;
import com.xbk.knowledge.api.dto.task.TaskTypeResponse;
import com.xbk.knowledge.domain.model.entity.ModelConfig;
import com.xbk.knowledge.domain.model.entity.TaskType;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.repository.ModelConfigRepository;
import com.xbk.knowledge.domain.repository.TaskTypeRepository;
import com.xbk.knowledge.infrastructure.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 配置审计切面
 * 通过统一拦截控制层变更操作，保证审计记录不遗漏
 *
 * 职责：基础设施审计能力，用于持久化变更记录
 * @author xiexu
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {

    private static final String MODEL_CONFIG_TABLE = "ai_model_config";
    private static final String TASK_TYPE_TABLE = "ai_task_type";
    private static final String OPERATION_INSERT = "INSERT";
    private static final String OPERATION_UPDATE = "UPDATE";
    private static final String OPERATION_DELETE = "DELETE";

    private final AuditService auditService;
    private final ModelConfigRepository modelConfigRepository;
    private final TaskTypeRepository taskTypeRepository;

    @Around("execution(* com.xbk.knowledge.trigger.http.ModelConfigController.createModel(..))")
    public Object aroundCreateModel(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        try {
            Long recordId = extractRecordId(result);
            if (recordId != null) {
                IdQuery idQuery = new IdQuery(recordId);
                ModelConfig newValue = modelConfigRepository
                        .findById(idQuery)
                        .orElse(null);
                auditService.recordAudit(MODEL_CONFIG_TABLE, recordId, OPERATION_INSERT, null, newValue);
            } else {
                log.warn("创建模型配置审计未记录，未解析到记录ID");
            }
        } catch (Exception ex) {
            log.error("记录模型配置创建审计失败", ex);
        }
        return result;
    }

    @Around("execution(* com.xbk.knowledge.trigger.http.ModelConfigController.updateModel(..))")
    public Object aroundUpdateModel(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Long recordId = extractRecordId(args);
        IdQuery idQuery = recordId == null ? null : new IdQuery(recordId);
        ModelConfig oldValue = idQuery == null ? null : modelConfigRepository
                .findById(idQuery)
                .orElse(null);
        try {
            Object result = joinPoint.proceed();
            try {
                if (recordId != null) {
                    ModelConfig newValue = modelConfigRepository
                            .findById(idQuery)
                            .orElse(null);
                    auditService.recordAudit(MODEL_CONFIG_TABLE, recordId, OPERATION_UPDATE, oldValue, newValue);
                } else {
                    log.warn("更新模型配置审计未记录，未解析到记录ID");
                }
            } catch (Exception ex) {
                log.error("记录模型配置更新审计失败，id: {}", recordId, ex);
            }
            return result;
        } catch (Throwable ex) {
            log.error("模型配置更新执行异常，审计未记录，id: {}", recordId, ex);
            throw ex;
        }
    }

    @Around("execution(* com.xbk.knowledge.trigger.http.ModelConfigController.deleteModel(..))")
    public Object aroundDeleteModel(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Long recordId = extractRecordId(args);
        IdQuery idQuery = recordId == null ? null : new IdQuery(recordId);
        ModelConfig oldValue = idQuery == null ? null : modelConfigRepository
                .findById(idQuery)
                .orElse(null);
        try {
            Object result = joinPoint.proceed();
            try {
                if (recordId != null) {
                    auditService.recordAudit(MODEL_CONFIG_TABLE, recordId, OPERATION_DELETE, oldValue, null);
                } else {
                    log.warn("删除模型配置审计未记录，未解析到记录ID");
                }
            } catch (Exception ex) {
                log.error("记录模型配置删除审计失败，id: {}", recordId, ex);
            }
            return result;
        } catch (Throwable ex) {
            log.error("模型配置删除执行异常，审计未记录，id: {}", recordId, ex);
            throw ex;
        }
    }

    @Around("execution(* com.xbk.knowledge.trigger.http.TaskTypeController.createTaskType(..))")
    public Object aroundCreateTaskType(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        try {
            Long recordId = extractRecordId(result);
            if (recordId != null) {
                IdQuery idQuery = new IdQuery(recordId);
                TaskType newValue = taskTypeRepository
                        .findById(idQuery)
                        .orElse(null);
                auditService.recordAudit(TASK_TYPE_TABLE, recordId, OPERATION_INSERT, null, newValue);
            } else {
                log.warn("创建任务类型审计未记录，未解析到记录ID");
            }
        } catch (Exception ex) {
            log.error("记录任务类型创建审计失败", ex);
        }
        return result;
    }

    @Around("execution(* com.xbk.knowledge.trigger.http.TaskTypeController.updateTaskType(..))")
    public Object aroundUpdateTaskType(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Long recordId = extractRecordId(args);
        IdQuery idQuery = recordId == null ? null : new IdQuery(recordId);
        TaskType oldValue = idQuery == null ? null : taskTypeRepository
                .findById(idQuery)
                .orElse(null);
        try {
            Object result = joinPoint.proceed();
            try {
                if (recordId != null) {
                    TaskType newValue = taskTypeRepository
                            .findById(idQuery)
                            .orElse(null);
                    auditService.recordAudit(TASK_TYPE_TABLE, recordId, OPERATION_UPDATE, oldValue, newValue);
                } else {
                    log.warn("更新任务类型审计未记录，未解析到记录ID");
                }
            } catch (Exception ex) {
                log.error("记录任务类型更新审计失败，id: {}", recordId, ex);
            }
            return result;
        } catch (Throwable ex) {
            log.error("任务类型更新执行异常，审计未记录，id: {}", recordId, ex);
            throw ex;
        }
    }

    @Around("execution(* com.xbk.knowledge.trigger.http.TaskTypeController.deleteTaskType(..))")
    public Object aroundDeleteTaskType(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Long recordId = extractRecordId(args);
        IdQuery idQuery = recordId == null ? null : new IdQuery(recordId);
        TaskType oldValue = idQuery == null ? null : taskTypeRepository
                .findById(idQuery)
                .orElse(null);
        try {
            Object result = joinPoint.proceed();
            try {
                if (recordId != null) {
                    auditService.recordAudit(TASK_TYPE_TABLE, recordId, OPERATION_DELETE, oldValue, null);
                } else {
                    log.warn("删除任务类型审计未记录，未解析到记录ID");
                }
            } catch (Exception ex) {
                log.error("记录任务类型删除审计失败，id: {}", recordId, ex);
            }
            return result;
        } catch (Throwable ex) {
            log.error("任务类型删除执行异常，审计未记录，id: {}", recordId, ex);
            throw ex;
        }
    }

    private Long extractRecordId(Object result) {
        if (!(result instanceof Result<?> resultWrapper)) {
            return null;
        }
        Integer resultCode = resultWrapper.getCode();
        if (!Objects.equals(resultCode, 200)) {
            return null;
        }
        Object data = resultWrapper.getData();
        if (data instanceof ModelConfigResponse response) {
            return response.getId();
        }
        if (data instanceof TaskTypeResponse response) {
            return response.getId();
        }
        return null;
    }

    private Long extractRecordId(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        Object firstArg = args[0];
        if (firstArg instanceof Long id) {
            return id;
        }
        return null;
    }
}
