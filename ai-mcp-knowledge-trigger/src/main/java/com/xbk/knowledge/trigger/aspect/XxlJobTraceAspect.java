package com.xbk.knowledge.trigger.aspect;

import com.xbk.knowledge.trigger.alert.XxlJobAlertHook;
import com.xbk.knowledge.types.trace.TraceIdUtils;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * XXL-Job TraceId 切面
 * 统一为定时任务注入 traceId，并在结束时清理
 *
 * 职责：AOP 切面，用于统一链路追踪上下文
 * @author xiexu
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class XxlJobTraceAspect {

    private final XxlJobAlertHook xxlJobAlertHook;

    /**
     * 拦截所有 @XxlJob 方法，统一注入 traceId
     *
     * 为什么：让所有定时任务共享同一套链路追踪、耗时统计与异常告警逻辑，避免重复实现。
     *
     * @param joinPoint 切点
     * @param xxlJob 任务注解（用于获取 handler 名称）
     * @return 执行结果
     * @throws Throwable 原始异常
     */
    @Around("@annotation(xxlJob)")
    public Object aroundXxlJob(ProceedingJoinPoint joinPoint, XxlJob xxlJob) throws Throwable {
        
        TraceIdUtils.TraceIdContext traceIdContext = TraceIdUtils.ensureTraceId();
        boolean generated = traceIdContext.isGenerated();
        String jobHandler = xxlJob.value();
        long startTime = System.currentTimeMillis();
        
        log.info("XXL-Job 开始执行, handler: {}", jobHandler);
        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - startTime;
            
            log.info("XXL-Job 执行完成, handler: {}, costMs: {}", jobHandler, cost);
            return result;
        } catch (Throwable throwable) {
            long cost = System.currentTimeMillis() - startTime;
            
            log.error("XXL-Job 执行异常, handler: {}, costMs: {}", jobHandler, cost, throwable);
            xxlJobAlertHook.onJobError(jobHandler, throwable);
            throw throwable;
        } finally {
            
            TraceIdUtils.clearIfGenerated(generated);
        }
    }
}
