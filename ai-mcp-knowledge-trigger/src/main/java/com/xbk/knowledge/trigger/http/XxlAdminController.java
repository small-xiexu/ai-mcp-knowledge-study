package com.xbk.knowledge.trigger.http;

import com.xbk.knowledge.api.IXxlAdminService;
import com.xbk.knowledge.api.dto.xxl.XxlJobCreateRequest;
import com.xbk.knowledge.api.dto.xxl.XxlJobDetailRequest;
import com.xbk.knowledge.api.dto.xxl.XxlJobDetailResponse;
import com.xbk.knowledge.api.dto.xxl.XxlJobListRequest;
import com.xbk.knowledge.api.dto.xxl.XxlJobLogDetailRequest;
import com.xbk.knowledge.api.dto.xxl.XxlJobLogDetailResponse;
import com.xbk.knowledge.api.dto.xxl.XxlJobLogListRequest;
import com.xbk.knowledge.api.dto.xxl.XxlJobLogResponse;
import com.xbk.knowledge.api.dto.xxl.XxlJobOptionRequest;
import com.xbk.knowledge.api.dto.xxl.XxlJobOperateRequest;
import com.xbk.knowledge.api.dto.xxl.XxlJobResponse;
import com.xbk.knowledge.api.dto.xxl.XxlJobTriggerRequest;
import com.xbk.knowledge.api.dto.xxl.XxlJobUpdateRequest;
import com.xbk.knowledge.application.service.app.XxlJobAppService;
import com.xbk.knowledge.config.XxlAdminProperties;
import com.xbk.knowledge.domain.job.model.entity.XxlJobInfo;
import com.xbk.knowledge.domain.job.model.entity.XxlJobLogDetail;
import com.xbk.knowledge.domain.job.model.entity.XxlJobLogInfo;
import com.xbk.knowledge.domain.job.model.valobj.XxlJobLogPageQuery;
import com.xbk.knowledge.domain.job.model.valobj.XxlJobPageQuery;
import com.xbk.knowledge.trigger.security.XxlPermissionGuard;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.common.PageResultConverter;
import com.xbk.knowledge.types.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * XXL 调度中心 Controller
 * 负责接收 HTTP 请求，调用应用服务，转换响应
 *
 * 职责：HTTP 接口适配，用于转发应用层能力
 * @author sxie
 */
@Slf4j
@RestController
@RequestMapping("/api/xxl")
@RequiredArgsConstructor
public class XxlAdminController implements IXxlAdminService {

    private final XxlJobAppService xxlJobAppService;
    private final XxlAdminProperties xxlAdminProperties;
    private final XxlPermissionGuard xxlPermissionGuard;

    /**
     * 查询 XXL 任务列表（分页）
     *
     * 为什么：统一入口管理任务分页，便于权限与 appName 约束集中处理。
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    @PostMapping("/jobs/list")
    @Override
    public Result<PageResult<XxlJobResponse>> listJobs(@Valid @RequestBody XxlJobListRequest request) {
        xxlPermissionGuard.assertCanView();
        String appName = resolveAppName(request.getAppName());
        Integer pageNum = request.getPageNum();
        Integer pageSize = request.getPageSize();
        XxlJobPageQuery query = new XxlJobPageQuery(appName, pageNum, pageSize);
        PageResult<XxlJobInfo> pageResult = xxlJobAppService.queryJobPage(query);

        PageResult<XxlJobResponse> result = PageResultConverter.convert(pageResult, this::convertJobResponse);
        return Result.success(result);
    }

    /**
     * 查询 XXL 任务下拉列表
     *
     * 为什么：通过缓存统一任务选项，避免前端反复分页拉取。
     *
     * @param request 查询请求
     * @return 任务列表
     */
    @PostMapping("/jobs/options")
    @Override
    public Result<List<XxlJobResponse>> listJobOptions(@RequestBody(required = false) XxlJobOptionRequest request) {
        xxlPermissionGuard.assertCanView();
        String appName = resolveAppName(null);
        boolean refresh = request != null && Boolean.TRUE.equals(request.getRefresh());
        List<XxlJobInfo> jobs = xxlJobAppService.queryAllJobs(appName, refresh);
        List<XxlJobResponse> responses = new ArrayList<>();
        for (XxlJobInfo job : jobs) {
            responses.add(convertJobResponse(job));
        }
        return Result.success(responses);
    }

    /**
     * 查询 XXL 任务详情
     *
     * 为什么：编辑/查看需要完整详情，统一从后端校验与转换。
     *
     * @param request 查询请求
     * @return 任务详情
     */
    @PostMapping("/jobs/detail")
    @Override
    public Result<XxlJobDetailResponse> getJobDetail(@Valid @RequestBody XxlJobDetailRequest request) {
        xxlPermissionGuard.assertCanView();
        String appName = resolveAppName(null);
        XxlJobInfo jobInfo = xxlJobAppService.queryJobDetail(appName, request.getId());
        return Result.success(convertJobDetailResponse(jobInfo));
    }

