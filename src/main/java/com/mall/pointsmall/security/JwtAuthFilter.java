package com.mall.pointsmall.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.pointsmall.common.ApiResponse;
import com.mall.pointsmall.enums.RoleType;
import com.mall.pointsmall.service.CustomerSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtTokenProvider tokenProvider;
    private final CustomerSessionService customerSessionService;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtTokenProvider tokenProvider,
                         CustomerSessionService customerSessionService,
                         ObjectMapper objectMapper) {
        this.tokenProvider = tokenProvider;
        this.customerSessionService = customerSessionService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/swagger-ui/") || path.startsWith("/v3/api-docs/")
                || path.startsWith("/swagger-resources/") || path.startsWith("/webjars/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                if (!"access".equals(tokenProvider.tokenType(token))) {
                    unauthorized(response);
                    return;
                }
                SecurityUser user = tokenProvider.parse(token);
                if (user.getRole() == RoleType.CUSTOMER
                        && !customerSessionService.isActive(user.getId(), tokenProvider.sessionId(token))) {
                    unauthorized(response);
                    return;
                }
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ignored) {
                unauthorized(response);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail("登录超时，请重新登录"));
    }
}
