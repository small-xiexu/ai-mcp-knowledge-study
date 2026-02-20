package com.xbk.knowledge.api.dto.advisor;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Advisor 绑定保存请求。
 *
 * @author sxie
 */
@Data
public class AdvisorBindingSaveRequest {

    @NotBlank(message = "bindType 不能为空")
    private String bindType;

    @NotNull(message = "bindTargetId 不能为空")
    private Long bindTargetId;

    private List<AdvisorBindingSaveItem> items;

    @Data
    public static class AdvisorBindingSaveItem {
        @NotNull(message = "advisorId 不能为空")
        private Long advisorId;
        private Integer orderNo;
        private Boolean enabled;
    }
}

