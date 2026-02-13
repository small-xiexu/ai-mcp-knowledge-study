package com.xbk.knowledge.domain.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * API Key 实体。
 * 对应数据库表：sys_api_key
 *
 * 职责：领域实体，用于承载服务账号密钥信息。
 *
 * @author xiexu
 */
@TableName("sys_api_key")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysApiKey {

    /**
     * 主键ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID。
     */
    private String tenantId;

    /**
     * 归属用户ID。
     */
    private Long ownerUserId;

    /**
     * 访问Key。
     */
    private String accessKey;

    /**
     * 密钥哈希。
     */
    private String secretHash;

    /**
     * 权限范围（JSON）。
     */
    private String scopes;

    /**
     * 状态：1启用、0禁用。
     */
    private Integer status;

    /**
     * 过期时间。
     */
    private LocalDateTime expireAt;

    /**
     * 最后使用时间。
     */
    private LocalDateTime lastUsedAt;

    /**
     * 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
