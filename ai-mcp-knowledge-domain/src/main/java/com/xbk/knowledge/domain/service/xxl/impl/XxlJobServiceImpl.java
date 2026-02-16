package com.xbk.knowledge.domain.service.xxl.impl;

import com.xbk.knowledge.domain.model.entity.XxlJobInfo;
import com.xbk.knowledge.domain.model.entity.XxlJobLogDetail;
import com.xbk.knowledge.domain.model.entity.XxlJobLogInfo;
import com.xbk.knowledge.domain.model.vo.xxl.XxlJobLogPageQuery;
import com.xbk.knowledge.domain.model.vo.xxl.XxlJobPageQuery;
import com.xbk.knowledge.domain.repository.xxl.XxlJobRepository;
import com.xbk.knowledge.domain.service.xxl.IXxlJobService;
import com.xbk.knowledge.types.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * XXL 任务领域服务实现
 * 封装分页查询的校验与统一口径
 *
 * 职责：领域服务实现，用于封装业务规则
 * @author xiexu
 */
@Service
@RequiredArgsConstructor
public class XxlJobServiceImpl implements IXxlJobService {

    private final XxlJobRepository xxlJobRepository;

    /**
     * 分页查询 XXL 任务
     *
     * 为什么：统一分页参数口径，避免下游查询异常
     * 入参：分页查询对象
     * 出参：分页结果
     */
    @Override
    public PageResult<XxlJobInfo> queryJobPage(XxlJobPageQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("分页查询条件不能为空");
        }
        Integer pageNum = query.getPageNum();
        Integer pageSize = query.getPageSize();
        /*
         * 目的：规范化分页参数，避免超大分页导致性能问题
         */
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
     * 为什么：提供下拉或缓存初始化的数据来源
     * 入参：执行器 AppName、是否刷新
     * 出参：任务列表
     */
    @Override
    public java.util.List<XxlJobInfo> queryAllJobs(String appName, boolean refresh) {
        if (appName == null || appName.trim().isEmpty()) {
            throw new IllegalArgumentException("执行器 AppName 不能为空");
        }
        return xxlJobRepository.queryAllJobs(appName, refresh);
    }

    /**
     * 查询 XXL 任务详情
     *
     * 为什么：详情页需要单条任务信息
     * 入参：执行器 AppName、任务 ID
     * 出参：任务详情
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
     * 为什么：统一创建入口，便于规则校验
     * 入参：任务实体
     * 出参：创建结果消息
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
     * 为什么：统一更新入口，便于规则校验
     * 入参：任务实体
     * 出参：无
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
     * 为什么：统一删除入口，便于规则校验
     * 入参：任务 ID
     * 出参：无
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
     * 为什么：统一启动入口，便于状态控制
     * 入参：任务 ID
     * 出参：无
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
     * 为什么：统一停止入口，便于状态控制
     * 入参：任务 ID
     * 出参：无
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
     * 为什么：支持即时触发，用于调试或临时执行
     * 入参：任务 ID、执行参数、执行地址
     * 出参：触发结果消息
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
     * 为什么：日志量大，需要分页以控制响应体积
     * 入参：日志分页查询对象
     * 出参：日志分页结果
     */
    @Override
    public PageResult<XxlJobLogInfo> queryJobLogPage(XxlJobLogPageQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("日志查询条件不能为空");
        }
        Integer pageNum = query.getPageNum();
        Integer pageSize = query.getPageSize();
        /*
         * 目的：规范化分页参数，避免超大分页影响性能
         */
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
     * 为什么：按行分页查看日志，支持增量读取
     * 入参：日志 ID、起始行
     * 出参：日志详情
     */
    @Override
    public XxlJobLogDetail queryLogDetail(Long logId, Integer fromLineNum) {
        if (logId == null || logId <= 0) {
            throw new IllegalArgumentException("日志 ID 不能为空");
        }
        /*
         * 目的：起始行兜底，避免负数导致接口异常
         */
        Integer startLine = fromLineNum == null || fromLineNum < 0 ? 0 : fromLineNum;
        return xxlJobRepository.queryLogDetail(logId, startLine);
    }
}
