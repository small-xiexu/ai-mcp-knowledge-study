package com.xbk.knowledge.application.model.preheat;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 预热结果（用于控制面展示）。
 *
 * @author sxie
 */
@Getter
@Builder
public class PreheatResult {

    private final String targetType;
    private final Long targetId;
    private final boolean mcpRefreshed;
    private final boolean toolCallbacksWarmed;
    private final boolean advisorsWarmed;
    private final boolean workflowValidated;
    private final List<String> warnings;
}
