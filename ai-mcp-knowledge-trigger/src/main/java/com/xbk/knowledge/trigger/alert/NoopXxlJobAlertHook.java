package com.xbk.knowledge.trigger.alert;

import org.springframework.stereotype.Component;

/**
 * XXL 任务告警空实现
 * 避免未接入告警系统时报错
 *
 * 职责：默认占位实现，确保告警链路可用
 * @author sxie
 */
@Component
public class NoopXxlJobAlertHook implements XxlJobAlertHook {

    /**
     * 任务执行异常告警
     *
     * 为什么：提供默认空实现，避免未配置告警时影响主流程。
     *
     * @param jobHandler 任务 Handler 名称
     * @param throwable 异常
     */
    @Override
    public void onJobError(String jobHandler, Throwable throwable) {
        // 默认不处理，后续可替换为实际告警实现
    }
}
