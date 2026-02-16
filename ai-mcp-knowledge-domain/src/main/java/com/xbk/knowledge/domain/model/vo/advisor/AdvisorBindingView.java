package com.xbk.knowledge.domain.model.vo.advisor;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Advisor 绑定视图（用于控制面展示与运行时装配）。
 *
 * 说明：该对象由 join 查询返回，不对应单表实体。
 
  * @author xiexu
  */
@Getter
@Setter
public class AdvisorBindingView {

    private Long bindingId;
    private Long orgId;
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

