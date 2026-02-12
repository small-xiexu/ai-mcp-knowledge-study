package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.gateway.McpToolBinding;
import com.xbk.knowledge.domain.model.vo.gateway.ToolBindingQuery;
import com.xbk.knowledge.domain.repository.gateway.McpToolBindingRepository;
import com.xbk.knowledge.infrastructure.mapper.McpToolBindingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

/**
 * 工具绑定关系仓储实现
 *
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class McpToolBindingRepositoryImpl implements McpToolBindingRepository {

    private final McpToolBindingMapper mapper;

    @Override
    public List<McpToolBinding> findByBindTypeAndTargetId(ToolBindingQuery query) {
        if (query == null || query.getBindType() == null || query.getBindTargetId() == null) {
            return Collections.emptyList();
        }
        return mapper.findByBindTypeAndTargetId(query);
    }

    @Override
    public List<McpToolBinding> findByToolId(Long toolId) {
        if (toolId == null) {
            return Collections.emptyList();
        }
        return mapper.findByToolId(toolId);
    }

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

    @Override
    public void deleteById(Long id) {
        if (id == null) {
            return;
        }
        mapper.deleteToolBindingById(id);
    }

    @Override
    public void deleteByToolId(Long toolId) {
        if (toolId == null) {
            return;
        }
        mapper.deleteToolBindingByToolId(toolId);
    }

    @Override
    public void saveAll(List<McpToolBinding> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            return;
        }
        for (McpToolBinding binding : bindings) {
            save(binding);
        }
    }
}
