package com.xbk.knowledge.api.dto.preheat;

import lombok.Data;

import java.util.List;

/**
 * 预热响应。
 
  * @author xiexu
  */
@Data
public class PreheatResponse {

    private String targetType;
    private Long targetId;
    private Boolean mcpRefreshed;
    private Boolean toolCallbacksWarmed;
    private Boolean advisorsWarmed;
    private Boolean workflowValidated;
    private List<String> warnings;
}
