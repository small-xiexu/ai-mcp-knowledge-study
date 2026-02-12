package com.xbk.knowledge.infrastructure.audit;

import com.xbk.knowledge.domain.model.entity.gateway.McpGateway;
import com.xbk.knowledge.domain.model.entity.gateway.McpToolBinding;
import com.xbk.knowledge.domain.model.entity.gateway.McpToolRegistry;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.gateway.ToolBindingQuery;
import com.xbk.knowledge.domain.repository.gateway.McpGatewayRepository;
import com.xbk.knowledge.domain.repository.gateway.McpToolBindingRepository;
import com.xbk.knowledge.domain.repository.gateway.McpToolRegistryRepository;
import com.xbk.knowledge.types.common.Result;
import com.xbk.knowledge.types.enums.ToolBindType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Gateway 管理审计切面
 * 覆盖网关实例、工具管理、模型绑定等关键操作。
 *
 * @author xiexu
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class GatewayAuditAspect {

    private static final String GATEWAY_TABLE = "mcp_gateway";
    private static final String TOOL_TABLE = "mcp_tool_registry";
    private static final String BINDING_TABLE = "mcp_tool_binding";

    private static final String OP_GATEWAY_INSTANCE_CREATE = "GATEWAY_INSTANCE_CREATE";
    private static final String OP_GATEWAY_INSTANCE_UPDATE = "GATEWAY_INSTANCE_UPDATE";
    private static final String OP_GATEWAY_INSTANCE_DELETE = "GATEWAY_INSTANCE_DELETE";
    private static final String OP_GATEWAY_TOOL_CREATE = "GATEWAY_TOOL_CREATE";
    private static final String OP_GATEWAY_TOOL_UPDATE = "GATEWAY_TOOL_UPDATE";
    private static final String OP_GATEWAY_TOOL_DELETE = "GATEWAY_TOOL_DELETE";
    private static final String OP_GATEWAY_TOOL_ENABLE = "GATEWAY_TOOL_ENABLE";
    private static final String OP_GATEWAY_TOOL_DISABLE = "GATEWAY_TOOL_DISABLE";
    private static final String OP_GATEWAY_MODEL_BINDING_UPDATE = "GATEWAY_MODEL_BINDING_UPDATE";

    private final AuditService auditService;
    private final McpGatewayRepository gatewayRepository;
    private final McpToolRegistryRepository toolRegistryRepository;
    private final McpToolBindingRepository toolBindingRepository;

    @Around("execution(* com.xbk.knowledge.trigger.http.GatewayManageController.saveGatewayInstance(..))")
    public Object aroundSaveGatewayInstance(ProceedingJoinPoint joinPoint) throws Throwable {
        Long id = extractLongField(firstArg(joinPoint), "id");
        McpGateway oldValue = loadGateway(id);

        Object result = joinPoint.proceed();
        if (!isSuccess(result)) {
            return result;
        }

        try {
            McpGateway newValue = extractResultData(result, McpGateway.class);
            Long recordId = newValue == null ? id : newValue.getId();
            if (recordId == null) {
                log.warn("Gateway 实例审计未记录，未解析到记录ID");
                return result;
            }
            if (newValue == null) {
                newValue = loadGateway(recordId);
            }
            String operation = oldValue == null ? OP_GATEWAY_INSTANCE_CREATE : OP_GATEWAY_INSTANCE_UPDATE;
            auditService.recordAudit(GATEWAY_TABLE, recordId, operation, oldValue, newValue);
        } catch (Exception e) {
            log.error("记录 Gateway 实例审计失败，id: {}", id, e);
        }
        return result;
    }

    @Around("execution(* com.xbk.knowledge.trigger.http.GatewayManageController.deleteGatewayInstance(..))")
    public Object aroundDeleteGatewayInstance(ProceedingJoinPoint joinPoint) throws Throwable {
        Long id = extractIdQuery(firstArg(joinPoint));
        McpGateway oldValue = loadGateway(id);
        Object result = joinPoint.proceed();
        if (!isSuccess(result) || id == null) {
            return result;
        }

        try {
            auditService.recordAudit(GATEWAY_TABLE, id, OP_GATEWAY_INSTANCE_DELETE, oldValue, null);
        } catch (Exception e) {
            log.error("记录 Gateway 删除审计失败，id: {}", id, e);
        }
        return result;
    }

    @Around("execution(* com.xbk.knowledge.trigger.http.GatewayManageController.saveTool(..))")
    public Object aroundSaveTool(ProceedingJoinPoint joinPoint) throws Throwable {
        Long id = extractLongField(firstArg(joinPoint), "id");
        McpToolRegistry oldValue = loadTool(id);

        Object result = joinPoint.proceed();
        if (!isSuccess(result)) {
            return result;
        }

        try {
            McpToolRegistry newValue = extractResultData(result, McpToolRegistry.class);
            Long recordId = newValue == null ? id : newValue.getId();
            if (recordId == null) {
                log.warn("Gateway 工具审计未记录，未解析到记录ID");
                return result;
            }
            if (newValue == null) {
                newValue = loadTool(recordId);
            }
            String operation = oldValue == null ? OP_GATEWAY_TOOL_CREATE : OP_GATEWAY_TOOL_UPDATE;
            auditService.recordAudit(TOOL_TABLE, recordId, operation, oldValue, newValue);
        } catch (Exception e) {
            log.error("记录 Gateway 工具保存审计失败，id: {}", id, e);
        }
        return result;
    }

    @Around("execution(* com.xbk.knowledge.trigger.http.GatewayManageController.deleteTool(..))")
    public Object aroundDeleteTool(ProceedingJoinPoint joinPoint) throws Throwable {
        Long id = extractIdQuery(firstArg(joinPoint));
        McpToolRegistry oldValue = loadTool(id);

        Object result = joinPoint.proceed();
        if (!isSuccess(result) || id == null) {
            return result;
        }

        try {
            auditService.recordAudit(TOOL_TABLE, id, OP_GATEWAY_TOOL_DELETE, oldValue, null);
        } catch (Exception e) {
            log.error("记录 Gateway 工具删除审计失败，id: {}", id, e);
        }
        return result;
    }

    @Around("execution(* com.xbk.knowledge.trigger.http.GatewayManageController.enableTool(..))")
    public Object aroundEnableTool(ProceedingJoinPoint joinPoint) throws Throwable {
        return aroundToolStatusChange(joinPoint, OP_GATEWAY_TOOL_ENABLE);
    }

    @Around("execution(* com.xbk.knowledge.trigger.http.GatewayManageController.disableTool(..))")
    public Object aroundDisableTool(ProceedingJoinPoint joinPoint) throws Throwable {
        return aroundToolStatusChange(joinPoint, OP_GATEWAY_TOOL_DISABLE);
    }

    @Around("execution(* com.xbk.knowledge.trigger.http.GatewayManageController.saveModelBindings(..))")
    public Object aroundSaveModelBindings(ProceedingJoinPoint joinPoint) throws Throwable {
        Object request = firstArg(joinPoint);
        Long modelId = extractLongField(request, "modelId");
        List<McpToolBinding> oldValue = queryModelBindings(modelId);

        Object result = joinPoint.proceed();
        if (!isSuccess(result) || modelId == null) {
            return result;
        }

        try {
            List<McpToolBinding> newValue = queryModelBindings(modelId);
            auditService.recordAudit(BINDING_TABLE, modelId, OP_GATEWAY_MODEL_BINDING_UPDATE, oldValue, newValue);
        } catch (Exception e) {
            log.error("记录 Gateway 模型绑定审计失败，modelId: {}", modelId, e);
        }
        return result;
    }

    private Object aroundToolStatusChange(ProceedingJoinPoint joinPoint, String operation) throws Throwable {
        Long id = extractIdQuery(firstArg(joinPoint));
        McpToolRegistry oldValue = loadTool(id);
        Object result = joinPoint.proceed();
        if (!isSuccess(result) || id == null) {
            return result;
        }
        try {
            McpToolRegistry newValue = loadTool(id);
            auditService.recordAudit(TOOL_TABLE, id, operation, oldValue, newValue);
        } catch (Exception e) {
            log.error("记录 Gateway 工具状态审计失败，id: {}, operation: {}", id, operation, e);
        }
        return result;
    }

    private McpGateway loadGateway(Long id) {
        if (id == null) {
            return null;
        }
        return gatewayRepository.findById(new IdQuery(id)).orElse(null);
    }

    private McpToolRegistry loadTool(Long id) {
        if (id == null) {
            return null;
        }
        return toolRegistryRepository.findById(new IdQuery(id)).orElse(null);
    }

    private List<McpToolBinding> queryModelBindings(Long modelId) {
        if (modelId == null) {
            return new ArrayList<>();
        }
        List<McpToolBinding> bindings = toolBindingRepository.findByBindTypeAndTargetId(
                new ToolBindingQuery(ToolBindType.MODEL.name(), modelId)
        );
        return bindings == null ? new ArrayList<>() : bindings;
    }

    private boolean isSuccess(Object result) {
        if (!(result instanceof Result<?> wrapper)) {
            return false;
        }
        return Objects.equals(wrapper.getCode(), 200);
    }

    private Object firstArg(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return null;
        }
        return args[0];
    }

    private Long extractIdQuery(Object arg) {
        if (arg instanceof IdQuery query) {
            return query.getId();
        }
        return extractLongField(arg, "id");
    }

    private <T> T extractResultData(Object result, Class<T> type) {
        if (!(result instanceof Result<?> wrapper)) {
            return null;
        }
        Object data = wrapper.getData();
        if (type.isInstance(data)) {
            return type.cast(data);
        }
        return null;
    }

    private Long extractLongField(Object target, String fieldName) {
        if (target == null || !StringUtils.hasText(fieldName)) {
            return null;
        }
        Class<?> current = target.getClass();
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(target);
                if (value instanceof Number number) {
                    return number.longValue();
                }
                return null;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
