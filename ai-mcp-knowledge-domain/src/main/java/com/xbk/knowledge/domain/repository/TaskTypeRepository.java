package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.entity.TaskType;

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
     * @param taskCode 任务编码
     * @return 任务类型
     */
    Optional<TaskType> findByTaskCode(String taskCode);

    /**
     * 根据ID查询任务类型
     *
     * @param id 任务类型ID
     * @return 任务类型
     */
    Optional<TaskType> findById(Long id);

    /**
     * 保存任务类型（新增或更新）
     *
     * @param taskType 任务类型
     * @return 保存后的任务类型
     */
    TaskType save(TaskType taskType);

    /**
     * 判断任务类型是否存在
     *
     * @param id 任务类型ID
     * @return 是否存在
     */
    boolean existsById(Long id);

    /**
     * 删除任务类型
     *
     * @param id 任务类型ID
     */
    void deleteById(Long id);

    /**
     * 查询任务类型分页数据
     *
     * @param offset   偏移量
     * @param pageSize 每页大小
     * @return 任务类型列表
     */
    List<TaskType> findPage(int offset, int pageSize);

    /**
     * 统计任务类型总数
     *
     * @return 总数
     */
    long countAll();
}
