package com.xbk.knowledge.domain.service;

import com.xbk.knowledge.domain.model.entity.XxlJobInfo;
import com.xbk.knowledge.domain.model.entity.XxlJobLogDetail;
import com.xbk.knowledge.domain.model.entity.XxlJobLogInfo;
import com.xbk.knowledge.domain.model.vo.xxl.XxlJobLogPageQuery;
import com.xbk.knowledge.domain.model.vo.xxl.XxlJobPageQuery;
import com.xbk.knowledge.types.common.PageResult;

/**
 * XXL 任务领域服务
 * 封装任务分页查询的业务规则
 *
 * 职责：领域服务接口，用于表达核心用例
 * @author xiexu
 */
public interface IXxlJobService {

    /**
     * 分页查询 XXL 任务
     *
     * 为什么：统一分页查询能力入口
     * 入参：分页查询条件
     * 出参：分页结果
     */
    PageResult<XxlJobInfo> queryJobPage(XxlJobPageQuery query);

    /**
     * 查询全部 XXL 任务（用于下拉缓存）
     *
     * 为什么：提供下拉或缓存数据源
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
     * 为什么：统一创建入口以保障规则一致
     * 入参：任务信息
     * 出参：创建结果内容
     */
    String createJob(XxlJobInfo jobInfo);

    /**
     * 更新 XXL 任务
     *
     * 为什么：统一更新入口以保障规则一致
     * 入参：任务信息
     * 出参：无
     */
    void updateJob(XxlJobInfo jobInfo);

    /**
     * 删除 XXL 任务
     *
     * 为什么：统一删除入口以保障规则一致
     * 入参：任务 ID
     * 出参：无
     */
    void removeJob(Long jobId);

    /**
     * 启动 XXL 任务
     *
     * 为什么：统一启动入口以保障规则一致
     * 入参：任务 ID
     * 出参：无
     */
    void startJob(Long jobId);

    /**
     * 停止 XXL 任务
     *
     * 为什么：统一停止入口以保障规则一致
     * 入参：任务 ID
     * 出参：无
     */
    void stopJob(Long jobId);

    /**
     * 手动触发 XXL 任务
     *
     * 为什么：支持即时触发执行
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
