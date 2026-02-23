package com.xbk.knowledge.infrastructure.dao;

import com.xbk.knowledge.infrastructure.dao.po.WorkflowRunContextPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * IWorkflowRunContext 数据访问接口定义。
 *
 * @author sxie
 */
@Mapper
public interface IWorkflowRunContextDao extends BaseMapper<WorkflowRunContextPO> {

    int upsert(WorkflowRunContextPO ctx);

    WorkflowRunContextPO findByRunId(@Param("runId") String runId);

    int updateStatus(@Param("runId") String runId, @Param("status") String status);

    int deleteByRunIds(@Param("runIds") List<String> runIds);
}