    /**
     * 创建 XXL 任务
     *
     * 为什么：创建前统一校验与字段白名单控制，避免越权字段写入。
     *
     * @param request 创建请求
     * @return 创建结果
     */
    @PostMapping("/jobs/create")
    @Override
    public Result<String> createJob(@Valid @RequestBody XxlJobCreateRequest request) {
        xxlPermissionGuard.assertCanEdit();
        XxlJobInfo jobInfo = buildJobInfo(request);
        String result = xxlJobAppService.createJob(jobInfo);
        return Result.success("任务创建成功", result);
    }

    /**
     * 更新 XXL 任务
     *
     * 为什么：更新需要保持字段一致性，统一从后端对接 xxl-admin。
     *
     * @param request 更新请求
     * @return 更新结果
     */
    @PostMapping("/jobs/update")
    @Override
    public Result<Void> updateJob(@Valid @RequestBody XxlJobUpdateRequest request) {
        xxlPermissionGuard.assertCanEdit();
        XxlJobInfo jobInfo = buildJobInfo(request);
        jobInfo.setId(request.getId());
        xxlJobAppService.updateJob(jobInfo);
        return Result.success("任务更新成功", null);
    }

    /**
     * 删除 XXL 任务
     *
     * 为什么：删除属高风险操作，集中权限与操作日志入口。
     *
     * @param request 删除请求
     * @return 删除结果
     */
    @PostMapping("/jobs/remove")
    @Override
    public Result<Void> removeJob(@Valid @RequestBody XxlJobOperateRequest request) {
        xxlPermissionGuard.assertCanEdit();
        xxlJobAppService.removeJob(request.getId());
        return Result.success("任务删除成功", null);
    }

    /**
     * 启动 XXL 任务
     *
     * 为什么：启动任务需校验权限并统一调用调度中心接口。
     *
     * @param request 启动请求
     * @return 启动结果
     */
    @PostMapping("/jobs/start")
    @Override
    public Result<Void> startJob(@Valid @RequestBody XxlJobOperateRequest request) {
        xxlPermissionGuard.assertCanEdit();
        xxlJobAppService.startJob(request.getId());
        return Result.success("任务启动成功", null);
    }

    /**
     * 停止 XXL 任务
     *
     * 为什么：停止任务避免误触发，统一走后端校验与调用链。
     *
     * @param request 停止请求
     * @return 停止结果
     */
    @PostMapping("/jobs/stop")
    @Override
    public Result<Void> stopJob(@Valid @RequestBody XxlJobOperateRequest request) {
        xxlPermissionGuard.assertCanEdit();
        xxlJobAppService.stopJob(request.getId());
        return Result.success("任务停止成功", null);
    }

    /**
     * 手动触发 XXL 任务
     *
     * 为什么：手动触发需审计与权限控制，避免绕过调度策略。
     *
     * @param request 触发请求
     * @return 触发结果
     */
    @PostMapping("/jobs/trigger")
    @Override
    public Result<String> triggerJob(@Valid @RequestBody XxlJobTriggerRequest request) {
        xxlPermissionGuard.assertCanEdit();
        String result = xxlJobAppService.triggerJob(request.getId(), request.getExecutorParam(), request.getAddressList());
        return Result.success("任务触发成功", result);
    }

    /**
     * 查询 XXL 任务日志列表（分页）
     *
     * 为什么：日志查询统一由后端过滤 appName，避免越权查看。
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    @PostMapping("/logs/list")
    @Override
    public Result<PageResult<XxlJobLogResponse>> listLogs(@Valid @RequestBody XxlJobLogListRequest request) {
        xxlPermissionGuard.assertCanView();
        String appName = resolveAppName(null);
        XxlJobLogPageQuery query = new XxlJobLogPageQuery(
                appName,
                request.getJobId(),
                request.getStartTime(),
                request.getEndTime(),
                request.getPageNum(),
                request.getPageSize()
        );
        PageResult<XxlJobLogInfo> pageResult = xxlJobAppService.queryJobLogPage(query);

        PageResult<XxlJobLogResponse> result = PageResultConverter.convert(pageResult, this::convertLogResponse);
        return Result.success(result);
    }

    /**
     * 查询 XXL 任务日志详情
     *
     * 为什么：日志内容需按行拉取，统一由后端做参数校验与调用。
     *
     * @param request 查询请求
     * @return 日志详情
     */
    @PostMapping("/logs/detail")
    @Override
    public Result<XxlJobLogDetailResponse> getLogDetail(@Valid @RequestBody XxlJobLogDetailRequest request) {
        xxlPermissionGuard.assertCanView();
        XxlJobLogDetail detail = xxlJobAppService.queryLogDetail(request.getLogId(), request.getFromLineNum());
        return Result.success(convertLogDetailResponse(detail));
    }

    private String resolveAppName(String requestAppName) {
        String configured = xxlAdminProperties.getAppName();
        if (!StringUtils.hasText(configured)) {
            throw new IllegalStateException("xxl.admin.app-name 未配置");
        }
        if (StringUtils.hasText(requestAppName) && !configured.equals(requestAppName)) {
            throw new IllegalArgumentException("执行器 AppName 不允许切换");
        }
        return configured;
    }

