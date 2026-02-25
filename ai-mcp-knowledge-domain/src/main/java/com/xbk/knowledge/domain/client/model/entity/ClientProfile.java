package com.xbk.knowledge.domain.client.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Client Profile 资产实体。
 *
 * @author sxie
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientProfile {

    /**
     * 主键 ID。
     */
    private Long id;

    /**
     * 客户画像编码。
     */
    private String clientCode;

    /**
     * 客户画像名称。
     */
    private String clientName;

    /**
     * 客户画像描述。
     */
    private String description;

    /**
     * ENABLED/DISABLED。
     */
    private String status;

    /**
     * 创建人 ID。
     */
    private Long createdBy;

    /**
     * 更新人 ID。
     */
    private Long updatedBy;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
