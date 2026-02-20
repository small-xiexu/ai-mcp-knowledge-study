package com.xbk.knowledge.domain.workflow.adapter.repository;

import com.xbk.knowledge.domain.workflow.model.entity.Workflow;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowCodeQuery;

import java.util.List;
import java.util.Optional;

/**
 * Workflow 资产仓储接口。
 
  * @author xiexu
  */
public interface WorkflowRepository {

    /**
     * 方法：findById。
     */
    Optional<Workflow> findById(IdQuery query);

    /**
     * 方法：findByCode。
     */
    Optional<Workflow> findByCode(WorkflowCodeQuery query);

    /**
     * 方法：insert。
     */
    Workflow insert(Workflow workflow);

    /**
     * 方法：updateById。
     */
    int updateById(Workflow workflow);

    /**
     * 方法：list。
     */
    List<Workflow> list(String keyword, int offset, int pageSize);

    /**
     * 方法：count。
     */
    long count(String keyword);
}