    private XxlJobResponse convertJobResponse(XxlJobInfo jobInfo) {
        return XxlJobResponse.builder()
                .id(jobInfo.getId())
                .jobDesc(jobInfo.getJobDesc())
                .executorHandler(jobInfo.getExecutorHandler())
                .executorParam(jobInfo.getExecutorParam())
                .scheduleConf(jobInfo.getScheduleConf())
                .executorRouteStrategy(jobInfo.getExecutorRouteStrategy())
                .triggerStatus(jobInfo.getTriggerStatus())
                .author(jobInfo.getAuthor())
                .addTime(jobInfo.getAddTime())
                .updateTime(jobInfo.getUpdateTime())
                .build();
    }

    private XxlJobDetailResponse convertJobDetailResponse(XxlJobInfo jobInfo) {
        return XxlJobDetailResponse.builder()
                .id(jobInfo.getId())
                .jobGroup(jobInfo.getJobGroup())
                .jobDesc(jobInfo.getJobDesc())
                .author(jobInfo.getAuthor())
                .alarmEmail(jobInfo.getAlarmEmail())
                .scheduleType(jobInfo.getScheduleType())
                .scheduleConf(jobInfo.getScheduleConf())
                .misfireStrategy(jobInfo.getMisfireStrategy())
                .executorRouteStrategy(jobInfo.getExecutorRouteStrategy())
                .executorHandler(jobInfo.getExecutorHandler())
                .executorParam(jobInfo.getExecutorParam())
                .executorBlockStrategy(jobInfo.getExecutorBlockStrategy())
                .executorTimeout(jobInfo.getExecutorTimeout())
                .executorFailRetryCount(jobInfo.getExecutorFailRetryCount())
                .glueType(jobInfo.getGlueType())
                .childJobId(jobInfo.getChildJobId())
                .triggerStatus(jobInfo.getTriggerStatus())
                .triggerLastTime(jobInfo.getTriggerLastTime())
                .triggerNextTime(jobInfo.getTriggerNextTime())
                .build();
    }

    private XxlJobLogResponse convertLogResponse(XxlJobLogInfo logInfo) {
        return XxlJobLogResponse.builder()
                .id(logInfo.getId())
                .jobId(logInfo.getJobId())
                .executorAddress(logInfo.getExecutorAddress())
                .executorHandler(logInfo.getExecutorHandler())
                .executorParam(logInfo.getExecutorParam())
                .executorShardingParam(logInfo.getExecutorShardingParam())
                .executorFailRetryCount(logInfo.getExecutorFailRetryCount())
                .triggerTime(logInfo.getTriggerTime())
                .triggerCode(logInfo.getTriggerCode())
                .triggerMsg(logInfo.getTriggerMsg())
                .handleTime(logInfo.getHandleTime())
                .handleCode(logInfo.getHandleCode())
                .handleMsg(logInfo.getHandleMsg())
                .alarmStatus(logInfo.getAlarmStatus())
                .build();
    }

    private XxlJobLogDetailResponse convertLogDetailResponse(XxlJobLogDetail detail) {
        return XxlJobLogDetailResponse.builder()
                .fromLineNum(detail.getFromLineNum())
                .toLineNum(detail.getToLineNum())
                .logContent(detail.getLogContent())
                .end(detail.getEnd())
                .build();
    }

    private XxlJobInfo buildJobInfo(XxlJobCreateRequest request) {
        return XxlJobInfo.builder()
                .jobDesc(request.getJobDesc())
                .author(request.getAuthor())
                .alarmEmail(request.getAlarmEmail())
                .scheduleType(request.getScheduleType())
                .scheduleConf(request.getScheduleConf())
                .misfireStrategy(request.getMisfireStrategy())
                .executorRouteStrategy(request.getExecutorRouteStrategy())
                .executorHandler(request.getExecutorHandler())
                .executorParam(request.getExecutorParam())
                .executorBlockStrategy(request.getExecutorBlockStrategy())
                .executorTimeout(request.getExecutorTimeout())
                .executorFailRetryCount(request.getExecutorFailRetryCount())
                .glueType(request.getGlueType())
                .childJobId(request.getChildJobId())
                .build();
    }

    private XxlJobInfo buildJobInfo(XxlJobUpdateRequest request) {
        return XxlJobInfo.builder()
                .jobDesc(request.getJobDesc())
                .author(request.getAuthor())
                .alarmEmail(request.getAlarmEmail())
                .scheduleType(request.getScheduleType())
                .scheduleConf(request.getScheduleConf())
                .misfireStrategy(request.getMisfireStrategy())
                .executorRouteStrategy(request.getExecutorRouteStrategy())
                .executorHandler(request.getExecutorHandler())
                .executorParam(request.getExecutorParam())
                .executorBlockStrategy(request.getExecutorBlockStrategy())
                .executorTimeout(request.getExecutorTimeout())
                .executorFailRetryCount(request.getExecutorFailRetryCount())
                .glueType(request.getGlueType())
                .childJobId(request.getChildJobId())
                .build();
    }
}
