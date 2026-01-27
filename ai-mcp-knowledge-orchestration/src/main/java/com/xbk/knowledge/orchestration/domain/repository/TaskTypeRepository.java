package com.xbk.knowledge.orchestration.domain.repository;

import com.xbk.knowledge.orchestration.domain.entity.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 任务类型 Repository
 *
 * @author xiexu
 */
@Repository
public interface TaskTypeRepository extends JpaRepository<TaskType, Long> {

    /**
     * 根据任务编码查询任务类型
     *
     * @param taskCode 任务编码
     * @return 任务类型
     */
    Optional<TaskType> findByTaskCode(String taskCode);
}
