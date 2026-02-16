package com.xbk.knowledge.domain.repository.task;

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
     * 为什么：任务执行依赖编码定位类型
     * 入参：任务编码查询条件
     * 出参：任务类型
     */
    Optional<TaskType> findByTaskCode(TaskTypeCodeQuery query);

    /**
     * 根据ID查询任务类型
     *
     * 为什么：按唯一 ID 获取任务类型
     * 入参：ID 查询条件
     * 出参：任务类型
     */
    Optional<TaskType> findById(IdQuery query);

    /**
     * 保存任务类型聚合（新增或更新）
     *
     * 为什么：保证聚合一致性
     * 入参：任务类型聚合
     * 出参：保存后的聚合
     */
    TaskTypeAggregate save(TaskTypeAggregate aggregate);

    /**
     * 判断任务类型是否存在
     *
     * 为什么：更新/删除前校验存在性
     * 入参：ID 查询条件
     * 出参：是否存在
     */
    boolean existsById(IdQuery query);

    /**
     * 删除任务类型
     *
     * 为什么：移除无效配置
     * 入参：ID 查询条件
     * 出参：无
     */
    void deleteById(IdQuery query);

    /**
     * 查询任务类型分页数据
     *
     * 为什么：分页展示任务类型列表
     * 入参：分页查询条件
     * 出参：任务类型列表
     */
    List<TaskType> findPage(TaskTypePageQuery query);

    /**
     * 统计任务类型总数
     *
     * 为什么：分页展示需要总数
     * 入参：无
     * 出参：总数
     */
    long countAll();
}
