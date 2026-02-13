package com.xbk.knowledge.api.dto.org;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 组织响应 DTO。
 *
 * 职责：接口层 DTO，用于输出组织信息。
 *
 * @author xiexu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 组织ID。
     */
    private Long id;

    /**
     * 组织编码。
     */
    private String orgCode;

    /**
     * 组织名称。
     */
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
    private String remark;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
