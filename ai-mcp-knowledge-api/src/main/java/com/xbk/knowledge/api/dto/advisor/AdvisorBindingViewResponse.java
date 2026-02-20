package com.xbk.knowledge.api.dto.advisor;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Advisor 绑定视图响应。
 
  * @author xiexu
  */
@Data
public class AdvisorBindingViewResponse {

    private Long bindingId;
    private String bindType;
    private Long bindTargetId;
    private Long advisorId;
    private Integer orderNo;
    private Integer bindingEnabled;
    private LocalDateTime bindingCreatedAt;
    private LocalDateTime bindingUpdatedAt;

    private String advisorCode;
    private String advisorName;
    private String advisorType;
    private Integer advisorEnabled;
    private String advisorConfigJson;
    private LocalDateTime advisorCreatedAt;
    private LocalDateTime advisorUpdatedAt;
}
