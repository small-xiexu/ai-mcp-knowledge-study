package com.xbk.knowledge.infrastructure.repository.gateway;

import com.xbk.knowledge.domain.model.entity.gateway.McpToolMapping;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.gateway.ToolMappingQuery;
import com.xbk.knowledge.domain.model.vo.gateway.ToolIdQuery;
import com.xbk.knowledge.domain.repository.gateway.McpToolMappingRepository;
import com.xbk.knowledge.infrastructure.mapper.gateway.McpToolMappingMapper;
import com.xbk.knowledge.types.context.OrgContextHolder;
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

    private Long currentOrgIdOrRoot() {
        Long orgId = OrgContextHolder.currentOrgIdOrNull();
        return orgId == null ? 1L : orgId;
    }

    /**
     * findByToolIdAndMappingType。
     *
     * @param query 参数
     * @return 返回结果
     */
    @Override
    public List<McpToolMapping> findByToolIdAndMappingType(ToolMappingQuery query) {
        if (query == null || query.getToolId() == null || query.getMappingType() == null) {
            return Collections.emptyList();
        }
        if (query.getOrgId() == null) {
            query.setOrgId(currentOrgIdOrRoot());
        }
        return mapper.findByToolIdAndMappingType(query);
    }

    /**
     * save。
     *
     * @param mapping 参数
     * @return 返回结果
     */
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

    /**
     * deleteById。
     *
     * @param query 参数
     */
    @Override
    public void deleteById(IdQuery query) {
        if (query == null || query.getId() == null) {
            return;
        }
        if (query.getOrgId() == null) {
            query.setOrgId(currentOrgIdOrRoot());
        }
        mapper.deleteToolMappingById(query);
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
        mapper.deleteToolMappingByToolId(new ToolIdQuery(currentOrgIdOrRoot(), toolId));
    }
}
