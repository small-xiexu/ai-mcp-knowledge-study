package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.aggregate.task.TaskTypeAggregate;
import com.xbk.knowledge.domain.model.entity.TaskType;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypeCodeQuery;
import com.xbk.knowledge.domain.model.vo.task.TaskTypePageQuery;

import java.util.List;
import java.util.Optional;

/**
 * 任务类型仓储接口
 * 通过仓储抽象隔离数据访问实现
 *
 * 职责：领域仓储接口，用于屏蔽存储细节
 * @author xiexu
 */
public interface TaskTypeRepository {

    /**
     * 根据任务编码查询任务类型
     *
     * @param query 任务编码查询条件
     * @return 任务类型
     */
    Optional<TaskType> findByTaskCode(TaskTypeCodeQuery query);

    /**
     * 根据ID查询任务类型
     *
     * @param query ID 查询条件
     * @return 任务类型
     */
    Optional<TaskType> findById(IdQuery query);

    /**
     * 保存任务类型聚合（新增或更新）
     *
     * @param aggregate 任务类型聚合
     * @return 保存后的聚合
     */
    TaskTypeAggregate save(TaskTypeAggregate aggregate);

    /**
     * 判断任务类型是否存在
     *
     * @param query ID 查询条件
     * @return 是否存在
     */
    boolean existsById(IdQuery query);

    /**
     * 删除任务类型
     *
     * @param query ID 查询条件
     */
    void deleteById(IdQuery query);

    /**
     * 查询任务类型分页数据
     *
     * @param query 分页查询条件
     * @return 任务类型列表
     */
    List<TaskType> findPage(TaskTypePageQuery query);

    /**
     * 统计任务类型总数
     *
     * @return 总数
     */
    long countAll();
}
