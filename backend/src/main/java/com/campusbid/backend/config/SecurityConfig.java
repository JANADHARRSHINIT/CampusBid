package com.campusbid.backend.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .securityContext(context -> context.securityContextRepository(securityContextRepository()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        // Preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Authentication endpoints
                        .requestMatchers("/api/auth/**").permitAll()

                        // Static uploaded images
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                        // Public browsing of listings & bid history
                        .requestMatchers(HttpMethod.GET, "/api/listings").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/listings/all").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/listings/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/listings/{id}/bids").permitAll()

                        // Administrator only
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Authenticated actions (STUDENT or ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/listings/upload-images").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/listings").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/listings/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/listings/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/listings/*/bids").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/listings/my-listings").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/listings/pending-decisions").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/listings/won").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/listings/*/decision").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/listings/*/approve").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/listings/*/reject").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/listings/*/handover").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/listings/*/cancel").authenticated()

                        // Notifications require authentication
                        .requestMatchers("/api/notifications/**").authenticated()

                        // Wanted Items Smart Alerts require authentication
                        .requestMatchers("/api/wanted-items", "/api/wanted-items/**").authenticated()

                        // AI Copilot interactions
                        .requestMatchers("/api/copilot/**").authenticated()

                        // Any other request
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"success\":false,\"message\":\"Authentication required. Please sign in to access this feature.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"success\":false,\"message\":\"Access denied. You do not have permission to perform this action.\"}");
                        })
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "http://localhost:5174",
                "http://localhost:3000",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:5174"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(Arrays.asList("X-Student-Id", "Set-Cookie"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
