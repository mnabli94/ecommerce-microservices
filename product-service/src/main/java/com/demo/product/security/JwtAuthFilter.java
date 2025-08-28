package com.demo.product.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;

    public JwtAuthFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        var auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            var token = auth.substring(7).trim();
            try {
                var claims = jwt.parse(token).getBody();
                var username = claims.getSubject();
                if (username != null && !username.isBlank()) {
//                    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
                    var roles = (List<String>) claims.getOrDefault("roles", List.of("USER"));
                    var authorities = roles.stream()
                            .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                            .map(SimpleGrantedAuthority::new)
                            .toList();
                    var authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

            } catch (Exception e) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

//    @Override
//    protected boolean shouldNotFilter(HttpServletRequest request) {
//        String p = request.getServletPath();
//        return p.startsWith("/auth/") || p.startsWith("/swagger-ui/") || p.startsWith("/v3/api-docs/") || p.startsWith("/actuator/health");
//    }
}
