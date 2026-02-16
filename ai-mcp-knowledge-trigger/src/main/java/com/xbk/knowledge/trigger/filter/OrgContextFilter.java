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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTP 组织上下文过滤器。
 *
 * 职责：为每个 HTTP 请求注入 OrgContext，统一组织归属。
 
  * @author xiexu
  */
@Component
@RequiredArgsConstructor
public class OrgContextFilter extends OncePerRequestFilter {

    private final OrgContextService orgContextService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        OrgContext previous = OrgContextHolder.get();
        try {
            if (StpUtil.isLogin()) {
                Long userId = StpUtil.getLoginIdAsLong();
                OrgContext context = orgContextService.resolve(userId);
                OrgContextHolder.set(context);
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
