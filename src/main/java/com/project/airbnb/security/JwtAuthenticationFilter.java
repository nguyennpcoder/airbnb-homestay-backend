package com.project.airbnb.security;

import com.project.airbnb.entity.NguoiDung;
import com.project.airbnb.repository.NguoiDungRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final NguoiDungRepository nguoiDungRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService,
            NguoiDungRepository nguoiDungRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.nguoiDungRepository = nguoiDungRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String jwt = null;
        final String email;
        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        } else if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    break;
                }
            }
        }

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            email = jwtService.extractUsername(jwt);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(email);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // Skip locked check for auth endpoints (they handle it themselves)
                    String requestUri = request.getRequestURI();
                    boolean isAuthEndpoint = requestUri.startsWith("/api/auth/");

                    if (!isAuthEndpoint) {
                        // Check if account is locked
                        nguoiDungRepository.findByEmail(email.trim().toLowerCase()).ifPresent(user -> {
                            if (Boolean.TRUE.equals(user.getBiKhoa())) {
                                try {
                                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                    response.setContentType("application/json");
                                    response.setCharacterEncoding("UTF-8");
                                    response.getWriter().write(
                                            "{\"locked\":true,\"message\":\"Tài khoản đã bị khóa. Vui lòng liên hệ admin@airbnb.com.vn để được hỗ trợ mở lại.\"}");
                                } catch (IOException e) {
                                    // ignore
                                }
                            }
                        });

                        // If response already committed (locked), skip setting auth
                        if (response.isCommitted()) {
                            return;
                        }
                    }

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Allow request to proceed (e.g. for public endpoints) even if token is invalid
        }
        filterChain.doFilter(request, response);
    }
}
