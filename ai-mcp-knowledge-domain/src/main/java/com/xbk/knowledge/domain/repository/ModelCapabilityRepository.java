package com.xbk.knowledge.domain.repository;

import com.xbk.knowledge.domain.model.entity.ModelCapability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 模型能力 Repository
 *
 * @author xiexu
 */
@Repository
public interface ModelCapabilityRepository extends JpaRepository<ModelCapability, Long> {

    /**
     * 根据模型ID查询模型能力
     *
     * @param modelId 模型ID
     * @return 模型能力
     */
    Optional<ModelCapability> findByModelId(Long modelId);
}
