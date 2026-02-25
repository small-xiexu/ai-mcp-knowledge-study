package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.XxlJobAppService;
import com.xbk.knowledge.domain.job.model.entity.XxlJobInfo;
import com.xbk.knowledge.domain.job.model.entity.XxlJobLogDetail;
import com.xbk.knowledge.domain.job.model.entity.XxlJobLogInfo;
import com.xbk.knowledge.domain.job.model.valobj.XxlJobLogPageQuery;
import com.xbk.knowledge.domain.job.model.valobj.XxlJobPageQuery;
import com.xbk.knowledge.domain.job.service.IXxlJobService;
import com.xbk.knowledge.types.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * XXL 任务应用服务实现
 * 负责任务查询用例编排
 *
 * 职责：应用层用例实现，用于协调领域能力
 * @author sxie
 */
@Service
@RequiredArgsConstructor
public class XxlJobAppServiceImpl implements XxlJobAppService {

    /**
     * XXL 作业领域服务。
     */
    private final IXxlJobService xxlJobService;

    /**
     * 分页查询 XXL 任务
     *
     * 统一分页入口，隔离应用层与领域层协议
     * 
     * @param query 分页查询条件。
     * @return 任务分页结果。
     */
    @Override
    public PageResult<XxlJobInfo> queryJobPage(XxlJobPageQuery query) {
        return xxlJobService.queryJobPage(query);
    }

    /**
     * 查询全部 XXL 任务
     *
     * 提供下拉或缓存初始化的数据源
     * 
     * @param appName 应用名称。
     * @param refresh 是否刷新缓存。
     * @return 任务列表。
     */
    @Override
    public List<XxlJobInfo> queryAllJobs(String appName, boolean refresh) {
        return xxlJobService.queryAllJobs(appName, refresh);
    }

    /**
     * 查询 XXL 任务详情
     *
     * 进入详情页时需要单条任务信息
     * 
     * @param appName 应用名称。
     * @param jobId 标识 ID。
     * @return 任务详情。
     */
    @Override
    public XxlJobInfo queryJobDetail(String appName, Long jobId) {
        return xxlJobService.queryJobDetail(appName, jobId);
    }

    /**
     * 创建 XXL 任务
     *
     * 统一创建入口，便于后续审计与校验
     * 
     * @param jobInfo 待创建的任务信息。
     * @return 创建后的任务 ID。
     */
    @Override
    public String createJob(XxlJobInfo jobInfo) {
        return xxlJobService.createJob(jobInfo);
    }

    /**
     * 更新 XXL 任务
     *
     * 统一更新入口，便于后续审计与校验
     * 
     * @param jobInfo 待更新的任务信息。
     */
    @Override
    public void updateJob(XxlJobInfo jobInfo) {
        xxlJobService.updateJob(jobInfo);
    }

    /**
     * 删除 XXL 任务
     *
     * 统一删除入口，便于后续审计与校验
     * 
     * @param jobId 标识 ID。
     */
    @Override
    public void removeJob(Long jobId) {
        xxlJobService.removeJob(jobId);
    }

    /**
     * 启动 XXL 任务
     *
     * 统一启动入口，便于状态控制
     * 
     * @param jobId 标识 ID。
     */
    @Override
    public void startJob(Long jobId) {
        xxlJobService.startJob(jobId);
    }

    /**
     * 停止 XXL 任务
     *
     * 统一停止入口，便于状态控制
     * 
     * @param jobId 标识 ID。
     */
    @Override
    public void stopJob(Long jobId) {
        xxlJobService.stopJob(jobId);
    }

    /**
     * 手动触发 XXL 任务
     *
     * 支持手动触发，便于调试或立即执行
     * 
     * @param jobId 标识 ID。
     * @param executorParam 执行参数。
     * @param addressList 执行器地址列表。
     * @return 触发结果消息。
     */
    @Override
    public String triggerJob(Long jobId, String executorParam, String addressList) {
        return xxlJobService.triggerJob(jobId, executorParam, addressList);
    }

    /**
     * 分页查询 XXL 任务日志
     *
     * 日志数量大，需要分页
     * 
     * @param query 分页查询条件。
     * @return XxlJobLogInfo 分页结果。
     */
    @Override
    public PageResult<XxlJobLogInfo> queryJobLogPage(XxlJobLogPageQuery query) {
        return xxlJobService.queryJobLogPage(query);
    }

    /**
     * 查询 XXL 任务日志详情
     *
     * 支持按行查看日志详情
     * 
     * @param logId 日志 ID。
     * @param fromLineNum 起始行号。
     * @return 任务日志详情。
     */
    @Override
    public XxlJobLogDetail queryLogDetail(Long logId, Integer fromLineNum) {
        return xxlJobService.queryLogDetail(logId, fromLineNum);
    }
}
