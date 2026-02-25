package com.xbk.knowledge.trigger.alert;

/**
 * XXL 任务告警钩子
 * 统一处理定时任务异常通知
 *
 * 职责：告警扩展点，用于后续接入通知系统
 * @author sxie
 */
public interface XxlJobAlertHook {

    /**
     * 任务执行异常告警
     *
     * 在异常发生时统一触发外部告警通道，避免任务静默失败。
     * 
     * @param jobHandler 任务 Handler 名称
     * @param throwable 异常
     */
    void onJobError(String jobHandler, Throwable throwable);
}
