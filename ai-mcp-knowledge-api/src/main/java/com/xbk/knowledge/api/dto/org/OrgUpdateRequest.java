package com.xbk.knowledge.api.dto.org;

import com.xbk.knowledge.types.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 组织更新请求 DTO。
 *
 * 职责：接口层 DTO，用于承载组织更新参数。
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrgUpdateRequest extends BaseRequest {

    private static final long serialVersionUID = 1L;

    /**
     * 组织ID。
     */
    @NotNull(message = "组织ID不能为空")
    private Long id;

    /**
     * 组织名称。
     */
    @NotNull(message = "组织名称不能为空")
    @Size(max = 128, message = "组织名称长度不能超过128")
    private String orgName;

    /**
     * 父组织ID。
     */
    private Long parentId;

    /**
     * 组织路径。
     */
    private String orgPath;

    /**
     * 状态。
     */
    private Integer status;

    /**
     * 备注。
     */
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}
