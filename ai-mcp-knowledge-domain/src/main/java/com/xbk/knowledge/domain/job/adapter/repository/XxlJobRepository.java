package com.xbk.knowledge.domain.job.adapter.repository;

import com.xbk.knowledge.domain.job.model.entity.XxlJobInfo;
import com.xbk.knowledge.domain.job.model.entity.XxlJobLogDetail;
import com.xbk.knowledge.domain.job.model.entity.XxlJobLogInfo;
import com.xbk.knowledge.domain.job.model.valobj.XxlJobLogPageQuery;
import com.xbk.knowledge.domain.job.model.valobj.XxlJobPageQuery;
import com.xbk.knowledge.types.common.PageResult;

import java.util.List;

/**
 * XXL 任务仓储接口
 * 抽象调度任务的查询能力
 *
 * 职责：领域仓储接口，用于屏蔽基础设施细节
 * @author sxie
 */
public interface XxlJobRepository {

    /**
     * 分页查询 XXL 任务
     *
     * 控制响应大小，适配列表分页
     * 
     * @param query 分页查询条件。
     * @return 任务分页结果。
     */
    PageResult<XxlJobInfo> queryJobPage(XxlJobPageQuery query);

    /**
     * 查询全部 XXL 任务（用于下拉缓存）
     *
     * 提供下拉/缓存数据源
     * 
     * @param appName 应用名称。
     * @param refresh 是否刷新缓存。
     * @return 任务列表。
     */
    List<XxlJobInfo> queryAllJobs(String appName, boolean refresh);

    /**
     * 查询 XXL 任务详情
     *
     * 详情页需要单条任务信息
     * 
     * @param appName 应用名称。
     * @param jobId 标识 ID。
     * @return 任务详情。
     */
    XxlJobInfo queryJobDetail(String appName, Long jobId);

    /**
     * 创建 XXL 任务
     *
     * 统一任务创建入口
     * 
     * @param jobInfo 待创建的任务信息。
     * @return 创建后的任务 ID。
     */
    String createJob(XxlJobInfo jobInfo);

    /**
     * 更新 XXL 任务
     *
     * 统一任务更新入口
     * 
     * @param jobInfo 待更新的任务信息。
     */
    void updateJob(XxlJobInfo jobInfo);

    /**
     * 删除 XXL 任务
     *
     * 统一任务删除入口
     * 
     * @param jobId 标识 ID。
     */
    void removeJob(Long jobId);

    /**
     * 启动 XXL 任务
     *
     * 统一任务启动入口
     * 
     * @param jobId 标识 ID。
     */
    void startJob(Long jobId);

    /**
     * 停止 XXL 任务
     *
     * 统一任务停止入口
     * 
     * @param jobId 标识 ID。
     */
    void stopJob(Long jobId);

    /**
     * 手动触发 XXL 任务
     *
     * 支持手动触发执行
     * 
     * @param jobId 标识 ID。
     * @param executorParam 执行参数。
     * @param addressList 执行器地址列表。
     * @return 触发结果消息。
     */
    String triggerJob(Long jobId, String executorParam, String addressList);

    /**
     * 分页查询 XXL 任务日志
     *
     * 日志量大，需要分页
     * 
     * @param query 分页查询条件。
     * @return XxlJobLogInfo 分页结果。
     */
    PageResult<XxlJobLogInfo> queryJobLogPage(XxlJobLogPageQuery query);

    /**
     * 查询 XXL 任务日志详情
     *
     * 按行读取日志，支持增量加载
     * 
     * @param logId 日志 ID。
     * @param fromLineNum 起始行号。
     * @return 任务日志详情。
     */
    XxlJobLogDetail queryLogDetail(Long logId, Integer fromLineNum);
}
