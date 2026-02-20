package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.types.contract.PlatformContractV1;
import com.xbk.knowledge.types.contract.PlatformStreamEvent;
import reactor.core.publisher.Flux;

/**
 * Agent 运行入口应用服务（按 agentCode 路由）。
 *
 * @author sxie
 */
public interface AgentRuntimeAppService {

    PlatformContractV1 chat(String agentCode, Long sessionId, String content, String ragTagsJson);

    Flux<PlatformStreamEvent> stream(String agentCode, Long sessionId, String content, String ragTagsJson);

    PlatformContractV1 invoke(String agentCode, Long sessionId, String content, String ragTagsJson);

    /**
     * XXL 调度执行入口（run_type=XXL_JOB, trigger_source=XXL）。
     *
     * @param scopeId       scopeId
     * @param agentCode   Agent 对外编码
     * @param content     输入内容（必填）
     * @param ragTagsJson RAG tags JSON（可选）
     * @return Platform Contract v1
     */
    PlatformContractV1 runJob(String agentCode, String content, String ragTagsJson);
}
