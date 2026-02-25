package com.xbk.knowledge.infrastructure.gateway.repository;

import com.xbk.knowledge.domain.gateway.model.entity.McpToolBinding;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolBindingQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolIdQuery;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolBindingRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IMcpToolBindingDao;
import com.xbk.knowledge.infrastructure.dao.po.McpToolBindingPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

/**
 * 工具绑定关系仓储实现。
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class McpToolBindingRepositoryImpl implements McpToolBindingRepository {

    /**
     * 工具绑定 DAO。
     */
    private final IMcpToolBindingDao mapper;

    /**
     * 查询MCP 工具绑定。
     *
     * @param query 绑定目标查询条件
     * @return McpToolBinding 列表
     */
    @Override
    public List<McpToolBinding> findByBindTypeAndTargetId(ToolBindingQuery query) {
        if (query == null || query.getBindType() == null || query.getBindTargetId() == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(mapper.findByBindTypeAndTargetId(query), McpToolBinding.class);
    }

    /**
     * 查询MCP 工具绑定。
     *
     * @param toolId 工具 ID
     * @return McpToolBinding 列表
     */
    @Override
    public List<McpToolBinding> findByToolId(Long toolId) {
        if (toolId == null) {
            return Collections.emptyList();
        }
        ToolIdQuery query = new ToolIdQuery(toolId);
        return BeanMappingUtils.mapList(mapper.findByToolId(query), McpToolBinding.class);
    }

    /**
     * 创建或更新MCP 工具绑定数据。
     *
     * @param binding 绑定实体
     * @return 保存后的 McpToolBinding 信息
     */
    @Override
    public McpToolBinding save(McpToolBinding binding) {
        if (binding == null) {
            return null;
        }
        if (binding.getId() == null) {
            mapper.insertToolBinding(BeanMappingUtils.map(binding, McpToolBindingPO.class));
            return binding;
        }
        mapper.updateToolBinding(BeanMappingUtils.map(binding, McpToolBindingPO.class));
        return binding;
    }

    /**
     * 删除MCP 工具绑定数据。
     *
     * @param id 主键 ID
     */
    @Override
    public void deleteById(Long id) {
        if (id == null) {
            return;
        }
        mapper.deleteToolBindingById(new IdQuery(id));
    }

    /**
     * 删除MCP 工具绑定数据。
     *
     * @param toolId 工具 ID
     */
    @Override
    public void deleteByToolId(Long toolId) {
        if (toolId == null) {
            return;
        }
        mapper.deleteToolBindingByToolId(new ToolIdQuery(toolId));
    }

    /**
     * 删除MCP 工具绑定数据。
     *
     * @param query 待删除的绑定目标查询条件
     */
    @Override
    public void deleteByBindTypeAndTargetId(ToolBindingQuery query) {
        if (query == null || query.getBindType() == null || query.getBindTargetId() == null) {
            return;
        }
        mapper.deleteByBindTypeAndTargetId(query);
    }
}
