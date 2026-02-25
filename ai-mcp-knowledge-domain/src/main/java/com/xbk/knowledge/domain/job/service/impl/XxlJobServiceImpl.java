package com.xbk.knowledge.domain.job.service.impl;

import com.xbk.knowledge.domain.job.model.entity.XxlJobInfo;
import com.xbk.knowledge.domain.job.model.entity.XxlJobLogDetail;
import com.xbk.knowledge.domain.job.model.entity.XxlJobLogInfo;
import com.xbk.knowledge.domain.job.model.valobj.XxlJobLogPageQuery;
import com.xbk.knowledge.domain.job.model.valobj.XxlJobPageQuery;
import com.xbk.knowledge.domain.job.adapter.repository.XxlJobRepository;
import com.xbk.knowledge.domain.job.service.IXxlJobService;
import com.xbk.knowledge.types.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * XXL 任务领域服务实现
 * 封装分页查询的校验与统一口径
 *
 * 职责：领域服务实现，用于封装业务规则
 * @author sxie
 */
@Service
@RequiredArgsConstructor
public class XxlJobServiceImpl implements IXxlJobService {

    /**
     * XXL 作业仓储。
     */
    private final XxlJobRepository xxlJobRepository;

    /**
     * 分页查询 XXL 任务
     *
     * 统一分页参数口径，避免下游查询异常
     * 
     * @param query 分页查询条件。
     * @return 任务分页结果。
     */
    @Override
    public PageResult<XxlJobInfo> queryJobPage(XxlJobPageQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("分页查询条件不能为空");
        }
        Integer pageNum = query.getPageNum();
        Integer pageSize = query.getPageSize();
        // 规范化分页参数，避免超大分页导致性能问题
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        } else if (pageSize > 100) {
            pageSize = 100;
        }
        XxlJobPageQuery pageQuery = new XxlJobPageQuery(query.getAppName(), pageNum, pageSize);
        return xxlJobRepository.queryJobPage(pageQuery);
    }

    /**
     * 查询全部 XXL 任务
     *
     * 提供下拉或缓存初始化的数据来源
     * 
     * @param appName 应用名称。
     * @param refresh 是否刷新缓存。
     * @return 任务列表。
     */
    @Override
    public List<XxlJobInfo> queryAllJobs(String appName, boolean refresh) {
        if (appName == null || appName.trim().isEmpty()) {
            throw new IllegalArgumentException("执行器 AppName 不能为空");
        }
        return xxlJobRepository.queryAllJobs(appName, refresh);
    }

    /**
     * 查询 XXL 任务详情
     *
     * 详情页需要单条任务信息
     * 
     * @param appName 应用名称。
     * @param jobId 标识 ID。
     * @return 任务详情。
     */
    @Override
    public XxlJobInfo queryJobDetail(String appName, Long jobId) {
        if (jobId == null || jobId <= 0) {
            throw new IllegalArgumentException("任务 ID 不能为空");
        }
        return xxlJobRepository.queryJobDetail(appName, jobId);
    }

    /**
     * 创建 XXL 任务
     *
     * 统一创建入口，便于规则校验
     * 
     * @param jobInfo 待创建的任务信息。
     * @return 创建后的任务 ID。
     */
    @Override
    public String createJob(XxlJobInfo jobInfo) {
        if (jobInfo == null) {
            throw new IllegalArgumentException("任务信息不能为空");
        }
        return xxlJobRepository.createJob(jobInfo);
    }

    /**
     * 更新 XXL 任务
     *
     * 统一更新入口，便于规则校验
     * 
     * @param jobInfo 待更新的任务信息。
     */
    @Override
    public void updateJob(XxlJobInfo jobInfo) {
        if (jobInfo == null || jobInfo.getId() == null) {
            throw new IllegalArgumentException("任务信息不能为空");
        }
        xxlJobRepository.updateJob(jobInfo);
    }

    /**
     * 删除 XXL 任务
     *
     * 统一删除入口，便于规则校验
     * 
     * @param jobId 标识 ID。
     */
    @Override
    public void removeJob(Long jobId) {
        if (jobId == null || jobId <= 0) {
            throw new IllegalArgumentException("任务 ID 不能为空");
        }
        xxlJobRepository.removeJob(jobId);
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
        if (jobId == null || jobId <= 0) {
            throw new IllegalArgumentException("任务 ID 不能为空");
        }
        xxlJobRepository.startJob(jobId);
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
        if (jobId == null || jobId <= 0) {
            throw new IllegalArgumentException("任务 ID 不能为空");
        }
        xxlJobRepository.stopJob(jobId);
    }

    /**
     * 手动触发 XXL 任务
     *
     * 支持即时触发，用于调试或临时执行
     * 
     * @param jobId 标识 ID。
     * @param executorParam 执行参数。
     * @param addressList 执行器地址列表。
     * @return 触发结果消息。
     */
    @Override
    public String triggerJob(Long jobId, String executorParam, String addressList) {
        if (jobId == null || jobId <= 0) {
            throw new IllegalArgumentException("任务 ID 不能为空");
        }
        return xxlJobRepository.triggerJob(jobId, executorParam, addressList);
    }

    /**
     * 分页查询 XXL 任务日志
     *
     * 日志量大，需要分页以控制响应体积
     * 
     * @param query 分页查询条件。
     * @return XxlJobLogInfo 分页结果。
     */
    @Override
    public PageResult<XxlJobLogInfo> queryJobLogPage(XxlJobLogPageQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("日志查询条件不能为空");
        }
        Integer pageNum = query.getPageNum();
        Integer pageSize = query.getPageSize();
        // 规范化分页参数，避免超大分页影响性能
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        } else if (pageSize > 100) {
            pageSize = 100;
        }
        XxlJobLogPageQuery pageQuery = new XxlJobLogPageQuery(
                query.getAppName(),
                query.getJobId(),
                query.getStartTime(),
                query.getEndTime(),
                pageNum,
                pageSize
        );
        return xxlJobRepository.queryJobLogPage(pageQuery);
    }

    /**
     * 查询 XXL 任务日志详情
     *
     * 按行分页查看日志，支持增量读取
     * 
     * @param logId 日志 ID。
     * @param fromLineNum 起始行号。
     * @return 任务日志详情。
     */
    @Override
    public XxlJobLogDetail queryLogDetail(Long logId, Integer fromLineNum) {
        if (logId == null || logId <= 0) {
            throw new IllegalArgumentException("日志 ID 不能为空");
        }
        // 起始行兜底，避免负数导致接口异常
        Integer startLine = fromLineNum == null || fromLineNum < 0 ? 0 : fromLineNum;
        return xxlJobRepository.queryLogDetail(logId, startLine);
    }
}
