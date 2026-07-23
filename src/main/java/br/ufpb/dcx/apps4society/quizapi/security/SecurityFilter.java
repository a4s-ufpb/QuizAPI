package br.ufpb.dcx.apps4society.quizapi.security;

import br.ufpb.dcx.apps4society.quizapi.service.UserDetailsServiceImpl;
import br.ufpb.dcx.apps4society.quizapi.service.exception.TokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class SecurityFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(SecurityFilter.class);

    private TokenProvider tokenProvider;
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    public SecurityFilter(TokenProvider tokenProvider, UserDetailsServiceImpl userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        String method = request.getMethod();
        String path = request.getRequestURI();
        String ip = resolveClientIp(request);

        MDC.put("requestId", requestId);

        try {
            String token = extractToken(request);

            if (token != null) {
                try {
                    String email = tokenProvider.getSubjectByToken(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    MDC.put("user", email);
                    log.debug("{} {} | ip={} | autenticado: {}", method, path, ip, email);
                } catch (TokenException e) {
                    log.warn("{} {} | ip={} | token rejeitado: {}", method, path, ip, e.getMessage());
                }
            } else {
                log.debug("{} {} | ip={} | sem token (anônimo)", method, path, ip);
            }

            filterChain.doFilter(request, response);

        } finally {
            MDC.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        String token = header.substring("Bearer ".length()).strip();
        return token.isBlank() ? null : token;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }
}
