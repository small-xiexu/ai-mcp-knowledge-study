package com.xbk.knowledge.infrastructure.audit;

import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.api.dto.model.ModelConfigResponse;
import com.xbk.knowledge.api.dto.mcp.McpServerConfigResponse;
import com.xbk.knowledge.api.dto.mcp.McpServerConfigRequest;
import com.xbk.knowledge.api.dto.common.IdRequest;
import com.xbk.knowledge.domain.llm.model.entity.ModelConfig;
import com.xbk.knowledge.domain.mcp.model.entity.McpServerConfig;
import com.xbk.knowledge.domain.mcp.adapter.repository.McpServerConfigRepository;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.llm.adapter.repository.ModelConfigRepository;
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
 * @author sxie
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {

    private static final String MODEL_CONFIG_TABLE = "ai_model_config";
    private static final String MCP_SERVER_CONFIG_TABLE = "ai_mcp_server_config";
    private static final String OPERATION_INSERT = "INSERT";
    private static final String OPERATION_UPDATE = "UPDATE";
    private static final String OPERATION_DELETE = "DELETE";

    private final AuditService auditService;
    private final ModelConfigRepository modelConfigRepository;
    private final McpServerConfigRepository mcpServerConfigRepository;

    /**
     * 对外暴露 aroundCreateModel 作为调用入口，便于上层复用。
     *
     * 为什么：创建后记录审计，避免遗漏
     * 入参：切点
     * 出参：原方法返回值
     */
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
                /*
                 * 目的：记录新增前后差异，新增时旧值为空
 */
                auditService.recordAudit(MODEL_CONFIG_TABLE, recordId, OPERATION_INSERT, null, newValue);
            } else {
                log.warn("创建模型配置审计未记录，未解析到记录ID");
            }
        } catch (Exception ex) {
            log.error("记录模型配置创建审计失败", ex);
        }
        return result;
    }

    /**
     * 对外暴露 aroundUpdateModel 作为调用入口，便于上层复用。
     *
     * 为什么：更新前后需要对比，记录变更内容
     * 入参：切点
     * 出参：原方法返回值
     */
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
                    /*
                     * 目的：记录更新前后差异
 */
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

    /**
     * 对外暴露 aroundDeleteModel 作为调用入口，便于上层复用。
     *
     * 为什么：删除前记录旧值，便于审计回溯
     * 入参：切点
     * 出参：原方法返回值
     */
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
                    /*
                     * 目的：删除时只记录旧值
 */
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

    /**
     * 对外暴露 aroundCreateMcpServerConfig 作为调用入口，便于上层复用。
     *
     * 为什么：创建后记录审计，避免遗漏
     * 入参：切点
     * 出参：原方法返回值
     */
    @Around("execution(* com.xbk.knowledge.trigger.http.McpServerConfigController.createConfig(..))")
    public Object aroundCreateMcpServerConfig(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        try {
            Long recordId = extractRecordId(result);
            if (recordId != null) {
                IdQuery idQuery = new IdQuery(recordId);
                McpServerConfig newValue = mcpServerConfigRepository
                        .findById(idQuery)
                        .orElse(null);
                /*
                 * 目的：记录新增前后差异，新增时旧值为空
 */
                auditService.recordAudit(MCP_SERVER_CONFIG_TABLE, recordId, OPERATION_INSERT, null, newValue);
            } else {
                log.warn("创建 MCP Server 配置审计未记录，未解析到记录ID");
            }
        } catch (Exception ex) {
            log.error("记录 MCP Server 配置创建审计失败", ex);
        }
        return result;
    }

    /**
     * 对外暴露 aroundUpdateMcpServerConfig 作为调用入口，便于上层复用。
     *
     * 为什么：更新前后需要对比，记录变更内容
     * 入参：切点
     * 出参：原方法返回值
     */
    @Around("execution(* com.xbk.knowledge.trigger.http.McpServerConfigController.updateConfig(..))")
    public Object aroundUpdateMcpServerConfig(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Long recordId = extractRecordId(args);
        IdQuery idQuery = recordId == null ? null : new IdQuery(recordId);
        McpServerConfig oldValue = idQuery == null ? null : mcpServerConfigRepository
                .findById(idQuery)
                .orElse(null);
        try {
            Object result = joinPoint.proceed();
            try {
                if (recordId != null) {
                    McpServerConfig newValue = mcpServerConfigRepository
                            .findById(idQuery)
                            .orElse(null);
                    /*
                     * 目的：记录更新前后差异
 */
                    auditService.recordAudit(MCP_SERVER_CONFIG_TABLE, recordId, OPERATION_UPDATE, oldValue, newValue);
                } else {
                    log.warn("更新 MCP Server 配置审计未记录，未解析到记录ID");
                }
            } catch (Exception ex) {
                log.error("记录 MCP Server 配置更新审计失败，id: {}", recordId, ex);
            }
            return result;
        } catch (Throwable ex) {
            log.error("MCP Server 配置更新执行异常，审计未记录，id: {}", recordId, ex);
            throw ex;
        }
    }

    /**
     * 对外暴露 aroundDeleteMcpServerConfig 作为调用入口，便于上层复用。
     *
     * 为什么：删除前记录旧值，便于审计回溯
     * 入参：切点
     * 出参：原方法返回值
     */
    @Around("execution(* com.xbk.knowledge.trigger.http.McpServerConfigController.deleteConfig(..))")
    public Object aroundDeleteMcpServerConfig(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Long recordId = extractRecordId(args);
        IdQuery idQuery = recordId == null ? null : new IdQuery(recordId);
        McpServerConfig oldValue = idQuery == null ? null : mcpServerConfigRepository
                .findById(idQuery)
                .orElse(null);
        try {
            Object result = joinPoint.proceed();
            try {
                if (recordId != null) {
                    /*
                     * 目的：删除时只记录旧值
 */
                    auditService.recordAudit(MCP_SERVER_CONFIG_TABLE, recordId, OPERATION_DELETE, oldValue, null);
                } else {
                    log.warn("删除 MCP Server 配置审计未记录，未解析到记录ID");
                }
            } catch (Exception ex) {
                log.error("记录 MCP Server 配置删除审计失败，id: {}", recordId, ex);
            }
            return result;
        } catch (Throwable ex) {
            log.error("MCP Server 配置删除执行异常，审计未记录，id: {}", recordId, ex);
            throw ex;
        }
    }

    /**
     * 从返回结果解析记录 ID
     *
     * 为什么：审计需要记录ID用于定位实体
     */
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
        if (data instanceof McpServerConfigResponse response) {
            return response.getId();
        }
        return null;
    }

    /**
     * 从方法入参解析记录 ID
     *
     * 为什么：删除/更新场景 ID 通常在请求参数中
     */
    private Long extractRecordId(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        Object firstArg = args[0];
        if (firstArg instanceof Long id) {
            return id;
        }
        if (firstArg instanceof IdRequest request) {
            return request.getId();
        }
        if (firstArg instanceof McpServerConfigRequest request) {
            return request.getId();
        }
        return null;
    }
}
