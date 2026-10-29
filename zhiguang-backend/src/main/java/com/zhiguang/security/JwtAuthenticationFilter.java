package com.zhiguang.security;

import com.zhiguang.dto.AuthSnapshot;
import com.zhiguang.entity.User;
import com.zhiguang.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final TokenBlacklistService tokenBlacklistService;
    private final SseTicketService sseTicketService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String bearerToken = getBearerToken(request);
        if (StringUtils.hasText(bearerToken)) {
            try {
                Claims claims = tokenProvider.parseClaims(bearerToken);
                authenticateClaims(claims, request);
            } catch (Exception e) {
                log.debug("JWT 校验失败: {}", e.getMessage());
            }
        } else {
            String ticket = request.getParameter("ticket");
            if (StringUtils.hasText(ticket)) {
                Long userId = sseTicketService.consumeTicket(ticket);
                if (userId != null) {
                    authenticateSnapshot(userId, null, request);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateClaims(Claims claims, HttpServletRequest request) {
        String jti = tokenProvider.getTokenId(claims);
        if (tokenBlacklistService.isBlacklisted(jti)) {
            log.debug("Token 已被吊销: jti={}", jti);
            return;
        }

        Long userId = tokenProvider.getUserId(claims);
        int tokenSecurityVersion = tokenProvider.getSecurityVersion(claims);
        authenticateSnapshot(userId, tokenSecurityVersion, request);
    }

    private void authenticateSnapshot(Long userId, Integer tokenSecurityVersion, HttpServletRequest request) {
        Optional<AuthSnapshot> snapshot = userService.findAuthSnapshot(userId);
        if (snapshot.isEmpty()) {
            return;
        }

        AuthSnapshot authSnapshot = snapshot.get();
        if ("BANNED".equalsIgnoreCase(authSnapshot.getStatus())) {
            log.debug("用户 {} 已被封禁，Token 失效", userId);
            return;
        }

        int currentSecurityVersion = authSnapshot.getSecurityVersion() == null ? 0 : authSnapshot.getSecurityVersion();
        if (tokenSecurityVersion != null && currentSecurityVersion != tokenSecurityVersion) {
            log.debug("用户 {} 的认证凭据版本已过期", userId);
            return;
        }

        User principal = userService.buildPrincipal(authSnapshot);
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + authSnapshot.getRole())
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String getBearerToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
