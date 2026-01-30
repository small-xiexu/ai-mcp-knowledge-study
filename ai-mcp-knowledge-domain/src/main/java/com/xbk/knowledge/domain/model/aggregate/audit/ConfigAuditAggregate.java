package com.xbk.knowledge.domain.model.aggregate.audit;

import com.xbk.knowledge.domain.model.entity.ConfigAudit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 配置审计聚合
 * 以审计记录为聚合根，保证审计落库一致性
 *
 * 职责：聚合根承载审计记录的生命周期
 * @author xiexu
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigAuditAggregate {

    /**
     * 配置审计记录（聚合根）
     */
    private ConfigAudit configAudit;
}
