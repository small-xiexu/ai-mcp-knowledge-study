package com.xbk.knowledge.api;

import com.xbk.knowledge.api.dto.xxl.XxlJobCreateRequest;
import com.xbk.knowledge.api.dto.xxl.XxlJobDetailRequest;
import com.xbk.knowledge.api.dto.xxl.XxlJobDetailResponse;
import com.xbk.knowledge.api.dto.xxl.XxlJobListRequest;
import com.xbk.knowledge.api.dto.xxl.XxlJobLogDetailRequest;
import com.xbk.knowledge.api.dto.xxl.XxlJobLogDetailResponse;
import com.xbk.knowledge.api.dto.xxl.XxlJobLogListRequest;
import com.xbk.knowledge.api.dto.xxl.XxlJobLogResponse;
import com.xbk.knowledge.api.dto.xxl.XxlJobOperateRequest;
import com.xbk.knowledge.api.dto.xxl.XxlJobOptionRequest;
import com.xbk.knowledge.api.dto.xxl.XxlJobResponse;
import com.xbk.knowledge.api.dto.xxl.XxlJobTriggerRequest;
import com.xbk.knowledge.api.dto.xxl.XxlJobUpdateRequest;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.Result;

import java.util.List;

/**
 * XXL-Job 管理服务接口
 * 定义 XXL-Job 调度管理的 API 契约
 *
 * 职责：接口契约，用于规范 Trigger 层对外服务
 * @author sxie
 */
public interface IXxlAdminService {

    /**
     * 分页查询任务列表。
     *
     * @param request 请求参数
     * @return 列表结果
     */
    Result<PageResult<XxlJobResponse>> listJobs(XxlJobListRequest request);

    /**
     * 查询任务选项列表。
     *
     * @param request 请求参数
     * @return 列表结果
     */
    Result<List<XxlJobResponse>> listJobOptions(XxlJobOptionRequest request);

    /**
     * 查询任务详情。
     *
     * @param request 请求参数
     * @return 查询结果
     */
    Result<XxlJobDetailResponse> getJobDetail(XxlJobDetailRequest request);

    /**
     * 创建任务。
     *
     * @param request 请求参数
     * @return 创建结果
     */
    Result<String> createJob(XxlJobCreateRequest request);

    /**
     * 更新任务。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<Void> updateJob(XxlJobUpdateRequest request);

    /**
     * 删除任务。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<Void> removeJob(XxlJobOperateRequest request);

    /**
     * 启动任务。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<Void> startJob(XxlJobOperateRequest request);

    /**
     * 停止任务。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    Result<Void> stopJob(XxlJobOperateRequest request);

    /**
     * 手动触发任务。
     *
     * @param request 请求参数
     * @return 任务结果
     */
    Result<String> triggerJob(XxlJobTriggerRequest request);

    /**
     * 分页查询任务日志。
     *
     * @param request 请求参数
     * @return 列表结果
     */
    Result<PageResult<XxlJobLogResponse>> listLogs(XxlJobLogListRequest request);

    /**
     * 查询日志详情。
     *
     * @param request 请求参数
     * @return 查询结果
     */
    Result<XxlJobLogDetailResponse> getLogDetail(XxlJobLogDetailRequest request);
}
