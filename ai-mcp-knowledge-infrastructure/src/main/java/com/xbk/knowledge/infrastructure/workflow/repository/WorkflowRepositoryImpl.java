package com.xbk.knowledge.infrastructure.workflow.repository;

import com.xbk.knowledge.domain.workflow.model.entity.Workflow;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.workflow.model.valobj.WorkflowCodeQuery;
import com.xbk.knowledge.domain.workflow.adapter.repository.WorkflowRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IWorkflowDao;
import com.xbk.knowledge.infrastructure.dao.po.WorkflowPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 工作流仓储实现。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class WorkflowRepositoryImpl implements WorkflowRepository {

    /**
     * Workflow 数据访问对象。
     */
    private final IWorkflowDao workflowMapper;

    /**
     * 查询工作流。
     *
     * @param query 主键查询条件
     * @return Workflow 查询结果（可能为空）
     */
    @Override
    public Optional<Workflow> findById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(workflowMapper.findById(query))
                .map(item -> BeanMappingUtils.map(item, Workflow.class));
    }

    /**
     * 查询工作流。
     *
     * @param query 工作流编码查询条件
     * @return Workflow 查询结果（可能为空）
     */
    @Override
    public Optional<Workflow> findByCode(WorkflowCodeQuery query) {
        if (query == null || !StringUtils.hasText(query.getWorkflowCode())) {
            return Optional.empty();
        }
        return Optional.ofNullable(workflowMapper.findByCode(query))
                .map(item -> BeanMappingUtils.map(item, Workflow.class));
    }

    /**
     * 创建并持久化工作流数据。
     *
     * @param workflow 工作流实体
     * @return 创建后的 Workflow 信息
     */
    @Override
    public Workflow insert(Workflow workflow) {
        if (workflow == null) {
            return null;
        }
        workflowMapper.insertWorkflow(BeanMappingUtils.map(workflow, WorkflowPO.class));
        return workflow;
    }

    /**
     * 更新工作流数据。
     *
     * @param workflow 工作流实体
     * @return 工作流更新条数
     */
    @Override
    public int updateById(Workflow workflow) {
        if (workflow == null || workflow.getId() == null) {
            return 0;
        }
        return workflowMapper.updateWorkflow(BeanMappingUtils.map(workflow, WorkflowPO.class));
    }

    /**
     * 根据筛选条件查询工作流列表。
     *
     * @param keyword 关键字
     * @param offset 分页偏移量
     * @param pageSize 分页大小
     * @return Workflow 列表
     */
    @Override
    public List<Workflow> list(String keyword, int offset, int pageSize) {
        int safeOffset = Math.max(offset, 0);
        int safeSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        return BeanMappingUtils.mapList(workflowMapper.list(keyword, safeOffset, safeSize), Workflow.class);
    }

    /**
     * 按条件统计业务数量。
     *
     * @param keyword 关键字
     * @return 统计数量
     */
    @Override
    public long count(String keyword) {
        return workflowMapper.count(keyword);
    }
}
