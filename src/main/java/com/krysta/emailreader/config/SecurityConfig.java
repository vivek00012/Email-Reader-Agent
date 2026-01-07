package com.krysta.emailreader.config;

import com.krysta.emailreader.filter.RateLimitFilter;
import com.krysta.emailreader.filter.SecurityHeadersFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security configuration for API authentication and authorization.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final RateLimitFilter rateLimitFilter;
    private final SecurityHeadersFilter securityHeadersFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    
    public SecurityConfig(RateLimitFilter rateLimitFilter,
                         SecurityHeadersFilter securityHeadersFilter,
                         CorsConfigurationSource corsConfigurationSource) {
        this.rateLimitFilter = rateLimitFilter;
        this.securityHeadersFilter = securityHeadersFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Enable CORS with custom configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            
            // Disable CSRF as this is a stateless API with token-based auth
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure authorization rules
            .authorizeHttpRequests(auth -> {
                // Allow all requests (no authentication)
                auth.anyRequest().permitAll();
            })
            
            // Stateless session management
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Add security headers filter first
            .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Add rate limiting filter
            .addFilterAfter(rateLimitFilter, SecurityHeadersFilter.class);
        
        return http.build();
    }
}
