package com.project.airbnb.config;

import com.project.airbnb.security.JwtAuthenticationFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity

public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Explicitly allow CORS preflight and multipart uploads
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Health check endpoints (for Render)
                        .requestMatchers("/", "/healthz", "/actuator/**").permitAll()
                        .requestMatchers("/api/health/**").permitAll()
                        // Public/auth endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll() // Allow access to uploaded files
                        .requestMatchers("/api/payment/**").permitAll() // Allow payment callbacks

                        // WebSocket endpoint
                        .requestMatchers("/ws/**").permitAll()

                        // User profile — GET for public viewing, mutating methods require auth
//                        .requestMatchers(HttpMethod.GET, "/api/users/**").permitAll()

                        // Read-only data that should be visible to all guests
                        .requestMatchers("/api/host/**").permitAll()
                        .requestMatchers("/api/users/**").permitAll()
                        .requestMatchers("/api/images/**").permitAll()
                        .requestMatchers("/api/danh-gia/**").permitAll()
                        .requestMatchers("/api/yeu-thich/**").permitAll()
                        .requestMatchers("/api/thong-bao/**").permitAll()
                        .requestMatchers("/api/tin-nhan/**").permitAll()
                        .requestMatchers("/api/dat-cho/**").permitAll()
                        .requestMatchers("/api/lich-trinh/**").permitAll()
                        .requestMatchers("/api/quick-replies/**").permitAll() // Allow for testing
                        .requestMatchers("/api/admin/**").permitAll() // Admin endpoints (protected by AdminGuard on frontend)
                        .requestMatchers("/api/availability/**").permitAll()
                        .requestMatchers("/api/chinh-sach-huy/**").permitAll()
                        .requestMatchers("/api/quy-dinh/**").permitAll()

                        // Everything else requires authentication
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:}") String allowedOriginsCsv) {
        CorsConfiguration configuration = new CorsConfiguration();
        java.util.List<String> origins = new java.util.ArrayList<>(Arrays.asList(
                "http://localhost:4301", "http://localhost:3000", "http://localhost:3001",
                "http://127.0.0.1:3000", "http://127.0.0.1:4301",
                "https://airbnb-homestay.onrender.com",
                "https://airbnb-homestay-frontend.onrender.com"));
        if (allowedOriginsCsv != null && !allowedOriginsCsv.isBlank()) {
            for (String o : allowedOriginsCsv.split(",")) {
                String trimmed = o.trim();
                if (!trimmed.isEmpty()) origins.add(trimmed);
            }
        }
        // If origin is not in the explicit list (e.g. a Vercel preview URL),
        // echo the request Origin back so credentials still work.
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
