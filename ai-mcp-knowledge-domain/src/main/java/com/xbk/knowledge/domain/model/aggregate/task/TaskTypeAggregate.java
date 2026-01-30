package com.xbk.knowledge.domain.model.aggregate.task;

import com.xbk.knowledge.domain.model.entity.TaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 任务类型聚合
 * 以任务类型为聚合根，承载配置一致性边界
 *
 * 职责：聚合根负责任务配置的生命周期与一致性维护
 * @author xiexu
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskTypeAggregate {

    /**
     * 任务类型（聚合根）
     */
    private TaskType taskType;
}
