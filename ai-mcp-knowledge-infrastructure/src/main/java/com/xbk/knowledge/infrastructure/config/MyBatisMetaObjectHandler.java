package com.xbk.knowledge.infrastructure.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.xbk.knowledge.types.context.OrgContextHolder;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 元对象处理器
 * 统一填充创建时间与更新时间
 *
 * 职责：基础设施配置，用于完成框架适配
 * @author xiexu
 */
@Component
public class MyBatisMetaObjectHandler implements MetaObjectHandler {

    /**
     * 对外暴露 insertFill 作为调用入口，便于上层复用。
     *
     * 为什么：统一补齐创建/更新时间，避免各处重复设置
     * 入参：元对象
     * 出参：无
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        /*
         * 目的：统一为包含 orgId 字段的实体补齐组织归属。
         * 约束：未注入 OrgContext 时默认回填组织ID（1），避免空值导致写入失败。
         */
        if (metaObject != null && metaObject.hasSetter("orgId")) {
            Long currentOrgId = OrgContextHolder.currentOrgIdOrNull();
            Long orgId = currentOrgId != null ? currentOrgId : 1L;
            this.strictInsertFill(metaObject, "orgId", Long.class, orgId);
        }
    }

    /**
     * 对外暴露 updateFill 作为调用入口，便于上层复用。
     *
     * 为什么：统一补齐更新时间，保持审计一致
     * 入参：元对象
     * 出参：无
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime updatedAt = LocalDateTime.now();
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, updatedAt);
    }
}
