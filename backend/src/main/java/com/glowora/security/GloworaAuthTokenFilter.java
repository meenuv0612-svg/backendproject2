package com.glowora.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates requests bearing a valid Glowora HMAC token in the Authorization header
 * when an HttpSession cookie is absent or blocked by cross-site browser policies.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GloworaAuthTokenFilter extends OncePerRequestFilter {

    private final GloworaTokenService tokenService;
    private final CustomerUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
            boolean alreadyAuthenticated = existingAuth != null
                    && existingAuth.isAuthenticated()
                    && !(existingAuth instanceof AnonymousAuthenticationToken);

            if (!alreadyAuthenticated) {
                String token = extractBearerToken(request);
                if (token != null) {
                    GloworaTokenService.TokenClaims claims = tokenService.validateAndParse(token);
                    if (claims != null && claims.email() != null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(claims.email());
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContext context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(authToken);
                        SecurityContextHolder.setContext(context);
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("Could not set authentication from Bearer token: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String raw = authHeader.substring(7).trim();
            if (!raw.isEmpty() && !"null".equalsIgnoreCase(raw) && !"undefined".equalsIgnoreCase(raw)) {
                return raw;
            }
        }
        String customHeader = request.getHeader("X-Auth-Token");
        if (customHeader != null && !customHeader.isBlank()) {
            return customHeader.trim();
        }
        return null;
    }
}
