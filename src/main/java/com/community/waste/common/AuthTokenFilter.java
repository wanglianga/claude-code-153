package com.community.waste.common;

import com.community.waste.domain.DomainEnums.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * 演示用令牌鉴权：X-Auth-Token 命中 app.auth.tokens 中任一角色令牌即通过。
 */
@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    private final Map<String, Role> tokenToRole;

    public AuthTokenFilter(
            @Value("${app.auth.tokens.property:property-token}") String property,
            @Value("${app.auth.tokens.company:company-token}") String company,
            @Value("${app.auth.tokens.finance:finance-token}") String finance,
            @Value("${app.auth.tokens.crew:crew-token}") String crew,
            @Value("${app.auth.tokens.owner:owner-token}") String owner) {
        this.tokenToRole = Map.of(
                property, Role.PROPERTY,
                company, Role.COMPANY,
                finance, Role.FINANCE,
                crew, Role.CREW,
                owner, Role.OWNER);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/swagger-ui.html")
                || path.equals("/")
                || path.startsWith("/api/public/")
                || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String token = request.getHeader("X-Auth-Token");
            Role role = token == null ? null : tokenToRole.get(token);
            if (role == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"status\":401,\"message\":\"未认证或令牌无效，请在请求头设置 X-Auth-Token\"}");
                return;
            }
            CurrentUser.set(role, token);
            chain.doFilter(request, response);
        } finally {
            CurrentUser.clear();
        }
    }
}
