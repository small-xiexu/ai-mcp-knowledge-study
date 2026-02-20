package com.xbk.knowledge.application.service.armory.node;

import com.xbk.knowledge.application.service.armory.factory.DefaultAiClientArmoryStrategyFactory;
import com.xbk.knowledge.config.ai.GlobalChatAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Advisor 节点，负责合并全局与运行时 Advisors。
 *
 * @author sxie
 */
@Component
public class AiClientAdvisorNode extends AbstractAiClientArmoryNode {

    private final List<CallAdvisor> globalAdvisors;

    public AiClientAdvisorNode(List<CallAdvisor> advisors) {
        List<CallAdvisor> globals = new ArrayList<>();
        if (advisors != null) {
            for (CallAdvisor advisor : advisors) {
                if (advisor == null) {
                    continue;
                }
                if (advisor.getClass().isAnnotationPresent(GlobalChatAdvisor.class)) {
                    globals.add(advisor);
                }
            }
        }
        this.globalAdvisors = globals;
    }

    @Override
    protected void doHandle(DefaultAiClientArmoryStrategyFactory.DynamicContext context) {
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

