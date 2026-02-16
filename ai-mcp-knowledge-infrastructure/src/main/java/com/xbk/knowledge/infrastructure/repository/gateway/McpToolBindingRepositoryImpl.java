package com.xbk.knowledge.infrastructure.repository.gateway;

import com.xbk.knowledge.domain.model.entity.gateway.McpToolBinding;
import com.xbk.knowledge.domain.model.vo.common.IdQuery;
import com.xbk.knowledge.domain.model.vo.gateway.ToolBindingQuery;
import com.xbk.knowledge.domain.model.vo.gateway.ToolIdQuery;
import com.xbk.knowledge.domain.repository.gateway.McpToolBindingRepository;
import com.xbk.knowledge.infrastructure.mapper.gateway.McpToolBindingMapper;
import com.xbk.knowledge.types.context.OrgContextHolder;
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

    private Long currentOrgIdOrRoot() {
        Long orgId = OrgContextHolder.currentOrgIdOrNull();
        return orgId == null ? 1L : orgId;
    }

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
        if (query.getOrgId() == null) {
            query.setOrgId(currentOrgIdOrRoot());
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
        ToolIdQuery query = new ToolIdQuery(currentOrgIdOrRoot(), toolId);
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
        mapper.deleteToolBindingById(new IdQuery(currentOrgIdOrRoot(), id));
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
        mapper.deleteToolBindingByToolId(new ToolIdQuery(currentOrgIdOrRoot(), toolId));
    }
}
