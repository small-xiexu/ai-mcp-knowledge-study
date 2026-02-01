package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.entity.XxlJobInfo;
import com.xbk.knowledge.domain.model.entity.XxlJobLogDetail;
import com.xbk.knowledge.domain.model.entity.XxlJobLogInfo;
import com.xbk.knowledge.domain.model.vo.xxl.XxlJobLogPageQuery;
import com.xbk.knowledge.domain.model.vo.xxl.XxlJobPageQuery;
import com.xbk.knowledge.types.common.PageResult;

/**
 * XXL 任务仓储接口
 * 抽象调度任务的查询能力
 *
 * 职责：领域仓储接口，用于屏蔽基础设施细节
 * @author xiexu
 */
public interface XxlJobRepository {

    /**
     * 分页查询 XXL 任务
     *
     * 为什么：控制响应大小，适配列表分页
     * 入参：分页查询条件
     * 出参：分页结果
     */
    PageResult<XxlJobInfo> queryJobPage(XxlJobPageQuery query);

    /**
     * 查询全部 XXL 任务（用于下拉缓存）
     *
     * 为什么：提供下拉/缓存数据源
     * 入参：执行器名称、是否强制刷新缓存
     * 出参：任务列表
     */
    java.util.List<XxlJobInfo> queryAllJobs(String appName, boolean refresh);

    /**
     * 查询 XXL 任务详情
     *
     * 为什么：详情页需要单条任务信息
     * 入参：执行器名称、任务 ID
     * 出参：任务详情
     */
    XxlJobInfo queryJobDetail(String appName, Long jobId);

    /**
     * 创建 XXL 任务
     *
     * 为什么：统一任务创建入口
     * 入参：任务信息
     * 出参：创建结果内容
     */
    String createJob(XxlJobInfo jobInfo);

    /**
     * 更新 XXL 任务
     *
     * 为什么：统一任务更新入口
     * 入参：任务信息
     * 出参：无
     */
    void updateJob(XxlJobInfo jobInfo);

    /**
     * 删除 XXL 任务
     *
     * 为什么：统一任务删除入口
     * 入参：任务 ID
     * 出参：无
     */
    void removeJob(Long jobId);

    /**
     * 启动 XXL 任务
     *
     * 为什么：统一任务启动入口
     * 入参：任务 ID
     * 出参：无
     */
    void startJob(Long jobId);

    /**
     * 停止 XXL 任务
     *
     * 为什么：统一任务停止入口
     * 入参：任务 ID
     * 出参：无
     */
    void stopJob(Long jobId);

    /**
     * 手动触发 XXL 任务
     *
     * 为什么：支持手动触发执行
     * 入参：任务 ID、执行参数、指定机器列表
     * 出参：触发结果内容
     */
    String triggerJob(Long jobId, String executorParam, String addressList);

    /**
     * 分页查询 XXL 任务日志
     *
     * 为什么：日志量大，需要分页
     * 入参：分页查询条件
     * 出参：分页结果
     */
    PageResult<XxlJobLogInfo> queryJobLogPage(XxlJobLogPageQuery query);

    /**
     * 查询 XXL 任务日志详情
     *
     * 为什么：按行读取日志，支持增量加载
     * 入参：日志 ID、起始行号
     * 出参：日志详情
     */
    XxlJobLogDetail queryLogDetail(Long logId, Integer fromLineNum);
}
