package com.xbk.knowledge.infrastructure.audit;

import com.xbk.knowledge.domain.gateway.model.entity.McpGateway;
import com.xbk.knowledge.domain.gateway.model.entity.McpGatewayAuth;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolBinding;
import com.xbk.knowledge.domain.gateway.model.entity.McpToolRegistry;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolBindingQuery;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpGatewayAuthRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpGatewayRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolBindingRepository;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolRegistryRepository;
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
 * 覆盖网关实例、工具管理、模型绑定等关键操作，自动记录变更前后快照
 *
 * 用 AOP审计逻辑与业务逻辑正交，通过切面拦截 Controller 方法，
 * 在操作前后分别加载旧值/新值，交由 AuditService 持久化审计日志
 *
 * @author sxie
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class GatewayAuditAspect {

    /**
     * 网关实例表名。
     */
    private static final String GATEWAY_TABLE = "mcp_gateway";

    /**
     * 网关鉴权表名。
     */
    private static final String GATEWAY_AUTH_TABLE = "mcp_gateway_auth";

    /**
     * 工具注册表名。
     */
    private static final String TOOL_TABLE = "mcp_tool_registry";

    /**
     * 工具绑定表名。
     */
    private static final String BINDING_TABLE = "mcp_tool_binding";

    /**
     * 网关实例创建操作码。
     */
    private static final String OP_GATEWAY_INSTANCE_CREATE = "GATEWAY_INSTANCE_CREATE";

    /**
     * 网关实例更新操作码。
     */
    private static final String OP_GATEWAY_INSTANCE_UPDATE = "GATEWAY_INSTANCE_UPDATE";

    /**
     * 网关实例删除操作码。
     */
    private static final String OP_GATEWAY_INSTANCE_DELETE = "GATEWAY_INSTANCE_DELETE";

    /**
     * 网关鉴权创建操作码。
     */
    private static final String OP_GATEWAY_AUTH_CREATE = "GATEWAY_AUTH_CREATE";

    /**
     * 网关鉴权更新操作码。
     */
    private static final String OP_GATEWAY_AUTH_UPDATE = "GATEWAY_AUTH_UPDATE";

    /**
     * 网关鉴权启用操作码。
     */
    private static final String OP_GATEWAY_AUTH_ENABLE = "GATEWAY_AUTH_ENABLE";

    /**
     * 网关鉴权禁用操作码。
     */
    private static final String OP_GATEWAY_AUTH_DISABLE = "GATEWAY_AUTH_DISABLE";

    /**
     * 网关工具创建操作码。
     */
    private static final String OP_GATEWAY_TOOL_CREATE = "GATEWAY_TOOL_CREATE";

    /**
     * 网关工具更新操作码。
     */
    private static final String OP_GATEWAY_TOOL_UPDATE = "GATEWAY_TOOL_UPDATE";

    /**
     * 网关工具删除操作码。
     */
    private static final String OP_GATEWAY_TOOL_DELETE = "GATEWAY_TOOL_DELETE";

    /**
     * 网关工具启用操作码。
     */
    private static final String OP_GATEWAY_TOOL_ENABLE = "GATEWAY_TOOL_ENABLE";

    /**
     * 网关工具禁用操作码。
     */
    private static final String OP_GATEWAY_TOOL_DISABLE = "GATEWAY_TOOL_DISABLE";

    /**
     * 网关模型绑定更新操作码。
     */
    private static final String OP_GATEWAY_MODEL_BINDING_UPDATE = "GATEWAY_MODEL_BINDING_UPDATE";

    /**
     * 审计记录服务。
     */
    private final AuditService auditService;

    /**
     * 网关实例仓储。
     */
    private final McpGatewayRepository gatewayRepository;

    /**
     * 网关鉴权仓储。
     */
    private final McpGatewayAuthRepository gatewayAuthRepository;

    /**
     * 工具注册仓储。
     */
    private final McpToolRegistryRepository toolRegistryRepository;

    /**
     * 工具绑定仓储。
     */
    private final McpToolBindingRepository toolBindingRepository;

    /**
     * 审计网关实例新增/更新
     * 
     * @param joinPoint AOP 切点信息。
     * @return 方法执行结果。
     */
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

    /**
     * 审计网关实例删除
     * 
     * @param joinPoint AOP 切点信息。
     * @return 方法执行结果。
     */
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

    /**
     * 审计网关凭证新增/更新
     * 
     * @param joinPoint AOP 切点信息。
     * @return 方法执行结果。
     */
     @Around("execution(* com.xbk.knowledge.trigger.http.GatewayManageController.saveGatewayAuth(..))")
    public Object aroundSaveGatewayAuth(ProceedingJoinPoint joinPoint) throws Throwable {
        Long id = extractLongField(firstArg(joinPoint), "id");
        McpGatewayAuth oldValue = loadGatewayAuth(id);

        Object result = joinPoint.proceed();
        if (!isSuccess(result)) {
            return result;
        }

        try {
            McpGatewayAuth newValue = extractResultData(result, McpGatewayAuth.class);
            Long recordId = newValue == null ? id : newValue.getId();
            if (recordId == null) {
                log.warn("Gateway 鉴权审计未记录，未解析到记录ID");
                return result;
            }
            if (newValue == null) {
                newValue = loadGatewayAuth(recordId);
            }
            String operation = oldValue == null ? OP_GATEWAY_AUTH_CREATE : OP_GATEWAY_AUTH_UPDATE;
            auditService.recordAudit(GATEWAY_AUTH_TABLE, recordId, operation, oldValue, newValue);
        } catch (Exception e) {
            log.error("记录 Gateway 鉴权保存审计失败，id: {}", id, e);
        }
        return result;
    }

    /**
     * 审计网关凭证启用
     * 
     * @param joinPoint AOP 切点信息。
     * @return 方法执行结果。
     */
     @Around("execution(* com.xbk.knowledge.trigger.http.GatewayManageController.enableGatewayAuth(..))")
    public Object aroundEnableGatewayAuth(ProceedingJoinPoint joinPoint) throws Throwable {
        return aroundGatewayAuthStatusChange(joinPoint, OP_GATEWAY_AUTH_ENABLE);
    }

    /**
     * 审计网关凭证禁用
     * 
     * @param joinPoint AOP 切点信息。
     * @return 方法执行结果。
     */
     @Around("execution(* com.xbk.knowledge.trigger.http.GatewayManageController.disableGatewayAuth(..))")
    public Object aroundDisableGatewayAuth(ProceedingJoinPoint joinPoint) throws Throwable {
        return aroundGatewayAuthStatusChange(joinPoint, OP_GATEWAY_AUTH_DISABLE);
    }

    /**
     * 审计工具新增/更新
     * 
     * @param joinPoint AOP 切点信息。
     * @return 方法执行结果。
     */
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

    /**
     * 审计工具删除
     * 
     * @param joinPoint AOP 切点信息。
     * @return 方法执行结果。
     */
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

    /**
     * 审计工具启用
     * 
     * @param joinPoint AOP 切点信息。
     * @return 方法执行结果。
     */
     @Around("execution(* com.xbk.knowledge.trigger.http.GatewayManageController.enableTool(..))")
    public Object aroundEnableTool(ProceedingJoinPoint joinPoint) throws Throwable {
        return aroundToolStatusChange(joinPoint, OP_GATEWAY_TOOL_ENABLE);
    }

    /**
     * 审计工具禁用
     * 
     * @param joinPoint AOP 切点信息。
     * @return 方法执行结果。
     */
     @Around("execution(* com.xbk.knowledge.trigger.http.GatewayManageController.disableTool(..))")
    public Object aroundDisableTool(ProceedingJoinPoint joinPoint) throws Throwable {
        return aroundToolStatusChange(joinPoint, OP_GATEWAY_TOOL_DISABLE);
    }

    /**
     * 审计模型绑定关系变更
     * 
     * @param joinPoint AOP 切点信息。
     * @return 方法执行结果。
     */
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

    /**
     * 通用工具状态变更审计（启用/禁用共用）
     * 
     * @param joinPoint AOP 切点信息。
     * @param operation 操作类型。
     * @return 方法执行结果。
     */
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

    /**
     * 通用网关凭证状态变更审计（启用/禁用共用）
     * 
     * @param joinPoint AOP 切点信息。
     * @param operation 操作类型。
     * @return 方法执行结果。
     */
     private Object aroundGatewayAuthStatusChange(ProceedingJoinPoint joinPoint, String operation) throws Throwable {
        Long id = extractIdQuery(firstArg(joinPoint));
        McpGatewayAuth oldValue = loadGatewayAuth(id);
        Object result = joinPoint.proceed();
        if (!isSuccess(result) || id == null) {
            return result;
        }
        try {
            McpGatewayAuth newValue = loadGatewayAuth(id);
            auditService.recordAudit(GATEWAY_AUTH_TABLE, id, operation, oldValue, newValue);
        } catch (Exception e) {
            log.error("记录 Gateway 鉴权状态审计失败，id: {}, operation: {}", id, operation, e);
        }
        return result;
    }

    /**
     * 按主键加载网关实例快照（审计前置）
     * 
     * @param id 主键 ID。
     * @return 网关快照。
     */
     private McpGateway loadGateway(Long id) {
        if (id == null) {
            return null;
        }
        return gatewayRepository.findById(new IdQuery(id)).orElse(null);
    }

    /**
     * 按主键加载工具注册快照（审计前置）
     * 
     * @param id 主键 ID。
     * @return 工具注册快照。
     */
     private McpToolRegistry loadTool(Long id) {
        if (id == null) {
            return null;
        }
        return toolRegistryRepository.findById(new IdQuery(id)).orElse(null);
    }

    /**
     * 按主键加载网关鉴权快照（审计前置）
     * 
     * @param id 主键 ID。
     * @return 网关鉴权快照。
     */
     private McpGatewayAuth loadGatewayAuth(Long id) {
        if (id == null) {
            return null;
        }
        return gatewayAuthRepository.findById(id).orElse(null);
    }

    /**
     * 查询指定模型的工具绑定列表（审计前后对比用）
     * 
     * @param modelId 模型 ID。
     * @return McpToolBinding 列表。
     */
     private List<McpToolBinding> queryModelBindings(Long modelId) {
        if (modelId == null) {
            return new ArrayList<>();
        }
        List<McpToolBinding> bindings = toolBindingRepository.findByBindTypeAndTargetId(
                new ToolBindingQuery(ToolBindType.MODEL.name(), modelId)
        );
        return bindings == null ? new ArrayList<>() : bindings;
    }

    /**
     * 判断 Controller 返回结果是否成功（code == 200）
     * 
     * @param result 控制器返回结果。
     * @return `true` 表示操作成功，`false` 表示操作失败。
     */
     private boolean isSuccess(Object result) {
        if (!(result instanceof Result<?> wrapper)) {
            return false;
        }
        return Objects.equals(wrapper.getCode(), 200);
    }

    /**
     * 提取切点方法的第一个参数
     * 
     * @param joinPoint AOP 切点信息。
     * @return 方法执行结果。
     */
     private Object firstArg(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return null;
        }
        return args[0];
    }

    /**
     * 从参数中提取 IdQuery 的 id 值
     * 
     * @param arg 待解析参数。
     * @return 数值型结果。
     */
     private Long extractIdQuery(Object arg) {
        if (arg instanceof IdQuery query) {
            return query.getId();
        }
        return extractLongField(arg, "id");
    }

    /**
     * 从 Result 包装中提取指定类型的 data 对象
     * 
     * @param result 控制器返回结果。
     * @param type 消息类型。
     * @return 方法执行结果。
     */
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

    /**
     * 通过反射提取对象中指定名称的 Long 字段值
     * 
     * @param target 目标数据。
     * @param fieldName 字段名。
     * @return 数值型结果。
     */
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
