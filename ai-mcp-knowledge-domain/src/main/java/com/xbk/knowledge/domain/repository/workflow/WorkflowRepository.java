package com.xbk.knowledge.domain.repository.workflow;

import com.xbk.knowledge.domain.model.entity.workflow.Workflow;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.workflow.WorkflowCodeQuery;

import java.util.List;
import java.util.Optional;

/**
 * Workflow 资产仓储接口。
 
  * @author xiexu
  */
public interface WorkflowRepository {

    Optional<Workflow> findById(IdQuery query);

    Optional<Workflow> findByCode(WorkflowCodeQuery query);

    Workflow insert(Workflow workflow);

    int updateById(Workflow workflow);

    List<Workflow> list(Long orgId, String keyword, int offset, int pageSize);

    long count(Long orgId, String keyword);
}

