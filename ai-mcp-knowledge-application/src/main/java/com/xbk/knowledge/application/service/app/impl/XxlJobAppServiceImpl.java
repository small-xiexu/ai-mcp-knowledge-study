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

    private final IXxlJobService xxlJobService;

    /**
     * 分页查询 XXL 任务
     *
     * 为什么：统一分页入口，隔离应用层与领域层协议
     * 入参：分页查询对象
     * 出参：分页结果
     */
    @Override
    public PageResult<XxlJobInfo> queryJobPage(XxlJobPageQuery query) {
        return xxlJobService.queryJobPage(query);
    }

    /**
     * 查询全部 XXL 任务
     *
     * 为什么：提供下拉或缓存初始化的数据源
     * 入参：执行器 appName、是否刷新
     * 出参：任务列表
     */
    @Override
    public List<XxlJobInfo> queryAllJobs(String appName, boolean refresh) {
        return xxlJobService.queryAllJobs(appName, refresh);
    }

    /**
     * 查询 XXL 任务详情
     *
     * 为什么：进入详情页时需要单条任务信息
     * 入参：执行器 appName、任务 ID
     * 出参：任务详情
     */
    @Override
    public XxlJobInfo queryJobDetail(String appName, Long jobId) {
        return xxlJobService.queryJobDetail(appName, jobId);
    }

    /**
     * 创建 XXL 任务
     *
     * 为什么：统一创建入口，便于后续审计与校验
     * 入参：任务实体
     * 出参：创建结果消息
     */
    @Override
    public String createJob(XxlJobInfo jobInfo) {
        return xxlJobService.createJob(jobInfo);
    }

    /**
     * 更新 XXL 任务
     *
     * 为什么：统一更新入口，便于后续审计与校验
     * 入参：任务实体
     * 出参：无
     */
    @Override
    public void updateJob(XxlJobInfo jobInfo) {
        xxlJobService.updateJob(jobInfo);
    }

    /**
     * 删除 XXL 任务
     *
     * 为什么：统一删除入口，便于后续审计与校验
     * 入参：任务 ID
     * 出参：无
     */
    @Override
    public void removeJob(Long jobId) {
        xxlJobService.removeJob(jobId);
    }

    /**
     * 启动 XXL 任务
     *
     * 为什么：统一启动入口，便于状态控制
     * 入参：任务 ID
     * 出参：无
     */
    @Override
    public void startJob(Long jobId) {
        xxlJobService.startJob(jobId);
    }

    /**
     * 停止 XXL 任务
     *
     * 为什么：统一停止入口，便于状态控制
     * 入参：任务 ID
     * 出参：无
     */
    @Override
    public void stopJob(Long jobId) {
        xxlJobService.stopJob(jobId);
    }

    /**
     * 手动触发 XXL 任务
     *
     * 为什么：支持手动触发，便于调试或立即执行
     * 入参：任务 ID、执行参数、指定地址
     * 出参：触发结果消息
     */
    @Override
    public String triggerJob(Long jobId, String executorParam, String addressList) {
        return xxlJobService.triggerJob(jobId, executorParam, addressList);
    }

    /**
     * 分页查询 XXL 任务日志
     *
     * 为什么：日志数量大，需要分页
     * 入参：分页查询对象
     * 出参：日志分页结果
     */
    @Override
    public PageResult<XxlJobLogInfo> queryJobLogPage(XxlJobLogPageQuery query) {
        return xxlJobService.queryJobLogPage(query);
    }

    /**
     * 查询 XXL 任务日志详情
     *
     * 为什么：支持按行查看日志详情
     * 入参：日志 ID、起始行
     * 出参：日志详情
     */
    @Override
    public XxlJobLogDetail queryLogDetail(Long logId, Integer fromLineNum) {
        return xxlJobService.queryLogDetail(logId, fromLineNum);
    }
}
