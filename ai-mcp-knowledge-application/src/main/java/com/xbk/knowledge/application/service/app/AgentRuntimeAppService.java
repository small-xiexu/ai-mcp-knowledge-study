package com.xbk.knowledge.application.service.app;

import com.xbk.knowledge.types.contract.PlatformContractV1;
import com.xbk.knowledge.types.contract.PlatformStreamEvent;
import reactor.core.publisher.Flux;

/**
 * Agent 运行入口应用服务（按 agentCode 路由）。
 
  * @author xiexu
  */
public interface AgentRuntimeAppService {

    PlatformContractV1 chat(Long orgId, String agentCode, Long sessionId, String content, String ragTagsJson);

    Flux<PlatformStreamEvent> stream(Long orgId, String agentCode, Long sessionId, String content, String ragTagsJson);

    PlatformContractV1 invoke(Long orgId, String agentCode, Long sessionId, String content, String ragTagsJson);

    /**
     * XXL 调度执行入口（run_type=XXL_JOB, trigger_source=XXL）。
     *
     * @param orgId       组织ID
     * @param agentCode   Agent 对外编码
     * @param content     输入内容（必填）
     * @param ragTagsJson RAG tags JSON（可选）
     * @return Platform Contract v1
     */
    PlatformContractV1 runJob(Long orgId, String agentCode, String content, String ragTagsJson);
}
