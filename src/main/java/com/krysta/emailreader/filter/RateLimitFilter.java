package com.krysta.emailreader.filter;

import com.krysta.emailreader.service.AuditService;
import com.krysta.emailreader.util.LogSanitizer;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter using token bucket algorithm.
 * Implements per-IP rate limiting to prevent API abuse.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    private final AuditService auditService;
    
    @Value("${rate-limit.requests-per-minute:10}")
    private int requestsPerMinute;
    
    @Value("${rate-limit.enabled:true}")
    private boolean rateLimitEnabled;
    
    public RateLimitFilter(AuditService auditService) {
        this.auditService = auditService;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // Skip rate limiting for health check, OAuth endpoints, credentials upload, and Swagger UI endpoints
        if (requestPath.equals("/api/v1/emails/health") ||
            requestPath.startsWith("/api/v1/emails/oauth") ||
            requestPath.startsWith("/api/v1/emails/credentials") ||
            requestPath.startsWith("/swagger-ui") ||
            requestPath.startsWith("/v3/api-docs") ||
            requestPath.equals("/swagger-ui.html")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Skip if rate limiting is disabled
        if (!rateLimitEnabled) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String clientIp = getClientIpAddress(request);
        Bucket bucket = cache.computeIfAbsent(clientIp, k -> createNewBucket());
        
        if (bucket.tryConsume(1)) {
            long remainingTokens = bucket.getAvailableTokens();
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(remainingTokens));
            filterChain.doFilter(request, response);
        } else {
            logger.warn("Rate limit exceeded for IP: {}", LogSanitizer.maskIpAddress(clientIp));
            auditService.logRateLimitViolation(clientIp, request.getRequestURI());
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("X-Rate-Limit-Retry-After", "60");
            response.getWriter().write(
                "{\"status\":429,\"message\":\"Rate limit exceeded. Please try again later.\"}"
            );
        }
    }
    
    /**
     * Creates a new rate limit bucket with configured limits.
     */
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(requestsPerMinute)
            .refillIntervally(requestsPerMinute, Duration.ofMinutes(1))
            .build();
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }
    
    /**
     * Extract client IP address from request, considering proxy headers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
