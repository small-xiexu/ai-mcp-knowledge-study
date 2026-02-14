package com.xbk.knowledge.trigger.filter;

import cn.dev33.satoken.stp.StpUtil;
import com.xbk.knowledge.application.service.app.OrgContextService;
import com.xbk.knowledge.types.context.OrgContext;
import com.xbk.knowledge.types.context.OrgContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTP 组织上下文过滤器。
 *
 * 职责：为每个 HTTP 请求注入 OrgContext，支撑：
 * - 部门隔离（org_id 过滤与写入归属）
 * - 超管跨组织管理需显式选择 targetOrgId
 */
@Component
@RequiredArgsConstructor
public class OrgContextFilter extends OncePerRequestFilter {

    /**
     * 超管跨组织管理的目标组织ID请求头。
     */
    public static final String TARGET_ORG_HEADER = "X-Target-Org-Id";

    private final OrgContextService orgContextService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        OrgContext previous = OrgContextHolder.get();
        try {
            if (StpUtil.isLogin()) {
                Long userId = StpUtil.getLoginIdAsLong();
                String targetOrgIdText = request.getHeader(TARGET_ORG_HEADER);
                OrgContext context = orgContextService.resolve(userId, targetOrgIdText);
                OrgContextHolder.set(context);
                if (context != null && StringUtils.hasText(targetOrgIdText)) {
                    response.setHeader(TARGET_ORG_HEADER, targetOrgIdText);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            if (previous != null) {
                OrgContextHolder.set(previous);
            } else {
                OrgContextHolder.clear();
            }
        }
    }
}

