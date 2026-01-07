package com.krysta.emailreader.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to add security headers to HTTP responses.
 * Implements OWASP recommended security headers.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {
    
    @Value("${security.headers.enabled:true}")
    private boolean headersEnabled;
    
    @Value("${security.headers.hsts-enabled:false}")
    private boolean hstsEnabled;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        
        if (headersEnabled) {
            // Prevent clickjacking attacks
            response.setHeader("X-Frame-Options", "DENY");
            
            // Prevent MIME type sniffing
            response.setHeader("X-Content-Type-Options", "nosniff");
            
            // Enable XSS protection
            response.setHeader("X-XSS-Protection", "1; mode=block");
            
            // Content Security Policy
            response.setHeader("Content-Security-Policy", 
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data:; " +
                "font-src 'self' data:; " +
                "connect-src 'self'; " +
                "frame-ancestors 'none'");
            
            // Referrer Policy
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            
            // Permissions Policy (formerly Feature Policy)
            response.setHeader("Permissions-Policy", 
                "geolocation=(), microphone=(), camera=()");
            
            // HTTP Strict Transport Security (only if HTTPS is enabled)
            if (hstsEnabled) {
                response.setHeader("Strict-Transport-Security", 
                    "max-age=31536000; includeSubDomains; preload");
            }
            
            // Remove server version header
            response.setHeader("Server", "");
        }
        
        filterChain.doFilter(request, response);
    }
}
