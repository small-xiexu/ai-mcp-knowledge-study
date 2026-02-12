package com.xbk.knowledge.infrastructure.repository;

import com.xbk.knowledge.domain.model.entity.gateway.McpToolMapping;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.gateway.ToolMappingQuery;
import com.xbk.knowledge.domain.repository.gateway.McpToolMappingRepository;
import com.xbk.knowledge.infrastructure.mapper.McpToolMappingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

/**
 * 工具参数映射仓储实现
 *
 * @author xiexu
 */
@Repository
@RequiredArgsConstructor
public class McpToolMappingRepositoryImpl implements McpToolMappingRepository {

    private final McpToolMappingMapper mapper;

    @Override
    public List<McpToolMapping> findByToolIdAndMappingType(ToolMappingQuery query) {
        if (query == null || query.getToolId() == null || query.getMappingType() == null) {
            return Collections.emptyList();
        }
        return mapper.findByToolIdAndMappingType(query);
    }

    @Override
    public McpToolMapping save(McpToolMapping mapping) {
        if (mapping == null) {
            return null;
        }
        if (mapping.getId() == null) {
            mapper.insertToolMapping(mapping);
            return mapping;
        }
        mapper.updateToolMapping(mapping);
        return mapping;
    }

    @Override
    public void deleteById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return;
        }
        mapper.deleteToolMappingById(query);
    }

    @Override
    public void deleteByToolId(Long toolId) {
        if (toolId == null) {
            return;
        }
        mapper.deleteToolMappingByToolId(toolId);
    }
}
