package com.xbk.knowledge.domain.job.service;

import com.xbk.knowledge.domain.job.model.entity.XxlJobInfo;
import com.xbk.knowledge.domain.job.model.entity.XxlJobLogDetail;
import com.xbk.knowledge.domain.job.model.entity.XxlJobLogInfo;
import com.xbk.knowledge.domain.job.model.valobj.XxlJobLogPageQuery;
import com.xbk.knowledge.domain.job.model.valobj.XxlJobPageQuery;
import com.xbk.knowledge.types.common.PageResult;

import java.util.List;

/**
 * XXL 任务领域服务
 * 封装任务分页查询的业务规则
 *
 * 职责：领域服务接口，用于表达核心用例
 * @author sxie
 */
public interface IXxlJobService {

    /**
     * 分页查询 XXL 任务
     *
     * 统一分页查询能力入口
     * 
     * @param query 分页查询条件。
     * @return 任务分页结果。
     */
    PageResult<XxlJobInfo> queryJobPage(XxlJobPageQuery query);

    /**
     * 查询全部 XXL 任务（用于下拉缓存）
     *
     * 提供下拉或缓存数据源
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
     * 统一创建入口以保障规则一致
     * 
     * @param jobInfo 待创建的任务信息。
     * @return 创建后的任务 ID。
     */
    String createJob(XxlJobInfo jobInfo);

    /**
     * 更新 XXL 任务
     *
     * 统一更新入口以保障规则一致
     * 
     * @param jobInfo 待更新的任务信息。
     */
    void updateJob(XxlJobInfo jobInfo);

    /**
     * 删除 XXL 任务
     *
     * 统一删除入口以保障规则一致
     * 
     * @param jobId 标识 ID。
     */
    void removeJob(Long jobId);

    /**
     * 启动 XXL 任务
     *
     * 统一启动入口以保障规则一致
     * 
     * @param jobId 标识 ID。
     */
    void startJob(Long jobId);

    /**
     * 停止 XXL 任务
     *
     * 统一停止入口以保障规则一致
     * 
     * @param jobId 标识 ID。
     */
    void stopJob(Long jobId);

    /**
     * 手动触发 XXL 任务
     *
     * 支持即时触发执行
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
