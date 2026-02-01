package com.xbk.knowledge.infrastructure.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.config.XxlAdminProperties;
import com.xbk.knowledge.domain.model.entity.XxlJobInfo;
import com.xbk.knowledge.domain.model.entity.XxlJobLogDetail;
import com.xbk.knowledge.domain.model.entity.XxlJobLogInfo;
import com.xbk.knowledge.domain.model.vo.xxl.XxlJobLogPageQuery;
import com.xbk.knowledge.domain.model.vo.xxl.XxlJobPageQuery;
import com.xbk.knowledge.domain.repository.XxlJobRepository;
import com.xbk.knowledge.infrastructure.redis.key.XxlJobRedisKeys;
import com.xbk.knowledge.types.common.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * XXL 任务仓储实现
 * 通过 WebClient 调用 xxl-admin 接口并统一处理登录态
 *
 * 职责：基础设施实现，负责对接外部调度中心
 * @author xiexu
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class XxlJobRepositoryImpl implements XxlJobRepository {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int DETAIL_PAGE_SIZE = 200;
    private static final String DEFAULT_SCHEDULE_TYPE = "CRON";
    private static final String DEFAULT_MISFIRE_STRATEGY = "DO_NOTHING";
    private static final String DEFAULT_ROUTE_STRATEGY = "FIRST";
    private static final String DEFAULT_BLOCK_STRATEGY = "SERIAL_EXECUTION";
    private static final String DEFAULT_GLUE_TYPE = "BEAN";

    private final WebClient.Builder webClientBuilder;
    private final StringRedisTemplate stringRedisTemplate;
    private final XxlAdminProperties xxlAdminProperties;
    private final ObjectMapper objectMapper;

    private volatile WebClient webClient;

    /**
     * 分页查询 XXL 任务
     * 自动解析执行器 ID 并调用任务分页接口
     *
     * 为什么：统一分页入口并复用执行器解析逻辑
     * 入参：分页查询条件
     * 出参：分页结果
     */
    @Override
    public PageResult<XxlJobInfo> queryJobPage(XxlJobPageQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("查询条件不能为空");
        }
        String appName = query.getAppName();
        if (!StringUtils.hasText(appName)) {
            throw new IllegalArgumentException("执行器 AppName 不能为空");
        }
        Integer pageNum = query.getPageNum();
        Integer pageSize = query.getPageSize();
        /*
         * 目的：规范化分页参数，避免异常分页导致接口报错
         */
        int safePageNum = pageNum == null ? 1 : pageNum;
        int safePageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        int start = (safePageNum - 1) * safePageSize;

        /*
         * 目的：先解析执行器 ID，再调用任务分页接口
         */
        Long jobGroupId = resolveJobGroupId(appName);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("jobGroup", String.valueOf(jobGroupId));
        form.add("start", String.valueOf(start));
        form.add("length", String.valueOf(safePageSize));
        form.add("triggerStatus", "-1");

        JsonNode response = postFormForJson("/jobinfo/pageList", form);
        Long totalValue = readLong(response, "recordsTotal", "total");
        JsonNode dataNode = extractDataNode(response);
        List<XxlJobInfo> jobs = parseJobs(dataNode);
        long total = totalValue == null ? jobs.size() : totalValue;

        return PageResult.of(jobs, total, safePageNum, safePageSize);
    }

    /**
     * 查询全部 XXL 任务（带缓存）
     *
     * 为什么：提供下拉数据源并减少对 xxl-admin 的调用
     * 入参：执行器名称、是否刷新缓存
     * 出参：任务列表
     */
    @Override
    public List<XxlJobInfo> queryAllJobs(String appName, boolean refresh) {
        String cacheKey = XxlJobRedisKeys.JOB_CACHE_PREFIX + appName;
        if (!refresh) {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.hasText(cached)) {
                try {
                    return objectMapper.readValue(cached, objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, XxlJobInfo.class));
                } catch (Exception e) {
                    log.warn("任务缓存解析失败，key: {}", cacheKey, e);
                    stringRedisTemplate.delete(cacheKey);
                }
            }
        }

        /*
         * 目的：缓存未命中或强刷时回源查询
         */
        List<XxlJobInfo> allJobs = fetchAllJobsFromXxl(appName);
        Integer ttlSeconds = xxlAdminProperties.getJobCacheTtlSeconds();
        int ttl = ttlSeconds == null ? 600 : ttlSeconds;
        try {
            String json = objectMapper.writeValueAsString(allJobs);
            stringRedisTemplate.opsForValue().set(cacheKey, json, Duration.ofSeconds(ttl));
        } catch (Exception e) {
            log.warn("任务缓存写入失败，key: {}", cacheKey, e);
        }
        return allJobs;
    }

    /**
     * 查询 XXL 任务详情
     * 通过分页接口扫描并匹配任务 ID
     *
     * 为什么：xxl-admin 无直接详情接口，需通过分页扫描
     * 入参：执行器名称、任务 ID
     * 出参：任务详情
     */
    @Override
    public XxlJobInfo queryJobDetail(String appName, Long jobId) {
        if (!StringUtils.hasText(appName)) {
            throw new IllegalArgumentException("执行器 AppName 不能为空");
        }
        Long jobGroupId = resolveJobGroupId(appName);
        int start = 0;
        int pageSize = DETAIL_PAGE_SIZE;
        while (true) {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("jobGroup", String.valueOf(jobGroupId));
            form.add("start", String.valueOf(start));
            form.add("length", String.valueOf(pageSize));
            form.add("triggerStatus", "-1");
            JsonNode response = postFormForJson("/jobinfo/pageList", form);
            JsonNode dataNode = extractDataNode(response);
            List<XxlJobInfo> jobs = parseJobs(dataNode);
            for (XxlJobInfo job : jobs) {
                if (jobId.equals(job.getId())) {
                    return job;
                }
            }
            Long totalValue = readLong(response, "recordsTotal", "total");
            if (totalValue == null) {
                break;
            }
            start += pageSize;
            if (start >= totalValue) {
                break;
            }
            if (jobs.isEmpty()) {
                break;
            }
        }
        throw new IllegalStateException("未找到任务详情，id: " + jobId);
    }

    /**
     * 从 xxl-admin 全量拉取任务
     *
     * 为什么：用于缓存刷新与全量拉取
     */
    private List<XxlJobInfo> fetchAllJobsFromXxl(String appName) {
        if (!StringUtils.hasText(appName)) {
            throw new IllegalArgumentException("执行器 AppName 不能为空");
        }
        Long jobGroupId = resolveJobGroupId(appName);
        int start = 0;
        int pageSize = DETAIL_PAGE_SIZE;
        List<XxlJobInfo> allJobs = new ArrayList<>();
        while (true) {
            /*
             * 目的：分页拉取，避免一次性请求过大
             */
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("jobGroup", String.valueOf(jobGroupId));
            form.add("start", String.valueOf(start));
            form.add("length", String.valueOf(pageSize));
            form.add("triggerStatus", "-1");
            JsonNode response = postFormForJson("/jobinfo/pageList", form);
            JsonNode dataNode = extractDataNode(response);
            List<XxlJobInfo> jobs = parseJobs(dataNode);
            allJobs.addAll(jobs);
            Long totalValue = readLong(response, "recordsTotal", "total");
            if (totalValue == null || allJobs.size() >= totalValue || jobs.isEmpty()) {
                break;
            }
            start += pageSize;
        }
        return allJobs;
    }

    /**
     * 创建 XXL 任务
     * 统一补全执行器 ID 与默认字段
     *
     * 为什么：确保创建请求包含必要默认值
     * 入参：任务信息
     * 出参：创建结果消息
     */
    @Override
    public String createJob(XxlJobInfo jobInfo) {
        XxlJobInfo normalized = normalizeJobInfo(jobInfo);
        MultiValueMap<String, String> form = buildJobForm(normalized);
        JsonNode response = postFormForJson("/jobinfo/add", form);
        return readText(response, "content");
    }

    /**
     * 更新 XXL 任务
     * 统一补全执行器 ID 与默认字段
     *
     * 为什么：保持字段完整，避免覆盖为空
     * 入参：任务信息
     * 出参：无
     */
    @Override
    public void updateJob(XxlJobInfo jobInfo) {
        XxlJobInfo normalized = normalizeJobInfo(jobInfo);
        XxlJobInfo existing = queryJobDetail(xxlAdminProperties.getAppName(), normalized.getId());
        XxlJobInfo merged = mergeJobInfo(normalized, existing);
        MultiValueMap<String, String> form = buildJobForm(merged);
        form.add("id", String.valueOf(merged.getId()));
        postFormForJson("/jobinfo/update", form);
    }

    /**
     * 删除 XXL 任务
     *
     * 为什么：统一删除入口
     * 入参：任务 ID
     * 出参：无
     */
    @Override
    public void removeJob(Long jobId) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("id", String.valueOf(jobId));
        postFormForJson("/jobinfo/remove", form);
    }

    /**
     * 启动 XXL 任务
     *
     * 为什么：统一启用入口
     * 入参：任务 ID
     * 出参：无
     */
    @Override
    public void startJob(Long jobId) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("id", String.valueOf(jobId));
        postFormForJson("/jobinfo/start", form);
    }

    /**
     * 停止 XXL 任务
     *
     * 为什么：统一停用入口
     * 入参：任务 ID
     * 出参：无
     */
    @Override
    public void stopJob(Long jobId) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("id", String.valueOf(jobId));
        postFormForJson("/jobinfo/stop", form);
    }

    /**
     * 手动触发 XXL 任务
     *
     * 为什么：支持立即执行
     * 入参：任务 ID、执行参数、指定地址
     * 出参：触发结果消息
     */
    @Override
    public String triggerJob(Long jobId, String executorParam, String addressList) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("id", String.valueOf(jobId));
        if (StringUtils.hasText(executorParam)) {
            form.add("executorParam", executorParam);
        }
        if (StringUtils.hasText(addressList)) {
            form.add("addressList", addressList);
        }
        JsonNode response = postFormForJson("/jobinfo/trigger", form);
        String msg = readText(response, "msg");
        if (StringUtils.hasText(msg)) {
            return msg;
        }
        return readText(response, "content");
    }

    /**
     * 分页查询 XXL 任务日志
     * 自动解析执行器 ID 并调用日志分页接口
     *
     * 为什么：统一分页入口并复用执行器解析逻辑
     * 入参：日志分页查询条件
     * 出参：日志分页结果
     */
    @Override
    public PageResult<XxlJobLogInfo> queryJobLogPage(XxlJobLogPageQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("日志查询条件不能为空");
        }
        Long jobId = query.getJobId();
        if (jobId == null || jobId <= 0) {
            throw new IllegalArgumentException("任务 ID 不能为空");
        }
        String appName = query.getAppName();
        if (!StringUtils.hasText(appName)) {
            throw new IllegalArgumentException("执行器 AppName 不能为空");
        }
        Integer pageNum = query.getPageNum();
        Integer pageSize = query.getPageSize();
        /*
         * 目的：规范化分页参数，避免异常分页导致接口报错
         */
        int safePageNum = pageNum == null ? 1 : pageNum;
        int safePageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        int start = (safePageNum - 1) * safePageSize;

        /*
         * 目的：先解析执行器 ID，再调用日志分页接口
         */
        Long jobGroupId = resolveJobGroupId(appName);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("start", String.valueOf(start));
        form.add("length", String.valueOf(safePageSize));
        form.add("jobGroup", String.valueOf(jobGroupId));
        form.add("jobId", String.valueOf(jobId));
        form.add("logStatus", "0");
        String filterTime = buildFilterTime(query.getStartTime(), query.getEndTime());
        if (StringUtils.hasText(filterTime)) {
            form.add("filterTime", filterTime);
        }

        JsonNode response = postFormForJson("/joblog/pageList", form);
        Long totalValue = readLong(response, "recordsTotal", "total");
        JsonNode dataNode = extractDataNode(response);
        List<XxlJobLogInfo> logs = parseJobLogs(dataNode);
        long total = totalValue == null ? logs.size() : totalValue;

        return PageResult.of(logs, total, safePageNum, safePageSize);
    }

    /**
     * 查询 XXL 任务日志详情
     *
     * 为什么：按行加载日志详情
     * 入参：日志 ID、起始行
     * 出参：日志详情
     */
    @Override
    public XxlJobLogDetail queryLogDetail(Long logId, Integer fromLineNum) {
        int startLine = fromLineNum == null ? 0 : fromLineNum;
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("logId", String.valueOf(logId));
        form.add("fromLineNum", String.valueOf(startLine));

        JsonNode response = postFormForJson("/joblog/logDetailCat", form);
        JsonNode content = response.get("content");
        if (content == null || content.isNull()) {
            return XxlJobLogDetail.builder()
                    .fromLineNum(startLine)
                    .toLineNum(startLine)
                    .logContent("")
                    .end(Boolean.TRUE)
                    .build();
        }

        return XxlJobLogDetail.builder()
                .fromLineNum(readInteger(content, "fromLineNum"))
                .toLineNum(readInteger(content, "toLineNum"))
                .logContent(readText(content, "logContent"))
                .end(readBoolean(content, "isEnd", "end"))
                .build();
    }

    private Long resolveJobGroupId(String appName) {
        /*
         * 目的：优先读取缓存，避免频繁查询执行器列表
         */
        String cacheKey = XxlJobRedisKeys.JOB_GROUP_PREFIX + appName;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            try {
                return Long.parseLong(cached);
            } catch (NumberFormatException ignored) {
                stringRedisTemplate.delete(cacheKey);
            }
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("appname", appName);
        JsonNode response = postFormForJson("/jobgroup/pageList", form);
        JsonNode dataNode = extractDataNode(response);
        if (dataNode == null || !dataNode.isArray()) {
            throw new IllegalStateException("获取执行器列表失败，响应格式不正确");
        }

        for (JsonNode item : dataNode) {
            String itemAppName = readText(item, "appname", "appName");
            if (appName.equals(itemAppName)) {
                Long id = readLong(item, "id");
                if (id != null) {
                    cacheJobGroup(cacheKey, id);
                    return id;
                }
            }
        }
        throw new IllegalStateException("未找到对应执行器：" + appName);
    }

    /**
     * 缓存执行器 ID
     *
     * 为什么：减少对 xxl-admin 的重复查询
     */
    private void cacheJobGroup(String cacheKey, Long jobGroupId) {
        Integer ttlSeconds = xxlAdminProperties.getCookieTtlSeconds();
        int ttl = ttlSeconds == null ? 1800 : ttlSeconds;
        stringRedisTemplate.opsForValue().set(cacheKey, String.valueOf(jobGroupId), Duration.ofSeconds(ttl));
    }

    /**
     * 发送表单请求并解析 JSON
     *
     * 为什么：统一处理登录态与响应校验
     */
    private JsonNode postFormForJson(String path, MultiValueMap<String, String> form) {
        String cookie = getOrLoginCookie();
        XxlHttpResponse response = executePostForm(path, form, cookie);
        if (isNeedRelogin(response)) {
            cookie = loginAndCacheCookie();
            response = executePostForm(path, form, cookie);
        }

        JsonNode json = parseJson(response.body());
        if (json == null) {
            throw new IllegalStateException("xxl-admin 响应解析失败");
        }
        validateResponseIfNeeded(json);
        return json;
    }

    /**
     * 判断是否需要重新登录
     *
     * 为什么：登录态过期需要自动续期
     */
    private boolean isNeedRelogin(XxlHttpResponse response) {
        if (response == null) {
            return true;
        }
        HttpStatusCode status = response.status();
        if (status != null && (status.value() == 302 || status.value() == 401)) {
            return true;
        }
        return response.body() != null && response.body().contains("login");
    }

    /**
     * 执行表单 POST 请求
     *
     * 为什么：统一 WebClient 调用入口
     */
    private XxlHttpResponse executePostForm(String path, MultiValueMap<String, String> form, String cookie) {
        WebClient client = getWebClient();
        return client.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.COOKIE, cookie)
                .body(BodyInserters.fromFormData(form))
                .exchangeToMono(this::toResponse)
                .block();
    }

    /**
     * 获取 Cookie（无则登录）
     *
     * 为什么：保持登录态可用
     */
    private String getOrLoginCookie() {
        String cached = stringRedisTemplate.opsForValue().get(XxlJobRedisKeys.ADMIN_COOKIE);
        if (StringUtils.hasText(cached)) {
            return cached;
        }
        return loginAndCacheCookie();
    }

    /**
     * 登录并缓存 Cookie
     *
     * 为什么：登录态需要缓存以复用
     */
    private String loginAndCacheCookie() {
        String username = xxlAdminProperties.getUsername();
        String password = xxlAdminProperties.getPassword();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalStateException("xxl-admin 登录账号未配置");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("userName", username);
        form.add("password", password);

        XxlHttpResponse response = executePostForm("/login", form, "");
        JsonNode json = parseJson(response.body());
        if (json == null) {
            throw new IllegalStateException("xxl-admin 登录响应解析失败");
        }
        Integer code = readInteger(json, "code");
        if (code != null && code != 200) {
            String msg = readText(json, "msg");
            throw new IllegalStateException("xxl-admin 登录失败：" + msg);
        }

        String cookie = buildCookieHeader(response.headers().get(HttpHeaders.SET_COOKIE));
        if (!StringUtils.hasText(cookie)) {
            throw new IllegalStateException("xxl-admin 登录未返回 Cookie");
        }
        Integer ttlSeconds = xxlAdminProperties.getCookieTtlSeconds();
        int ttl = ttlSeconds == null ? 1800 : ttlSeconds;
        stringRedisTemplate.opsForValue().set(XxlJobRedisKeys.ADMIN_COOKIE, cookie, Duration.ofSeconds(ttl));
        return cookie;
    }

    /**
     * 获取 WebClient
     *
     * 为什么：懒加载避免重复构建
     */
    private WebClient getWebClient() {
        if (webClient != null) {
            return webClient;
        }
        synchronized (this) {
            if (webClient == null) {
                String baseUrl = xxlAdminProperties.getBaseUrl();
                if (!StringUtils.hasText(baseUrl)) {
                    throw new IllegalStateException("xxl-admin baseUrl 未配置");
                }
                webClient = webClientBuilder.baseUrl(baseUrl).build();
            }
        }
        return webClient;
    }

    /**
     * 将响应转换为内部响应对象
     *
     * 为什么：统一读取状态、头与内容
     */
    private reactor.core.publisher.Mono<XxlHttpResponse> toResponse(ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> new XxlHttpResponse(response.statusCode(), response.headers().asHttpHeaders(), body));
    }

    /**
     * 解析 JSON 字符串
     *
     * 为什么：统一异常处理，避免上层重复 try/catch
     */
    private JsonNode parseJson(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            log.warn("xxl-admin 响应解析失败: {}", body, e);
            return null;
        }
    }

    /**
     * 校验 xxl-admin 响应状态
     *
     * 为什么：非 200 直接抛出异常
     */
    private void validateResponseIfNeeded(JsonNode json) {
        JsonNode codeNode = json.get("code");
        if (codeNode != null && codeNode.isInt()) {
            int code = codeNode.asInt();
            if (code != 200) {
                String msg = readText(json, "msg");
                throw new IllegalStateException("xxl-admin 接口调用失败：" + msg);
            }
        }
    }

    /**
     * 提取数据节点
     *
     * 为什么：兼容不同响应字段（data/content）
     */
    private JsonNode extractDataNode(JsonNode json) {
        if (json == null) {
            return null;
        }
        JsonNode data = json.get("data");
        if (data != null && !data.isNull()) {
            return data;
        }
        JsonNode content = json.get("content");
        if (content != null && !content.isNull()) {
            return content;
        }
        return null;
    }

    /**
     * 解析任务列表
     *
     * 为什么：将响应 JSON 转为领域实体
     */
    private List<XxlJobInfo> parseJobs(JsonNode dataNode) {
        List<XxlJobInfo> result = new ArrayList<>();
        if (dataNode == null || !dataNode.isArray()) {
            return result;
        }
        for (JsonNode item : dataNode) {
            XxlJobInfo job = XxlJobInfo.builder()
                    .id(readLong(item, "id"))
                    .jobDesc(readText(item, "jobDesc"))
                    .jobGroup(readInteger(item, "jobGroup", "job_group"))
                    .author(readText(item, "author"))
                    .alarmEmail(readText(item, "alarmEmail", "alarm_email"))
                    .scheduleType(readText(item, "scheduleType", "schedule_type"))
                    .executorHandler(readText(item, "executorHandler"))
                    .executorParam(readText(item, "executorParam"))
                    .scheduleConf(readText(item, "scheduleConf"))
                    .misfireStrategy(readText(item, "misfireStrategy", "misfire_strategy"))
                    .executorRouteStrategy(readText(item, "executorRouteStrategy"))
                    .executorBlockStrategy(readText(item, "executorBlockStrategy", "executor_block_strategy"))
                    .executorTimeout(readInteger(item, "executorTimeout", "executor_timeout"))
                    .executorFailRetryCount(readInteger(item, "executorFailRetryCount", "executor_fail_retry_count"))
                    .glueType(readText(item, "glueType", "glue_type"))
                    .childJobId(readText(item, "childJobId", "child_jobid"))
                    .triggerStatus(readInteger(item, "triggerStatus"))
                    .triggerLastTime(readLong(item, "triggerLastTime", "trigger_last_time"))
                    .triggerNextTime(readLong(item, "triggerNextTime", "trigger_next_time"))
                    .addTime(readText(item, "addTime"))
                    .updateTime(readText(item, "updateTime"))
                    .build();
            result.add(job);
        }
        return result;
    }

    /**
     * 解析日志列表
     *
     * 为什么：将响应 JSON 转为领域实体
     */
    private List<XxlJobLogInfo> parseJobLogs(JsonNode dataNode) {
        List<XxlJobLogInfo> result = new ArrayList<>();
        if (dataNode == null || !dataNode.isArray()) {
            return result;
        }
        for (JsonNode item : dataNode) {
            XxlJobLogInfo logInfo = XxlJobLogInfo.builder()
                    .id(readLong(item, "id"))
                    .jobGroup(readInteger(item, "jobGroup", "job_group"))
                    .jobId(readLong(item, "jobId", "job_id"))
                    .executorAddress(readText(item, "executorAddress", "executor_address"))
                    .executorHandler(readText(item, "executorHandler", "executor_handler"))
                    .executorParam(readText(item, "executorParam", "executor_param"))
                    .executorShardingParam(readText(item, "executorShardingParam", "executor_sharding_param"))
                    .executorFailRetryCount(readInteger(item, "executorFailRetryCount", "executor_fail_retry_count"))
                    .triggerTime(readText(item, "triggerTime", "trigger_time"))
                    .triggerCode(readInteger(item, "triggerCode", "trigger_code"))
                    .triggerMsg(readText(item, "triggerMsg", "trigger_msg"))
                    .handleTime(readText(item, "handleTime", "handle_time"))
                    .handleCode(readInteger(item, "handleCode", "handle_code"))
                    .handleMsg(readText(item, "handleMsg", "handle_msg"))
                    .alarmStatus(readInteger(item, "alarmStatus", "alarm_status"))
                    .build();
            result.add(logInfo);
        }
        return result;
    }

    /**
     * 组装 Cookie Header
     *
     * 为什么：将 Set-Cookie 头转为 Cookie 请求头
     */
    private String buildCookieHeader(List<String> setCookies) {
        if (CollectionUtils.isEmpty(setCookies)) {
            return null;
        }
        List<String> cookiePairs = new ArrayList<>();
        for (String cookie : setCookies) {
            if (!StringUtils.hasText(cookie)) {
                continue;
            }
            String pair = cookie.split(";", 2)[0];
            if (StringUtils.hasText(pair)) {
                cookiePairs.add(pair);
            }
        }
        if (cookiePairs.isEmpty()) {
            return null;
        }
        return String.join("; ", cookiePairs);
    }

    /**
     * 构建日志过滤时间字符串
     *
     * 为什么：xxl-admin 接口需要 "start - end" 格式
     */
    private String buildFilterTime(String startTime, String endTime) {
        if (!StringUtils.hasText(startTime) || !StringUtils.hasText(endTime)) {
            return null;
        }
        return startTime.trim() + " - " + endTime.trim();
    }

    /**
     * 归一化任务信息
     *
     * 为什么：补齐默认字段与执行器 ID
     */
    private XxlJobInfo normalizeJobInfo(XxlJobInfo jobInfo) {
        String appName = xxlAdminProperties.getAppName();
        if (!StringUtils.hasText(appName)) {
            throw new IllegalStateException("xxl.admin.app-name 未配置");
        }
        Long jobGroupId = resolveJobGroupId(appName);
        String scheduleType = StringUtils.hasText(jobInfo.getScheduleType()) ? jobInfo.getScheduleType() : DEFAULT_SCHEDULE_TYPE;
        String misfireStrategy = StringUtils.hasText(jobInfo.getMisfireStrategy()) ? jobInfo.getMisfireStrategy() : DEFAULT_MISFIRE_STRATEGY;
        String routeStrategy = StringUtils.hasText(jobInfo.getExecutorRouteStrategy()) ? jobInfo.getExecutorRouteStrategy() : DEFAULT_ROUTE_STRATEGY;
        String blockStrategy = StringUtils.hasText(jobInfo.getExecutorBlockStrategy()) ? jobInfo.getExecutorBlockStrategy() : DEFAULT_BLOCK_STRATEGY;
        String glueType = StringUtils.hasText(jobInfo.getGlueType()) ? jobInfo.getGlueType() : DEFAULT_GLUE_TYPE;

        return XxlJobInfo.builder()
                .id(jobInfo.getId())
                .jobGroup(jobGroupId == null ? null : jobGroupId.intValue())
                .jobDesc(jobInfo.getJobDesc())
                .author(jobInfo.getAuthor())
                .alarmEmail(jobInfo.getAlarmEmail())
                .scheduleType(scheduleType)
                .scheduleConf(jobInfo.getScheduleConf())
                .misfireStrategy(misfireStrategy)
                .executorRouteStrategy(routeStrategy)
                .executorHandler(jobInfo.getExecutorHandler())
                .executorParam(jobInfo.getExecutorParam())
                .executorBlockStrategy(blockStrategy)
                .executorTimeout(jobInfo.getExecutorTimeout())
                .executorFailRetryCount(jobInfo.getExecutorFailRetryCount())
                .glueType(glueType)
                .childJobId(jobInfo.getChildJobId())
                .build();
    }

    /**
     * 构建任务表单
     *
     * 为什么：与 xxl-admin 表单字段对齐
     */
    private MultiValueMap<String, String> buildJobForm(XxlJobInfo jobInfo) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("jobGroup", String.valueOf(jobInfo.getJobGroup()));
        form.add("jobDesc", jobInfo.getJobDesc());
        form.add("author", jobInfo.getAuthor());
        if (StringUtils.hasText(jobInfo.getAlarmEmail())) {
            form.add("alarmEmail", jobInfo.getAlarmEmail());
        }
        form.add("scheduleType", jobInfo.getScheduleType());
        form.add("scheduleConf", jobInfo.getScheduleConf());
        form.add("misfireStrategy", jobInfo.getMisfireStrategy());
        form.add("executorRouteStrategy", jobInfo.getExecutorRouteStrategy());
        form.add("executorHandler", jobInfo.getExecutorHandler());
        if (StringUtils.hasText(jobInfo.getExecutorParam())) {
            form.add("executorParam", jobInfo.getExecutorParam());
        }
        form.add("executorBlockStrategy", jobInfo.getExecutorBlockStrategy());
        if (jobInfo.getExecutorTimeout() != null) {
            form.add("executorTimeout", String.valueOf(jobInfo.getExecutorTimeout()));
        }
        if (jobInfo.getExecutorFailRetryCount() != null) {
            form.add("executorFailRetryCount", String.valueOf(jobInfo.getExecutorFailRetryCount()));
        }
        form.add("glueType", jobInfo.getGlueType());
        if (StringUtils.hasText(jobInfo.getChildJobId())) {
            form.add("childJobId", jobInfo.getChildJobId());
        }
        if (jobInfo.getTriggerStatus() != null) {
            form.add("triggerStatus", String.valueOf(jobInfo.getTriggerStatus()));
        }
        if (jobInfo.getTriggerLastTime() != null) {
            form.add("triggerLastTime", String.valueOf(jobInfo.getTriggerLastTime()));
        }
        if (jobInfo.getTriggerNextTime() != null) {
            form.add("triggerNextTime", String.valueOf(jobInfo.getTriggerNextTime()));
        }
        return form;
    }

    /**
     * 合并任务信息
     *
     * 为什么：更新时保留只读字段（如触发状态）
     */
    private XxlJobInfo mergeJobInfo(XxlJobInfo incoming, XxlJobInfo existing) {
        if (existing == null) {
            return incoming;
        }
        return XxlJobInfo.builder()
                .id(incoming.getId())
                .jobGroup(incoming.getJobGroup())
                .jobDesc(incoming.getJobDesc())
                .author(incoming.getAuthor())
                .alarmEmail(incoming.getAlarmEmail())
                .scheduleType(incoming.getScheduleType())
                .scheduleConf(incoming.getScheduleConf())
                .misfireStrategy(incoming.getMisfireStrategy())
                .executorRouteStrategy(incoming.getExecutorRouteStrategy())
                .executorHandler(incoming.getExecutorHandler())
                .executorParam(incoming.getExecutorParam())
                .executorBlockStrategy(incoming.getExecutorBlockStrategy())
                .executorTimeout(incoming.getExecutorTimeout())
                .executorFailRetryCount(incoming.getExecutorFailRetryCount())
                .glueType(incoming.getGlueType())
                .childJobId(incoming.getChildJobId())
                .triggerStatus(existing.getTriggerStatus())
                .triggerLastTime(existing.getTriggerLastTime())
                .triggerNextTime(existing.getTriggerNextTime())
                .build();
    }

    private Long readLong(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.canConvertToLong()) {
                return value.asLong();
            }
        }
        return null;
    }

    private Integer readInteger(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isInt()) {
                return value.asInt();
            }
        }
        return null;
    }

    private Boolean readBoolean(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isBoolean()) {
                return value.asBoolean();
            }
        }
        return null;
    }

    private String readText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                return value.asText();
            }
        }
        return null;
    }

    /**
     * xxl-admin HTTP 响应封装
     *
     * 为什么：统一承载状态码、头与响应体
     */
    private static class XxlHttpResponse {

        private final HttpStatusCode status;
        private final HttpHeaders headers;
        private final String body;

        private XxlHttpResponse(HttpStatusCode status, HttpHeaders headers, String body) {
            this.status = status;
            this.headers = headers;
            this.body = body;
        }

        private HttpStatusCode status() {
            return status;
        }

        private HttpHeaders headers() {
            return headers;
        }

        private String body() {
            return body;
        }
    }
}
