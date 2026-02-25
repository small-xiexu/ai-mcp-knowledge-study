package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.PermissionAppService;
import com.xbk.knowledge.domain.identity.model.entity.SysPermission;
import com.xbk.knowledge.domain.identity.model.valobj.PermissionPageQuery;
import com.xbk.knowledge.domain.identity.adapter.repository.IdentityRepository;
import com.xbk.knowledge.types.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 权限管理应用服务实现。
 *
 * 职责：应用层用例实现，用于编排权限查询流程。
 *
 * @author sxie
 */
@Service
@RequiredArgsConstructor
public class PermissionAppServiceImpl implements PermissionAppService {

    /**
     * 身份仓储。
     */
    private final IdentityRepository identityRepository;

    /**
     * 分页查询权限。
     * 
     * @param query 分页查询条件。
     * @return SysPermission 分页结果。
     */
    @Override
    public PageResult<SysPermission> queryPermissionPage(PermissionPageQuery query) {
        Integer offset = query.getOffset() == null ? 0 : query.getOffset();
        Integer pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        PermissionPageQuery normalizedQuery = new PermissionPageQuery(
                query.getResourceType(),
                query.getAction(),
                query.getStatus(),
                offset,
                pageSize
        );
        List<SysPermission> permissions = identityRepository.findPermissionPage(normalizedQuery);
        long total = identityRepository.countPermission(normalizedQuery);
        int pageNum = (offset / pageSize) + 1;
        return PageResult.of(permissions, total, pageNum, pageSize);
    }
}
