package com.xbk.knowledge.infrastructure.redis.key;

/**
 * XXL-Job 相关 Redis Key 定义
 *
 * 职责：统一调度中心缓存 Key 命名
 * @author xiexu
 */
public final class XxlJobRedisKeys {

    /**
     * 调度中心登录 Cookie Key
     * 为什么：统一管理登录态缓存
     */
    public static final String ADMIN_COOKIE = "xxl:admin:cookie";

    /**
     * 执行器 ID 缓存前缀
     * 为什么：按执行器名称缓存其 ID，减少重复查询
     */
    public static final String JOB_GROUP_PREFIX = "xxl:admin:jobgroup:";

    /**
     * 执行器任务列表缓存前缀
     * 为什么：缓存任务列表，降低调度中心调用频次
     */
    public static final String JOB_CACHE_PREFIX = "xxl:admin:jobs:all:";

    private XxlJobRedisKeys() {
        
    }
}
