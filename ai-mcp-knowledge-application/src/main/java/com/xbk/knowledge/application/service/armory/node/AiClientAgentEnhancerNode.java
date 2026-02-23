package com.xbk.knowledge.application.service.armory.node;

import com.xbk.knowledge.application.service.armory.factory.AiClientArmoryContext;
import com.xbk.knowledge.config.ai.GlobalChatAgentEnhancer;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * AgentEnhancer 节点，负责合并全局与运行时 AgentEnhancers。
 *
 * @author sxie
 */
@Component
public class AiClientAgentEnhancerNode extends AbstractAiClientArmoryNode {

    private final List<CallAdvisor> globalAdvisors;

    public AiClientAgentEnhancerNode(List<CallAdvisor> advisors) {
        List<CallAdvisor> globals = new ArrayList<>();
        if (advisors != null) {
            for (CallAdvisor advisor : advisors) {
                if (advisor == null) {
                    continue;
                }
                if (advisor.getClass().isAnnotationPresent(GlobalChatAgentEnhancer.class)) {
                    globals.add(advisor);
                }
            }
        }
        this.globalAdvisors = globals;
    }

    @Override
    protected void doHandle(AiClientArmoryContext context) {
        List<CallAdvisor> merged = new ArrayList<>();
        if (globalAdvisors != null && !globalAdvisors.isEmpty()) {
            merged.addAll(globalAdvisors);
        }
        CallAdvisor[] extraAdvisors = context.getExtraAdvisors();
        if (extraAdvisors != null) {
            for (CallAdvisor extraAdvisor : extraAdvisors) {
                if (extraAdvisor != null) {
                    merged.add(extraAdvisor);
                }
            }
        }
        merged.sort(Comparator.comparingInt(advisor -> {
            try {
                return advisor.getOrder();
            } catch (Exception ignore) {
                return 0;
            }
        }));
        context.setMergedAdvisors(merged);
    }
}
