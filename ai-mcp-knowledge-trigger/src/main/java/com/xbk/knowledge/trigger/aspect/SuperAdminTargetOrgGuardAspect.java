package com.xbk.knowledge.trigger.aspect;

import com.xbk.knowledge.types.context.OrgContext;
import com.xbk.knowledge.types.context.OrgContextHolder;
import com.xbk.knowledge.types.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 超级管理员跨组织治理门禁切面。
 *
 * 目标：防止超级管理员在未显式选择目标组织（X-Target-Org-Id）时发生误写。
 *
 * 规则：
 * - 普通用户：不受影响
 * - 超级管理员：对“写操作”必须显式选择 targetOrgId（OrgContext.explicitTargetOrg=true）
 *
 * 说明：
 * - 读操作不强制要求选择 targetOrgId（由产品/前端决定是否提示）
 * - 写操作的定义采用“控制面关键接口白名单”，避免误伤 list/get 这类 POST 读接口
 *
 * @author xiexu
 */
@Slf4j
@Aspect
@Component
public class SuperAdminTargetOrgGuardAspect {

    private static final String TIP = "超级管理员请先选择目标组织（请求头 X-Target-Org-Id）";

    @Around(
            // Agent 控制面写操作
            "execution(* com.xbk.knowledge.trigger.http.AgentController.create(..)) || " +
            "execution(* com.xbk.knowledge.trigger.http.AgentController.update(..)) || " +
            // AgentVersion 控制面写操作
            "execution(* com.xbk.knowledge.trigger.http.AgentVersionController.saveDraft(..)) || " +
            "execution(* com.xbk.knowledge.trigger.http.AgentVersionController.publish(..)) || " +
            "execution(* com.xbk.knowledge.trigger.http.AgentVersionController.rollback(..)) || " +
            // PromptTemplate 控制面写操作
            "execution(* com.xbk.knowledge.trigger.http.PromptTemplateController.create(..)) || " +
            "execution(* com.xbk.knowledge.trigger.http.PromptTemplateController.update(..)) || " +
            "execution(* com.xbk.knowledge.trigger.http.PromptTemplateController.publish(..)) || " +
            "execution(* com.xbk.knowledge.trigger.http.PromptTemplateController.archive(..)) || " +
            // Schedule 控制面写操作
            "execution(* com.xbk.knowledge.trigger.http.AgentScheduleController.create(..)) || " +
            "execution(* com.xbk.knowledge.trigger.http.AgentScheduleController.update(..)) || " +
            "execution(* com.xbk.knowledge.trigger.http.AgentScheduleController.enable(..)) || " +
            "execution(* com.xbk.knowledge.trigger.http.AgentScheduleController.disable(..)) || " +
            "execution(* com.xbk.knowledge.trigger.http.AgentScheduleController.remove(..)) || " +
            // Approval 决策是强治理写操作
            "execution(* com.xbk.knowledge.trigger.http.ApprovalController.approve(..)) || " +
            "execution(* com.xbk.knowledge.trigger.http.ApprovalController.reject(..))"
    )
    public Object requireExplicitTargetOrgForSuperAdmin(ProceedingJoinPoint joinPoint) throws Throwable {
        OrgContext ctx = OrgContextHolder.get();
        if (ctx != null && ctx.superAdmin() && !ctx.explicitTargetOrg()) {
            // 业务治理门禁：拒绝执行，避免误写到 operatorOrgId
            log.warn("阻止超级管理员写操作：未显式选择目标组织，signature={}", joinPoint.getSignature());
            throw new BusinessException(TIP);
        }
        return joinPoint.proceed();
    }
}

