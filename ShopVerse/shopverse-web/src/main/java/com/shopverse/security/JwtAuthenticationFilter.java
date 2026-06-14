package com.shopverse.security;

import com.shopverse.infrastructure.redis.RedisSessionStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Ch07-06: JWT filter — validates Bearer token on every request.
 * Extends OncePerRequestFilter — guaranteed single execution per request.
 * Ch06-01: Checks Redis blocklist so logged-out tokens are rejected immediately.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider    jwtTokenProvider;
    private final RedisSessionStore   redisSessionStore;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   RedisSessionStore redisSessionStore) {
        this.jwtTokenProvider  = jwtTokenProvider;
        this.redisSessionStore = redisSessionStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && jwtTokenProvider.isValid(token)
                && !redisSessionStore.isBlocklisted(token)) {
            String username = jwtTokenProvider.getUsername(token);
            String role     = jwtTokenProvider.getRole(token);
            var auth = new UsernamePasswordAuthenticationToken(
                    username, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
