package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.WorkflowPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowCodeQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * IWorkflow 数据访问接口定义。
 *
 * @author sxie
 */
@Mapper
public interface IWorkflowDao extends BaseMapper<WorkflowPO> {

    int insertWorkflow(WorkflowPO workflow);

    int updateWorkflow(WorkflowPO workflow);

    WorkflowPO findById(IdQuery query);

    WorkflowPO findByCode(WorkflowCodeQuery query);

    List<WorkflowPO> list(@Param("keyword") String keyword,
                        @Param("offset") int offset,
                        @Param("pageSize") int pageSize);

    long count(@Param("keyword") String keyword);
}

