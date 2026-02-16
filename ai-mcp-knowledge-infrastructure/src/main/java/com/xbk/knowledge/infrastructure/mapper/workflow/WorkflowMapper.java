package com.xbk.knowledge.infrastructure.mapper.workflow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.model.entity.workflow.Workflow;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.workflow.WorkflowCodeQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * WorkflowMapper。
 *
 * @author xiexu
 */
@Mapper
public interface WorkflowMapper extends BaseMapper<Workflow> {

    int insertWorkflow(Workflow workflow);

    int updateWorkflow(Workflow workflow);

    Workflow findById(IdQuery query);

    Workflow findByCode(WorkflowCodeQuery query);

    List<Workflow> list(@Param("orgId") Long orgId,
                        @Param("keyword") String keyword,
                        @Param("offset") int offset,
                        @Param("pageSize") int pageSize);

    long count(@Param("orgId") Long orgId, @Param("keyword") String keyword);
}

