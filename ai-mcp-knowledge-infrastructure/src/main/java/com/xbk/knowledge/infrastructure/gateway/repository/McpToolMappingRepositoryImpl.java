package com.xbk.knowledge.infrastructure.gateway.repository;

import com.xbk.knowledge.domain.gateway.model.entity.McpToolMapping;
import com.xbk.knowledge.domain.common.model.valobj.IdQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolMappingQuery;
import com.xbk.knowledge.domain.gateway.model.valobj.ToolIdQuery;
import com.xbk.knowledge.domain.gateway.adapter.repository.McpToolMappingRepository;
import com.xbk.knowledge.infrastructure.common.BeanMappingUtils;
import com.xbk.knowledge.infrastructure.dao.IMcpToolMappingDao;
import com.xbk.knowledge.infrastructure.dao.po.McpToolMappingPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

/**
 * 工具参数映射仓储实现
 *
 * @author sxie
 */
@Repository
@RequiredArgsConstructor
public class McpToolMappingRepositoryImpl implements McpToolMappingRepository {

    /**
     * 工具映射 DAO。
     */
    private final IMcpToolMappingDao mapper;

    /**
     * 查询MCP 工具映射。
     * 
     * @param query 工具映射查询条件。
     * @return McpToolMapping 列表数据。
     */
    @Override
    public List<McpToolMapping> findByToolIdAndMappingType(ToolMappingQuery query) {
        if (query == null || query.getToolId() == null || query.getMappingType() == null) {
            return Collections.emptyList();
        }
        return BeanMappingUtils.mapList(mapper.findByToolIdAndMappingType(query), McpToolMapping.class);
    }

    /**
     * 创建或更新MCP 工具映射数据。
     * 
     * @param mapping 工具映射配置。
     * @return McpToolMapping 数据。
     */
    @Override
    public McpToolMapping save(McpToolMapping mapping) {
        if (mapping == null) {
            return null;
        }
        if (mapping.getId() == null) {
            mapper.insertToolMapping(BeanMappingUtils.map(mapping, McpToolMappingPO.class));
            return mapping;
        }
        mapper.updateToolMapping(BeanMappingUtils.map(mapping, McpToolMappingPO.class));
        return mapping;
    }

    /**
     * 删除MCP 工具映射数据。
     * 
     * @param query 主键查询条件。
     */
    @Override
    public void deleteById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return;
        }
        mapper.deleteToolMappingById(query);
    }

    /**
     * 删除MCP 工具映射数据。
     * 
     * @param toolId 工具 ID
     */
    @Override
    public void deleteByToolId(Long toolId) {
        if (toolId == null) {
            return;
        }
        mapper.deleteToolMappingByToolId(new ToolIdQuery(toolId));
    }
}
