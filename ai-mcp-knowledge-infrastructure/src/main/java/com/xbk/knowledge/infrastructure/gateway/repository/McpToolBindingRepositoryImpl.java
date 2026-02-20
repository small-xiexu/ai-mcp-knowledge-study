package com.xbk.knowledge.infrastructure.gateway.repository;

import com.xbk.knowledge.domain.gateway.model.entity.McpToolBinding;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolBindingQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolIdQuery;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolBindingRepository;
import com.xbk.knowledge.infrastructure.dao.IMcpToolBindingDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

/**
 * 工具绑定关系仓储实现
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class McpToolBindingRepositoryImpl implements McpToolBindingRepository {

    private final IMcpToolBindingDao mapper;

    /**
     * findByBindTypeAndTargetId。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public List<McpToolBinding> findByBindTypeAndTargetId(ToolBindingQuery query) {
        if (query == null || query.getBindType() == null || query.getBindTargetId() == null) {
            return Collections.emptyList();
        }
        return mapper.findByBindTypeAndTargetId(query);
    }

    /**
     * findByToolId。
     *
     * @param toolId 参数
     * @return 返回结果
     */
    @Override
    public List<McpToolBinding> findByToolId(Long toolId) {
        if (toolId == null) {
            return Collections.emptyList();
        }
        ToolIdQuery query = new ToolIdQuery(toolId);
        return mapper.findByToolId(query);
    }

    /**
     * save。
     *
     * @param binding 参数
     * @return 返回结果
     */
    @Override
    public McpToolBinding save(McpToolBinding binding) {
        if (binding == null) {
            return null;
        }
        if (binding.getId() == null) {
            mapper.insertToolBinding(binding);
            return binding;
        }
        mapper.updateToolBinding(binding);
        return binding;
    }

    /**
     * deleteById。
     *
     * @param id 参数
     */
    @Override
    public void deleteById(Long id) {
        if (id == null) {
            return;
        }
        mapper.deleteToolBindingById(new IdQuery(id));
    }

    /**
     * deleteByToolId。
     *
     * @param toolId 参数
     */
    @Override
    public void deleteByToolId(Long toolId) {
        if (toolId == null) {
            return;
        }
        mapper.deleteToolBindingByToolId(new ToolIdQuery(toolId));
    }
}
