package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.domain.job.model.entity.XxlJobInfo;
import com.xbk.knowledge.domain.job.model.entity.XxlJobLogDetail;
import com.xbk.knowledge.domain.job.model.entity.XxlJobLogInfo;
import com.xbk.knowledge.domain.job.model.valobj.XxlJobLogPageQuery;
import com.xbk.knowledge.domain.job.model.valobj.XxlJobPageQuery;
import com.xbk.knowledge.types.common.PageResult;

import java.util.List;

/**
 * XXL 任务应用服务
 * 负责任务查询用例编排
 *
 * 职责：应用服务接口，用于对外暴露用例能力
 * @author sxie
 */
public interface XxlJobAppService {

    /**
     * 分页查询 XXL 任务
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<XxlJobInfo> queryJobPage(XxlJobPageQuery query);

    /**
     * 查询全部 XXL 任务（用于下拉缓存）
     *
     * @param appName 执行器名称
     * @param refresh 是否强制刷新缓存
     * @return 任务列表
     */
    List<XxlJobInfo> queryAllJobs(String appName, boolean refresh);

    /**
     * 查询 XXL 任务详情
     *
     * @param appName 执行器名称
     * @param jobId 任务 ID
     * @return 任务详情
     */
    XxlJobInfo queryJobDetail(String appName, Long jobId);

    /**
     * 创建 XXL 任务
     *
     * @param jobInfo 任务信息
     * @return 创建结果内容
     */
    String createJob(XxlJobInfo jobInfo);

    /**
     * 更新 XXL 任务
     *
     * @param jobInfo 任务信息
     */
    void updateJob(XxlJobInfo jobInfo);

    /**
     * 删除 XXL 任务
     *
     * @param jobId 任务 ID
     */
    void removeJob(Long jobId);

    /**
     * 启动 XXL 任务
     *
     * @param jobId 任务 ID
     */
    void startJob(Long jobId);

    /**
     * 停止 XXL 任务
     *
     * @param jobId 任务 ID
     */
    void stopJob(Long jobId);

    /**
     * 手动触发 XXL 任务
     *
     * @param jobId 任务 ID
     * @param executorParam 执行参数
     * @param addressList 指定机器列表
     */
    String triggerJob(Long jobId, String executorParam, String addressList);

    /**
     * 分页查询 XXL 任务日志
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<XxlJobLogInfo> queryJobLogPage(XxlJobLogPageQuery query);

    /**
     * 查询 XXL 任务日志详情
     *
     * @param logId 日志 ID
     * @param fromLineNum 起始行号
     * @return 日志详情
     */
    XxlJobLogDetail queryLogDetail(Long logId, Integer fromLineNum);
}
