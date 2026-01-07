# Security Documentation

## Overview

This document outlines the comprehensive security measures implemented in the Email Reader Agent application. All critical vulnerabilities identified in the security audit have been addressed.

## Security Features Implemented

### 1. Authentication & Authorization

**API Key Authentication**
- All API endpoints (except health check) require authentication via X-API-Key header
- API key is configured via environment variable: `API_KEY`
- Failed authentication attempts are logged and audited
- Support for disabling security in development: `security.enabled=false`

**Usage:**
```bash
curl -H "X-API-Key: your-api-key-here" http://localhost:8080/api/v1/emails/count?senderEmail=test@example.com
```

### 2. CORS Protection

**Configuration:**
- Whitelisted origins only (no wildcard `*`)
- Configurable via environment variable: `CORS_ALLOWED_ORIGINS`
- Default: `http://localhost:3000,http://localhost:8080`
- Restricted methods: GET, POST, OPTIONS
- Credentials support with appropriate headers

**Configuration File:** `src/main/java/com/krysta/emailreader/config/CorsSecurityConfig.java`

### 3. Rate Limiting

**Per-IP Rate Limiting:**
- Token bucket algorithm implementation using Bucket4j
- Default: 10 requests per minute per IP address
- Configurable via: `RATE_LIMIT_RPM` environment variable
- HTTP 429 response when limit exceeded
- X-Rate-Limit-Remaining header in responses
- Rate limit violations are audited

**Configuration File:** `src/main/java/com/krysta/emailreader/filter/RateLimitFilter.java`

### 4. Error Handling & Information Disclosure

**Sanitized Error Messages:**
- No stack traces exposed in production
- Generic error messages for security issues
- Detailed errors logged server-side only
- Correlation IDs for error tracking
- Environment-based error detail control: `security.detailed-errors`

**Features:**
- Email addresses redacted in error messages
- IP addresses masked
- Sensitive data (passwords, tokens, keys) removed

**Configuration File:** `src/main/java/com/krysta/emailreader/exception/GlobalExceptionHandler.java`

### 5. Data Protection

**OAuth Token Encryption:**
- AES-256-GCM encryption for tokens at rest
- Secure key management with file permissions
- Automatic key generation and storage
- Encrypted DataStore implementation

**PII Logging Protection:**
- Email addresses masked in logs (e.g., `su****@example.com`)
- IP addresses masked (e.g., `192.168.*.*`)
- API keys and sensitive values redacted

**Configuration Files:**
- `src/main/java/com/krysta/emailreader/security/EncryptedDataStoreFactory.java`
- `src/main/java/com/krysta/emailreader/util/LogSanitizer.java`

### 6. Security Headers

**HTTP Security Headers:**
- `X-Frame-Options: DENY` - Prevents clickjacking
- `X-Content-Type-Options: nosniff` - Prevents MIME sniffing
- `X-XSS-Protection: 1; mode=block` - XSS protection
- `Content-Security-Policy` - Restricts resource loading
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Permissions-Policy` - Disables unnecessary browser features
- `Strict-Transport-Security` - HTTPS enforcement (when enabled)

**Configuration File:** `src/main/java/com/krysta/emailreader/filter/SecurityHeadersFilter.java`

### 7. HTTPS Support

**SSL/TLS Configuration:**
- Production profile includes SSL configuration
- Configurable keystore path and password
- HSTS header support
- HTTP to HTTPS redirect capability

**Configuration File:** `src/main/resources/application-prod.yml`

**Setup:**
```bash
# Generate self-signed certificate (development)
keytool -genkeypair -alias tomcat -keyalg RSA -keysize 2048 \
  -keystore keystore.p12 -validity 365 -storepass changeit

# Run with production profile
java -jar app.jar --spring.profiles.active=prod \
  -DSSL_KEYSTORE_PASSWORD=changeit -DAPI_KEY=your-secure-key
```

### 8. Input Validation

**Enhanced Email Validation:**
- Apache Commons Validator for RFC 5321 compliance
- Maximum length check (254 characters)
- Homograph attack prevention (non-ASCII character detection)
- IP address domain blocking
- Special character sanitization
- Suspicious pattern detection

**Configuration File:** `src/main/java/com/krysta/emailreader/service/EmailService.java`

### 9. Dependency Security

**Updated Dependencies:**
- Spring Boot: 3.2.11 (from 3.2.1)
- Google API Client: 2.7.0 (from 2.2.0)
- Gmail API: v1-rev20240520-2.0.0
- Bucket4j: 8.10.1 (rate limiting)
- Apache Commons Validator: 1.9.0

**OWASP Dependency Check:**
- Maven plugin configured
- Fails build on CVSS score ≥ 7
- Run with: `mvn dependency-check:check`

**Configuration File:** `pom.xml`

### 10. Audit Logging

**Comprehensive Security Logging:**
- API access logging (who, what, when, from where)
- Authentication success/failure
- Rate limit violations
- Input validation failures
- Gmail API errors
- Cache operations
- Security events

**Audit Log Format:**
```
[timestamp] | event_type | ip_address | details
```

**Log Files:**
- Application log: `logs/email-reader-agent.log`
- Audit log: `logs/audit.log`

**Configuration File:** `src/main/java/com/krysta/emailreader/service/AuditService.java`

### 11. Request Size Limits

**Protection Against Memory Exhaustion:**
- Maximum HTTP header size: 8KB
- Maximum file upload size: 1MB
- Maximum request size: 1MB
- Tomcat max swallow size: 2MB

**Configuration File:** `src/main/resources/application.yml`

### 12. Actuator Security

**Management Endpoints:**
- Require authentication
- Configurable separate port: `ACTUATOR_PORT`
- Limited exposure: health, info, metrics
- Details shown only when authorized

### 13. Swagger UI Protection

**API Documentation Security:**
- Disabled by default in production profile
- Configurable via: `SWAGGER_ENABLED` environment variable
- Public in development for ease of testing
- Authentication required when enabled in production

## Configuration

### Environment Variables

Required:
- `API_KEY` - API authentication key (strong, random value)

Optional:
- `CORS_ALLOWED_ORIGINS` - Comma-separated list of allowed origins
- `RATE_LIMIT_RPM` - Requests per minute (default: 10)
- `GMAIL_OAUTH_PORT` - OAuth callback port (default: 8888)
- `ACTUATOR_PORT` - Management endpoint port (default: 8080)
- `SWAGGER_ENABLED` - Enable Swagger UI (default: true in dev, false in prod)
- `SSL_ENABLED` - Enable HTTPS (default: false)
- `SSL_KEYSTORE` - Path to SSL keystore
- `SSL_KEYSTORE_PASSWORD` - Keystore password
- `SSL_KEY_ALIAS` - Key alias (default: tomcat)

### Application Profiles

**Development (`application.yml`):**
- Security enabled but lenient
- Swagger UI enabled
- Detailed logging
- HTTP only

**Production (`application-prod.yml`):**
- Security fully enforced
- Swagger UI disabled by default
- HTTPS required
- HSTS enabled
- Minimal logging
- No error details exposed

## Security Best Practices

### Deployment

1. **Always set a strong API key:**
   ```bash
   export API_KEY=$(openssl rand -base64 32)
   ```

2. **Enable HTTPS in production:**
   ```bash
   export SSL_ENABLED=true
   export SSL_KEYSTORE=/path/to/keystore.p12
   export SSL_KEYSTORE_PASSWORD=your-secure-password
   ```

3. **Restrict CORS origins:**
   ```bash
   export CORS_ALLOWED_ORIGINS=https://yourdomain.com
   ```

4. **Use separate port for actuator:**
   ```bash
   export ACTUATOR_PORT=8444
   ```

5. **Disable Swagger in production:**
   ```bash
   export SWAGGER_ENABLED=false
   ```

### Monitoring

1. **Review audit logs regularly:**
   ```bash
   tail -f logs/audit.log | grep "AUTH_FAILURE\|RATE_LIMIT_EXCEEDED"
   ```

2. **Set up alerts for:**
   - Multiple authentication failures from same IP
   - Rate limit violations
   - Validation failures
   - Security events

3. **Monitor metrics:**
   ```bash
   curl http://localhost:8444/actuator/metrics
   ```

### Regular Maintenance

1. **Run dependency vulnerability scans:**
   ```bash
   mvn dependency-check:check
   ```

2. **Update dependencies regularly:**
   ```bash
   mvn versions:display-dependency-updates
   ```

3. **Review logs for security events:**
   ```bash
   grep "SECURITY_EVENT" logs/audit.log
   ```

## Security Testing

### Manual Testing

1. **Test authentication:**
   ```bash
   # Should fail
   curl http://localhost:8080/api/v1/emails/count?senderEmail=test@example.com
   
   # Should succeed
   curl -H "X-API-Key: your-key" http://localhost:8080/api/v1/emails/count?senderEmail=test@example.com
   ```

2. **Test rate limiting:**
   ```bash
   for i in {1..15}; do 
     curl -H "X-API-Key: your-key" http://localhost:8080/api/v1/emails/count?senderEmail=test@example.com
   done
   ```

3. **Test input validation:**
   ```bash
   # Should fail
   curl -H "X-API-Key: your-key" "http://localhost:8080/api/v1/emails/count?senderEmail=invalid-email"
   ```

### Automated Security Testing

Run OWASP ZAP or similar tools:
```bash
# Using OWASP ZAP CLI
zap-cli quick-scan --self-contained --start-options '-config api.disablekey=true' \
  http://localhost:8080
```

## Incident Response

### Authentication Failures

1. Check audit logs for the IP address
2. Verify if it's legitimate traffic
3. Consider blocking the IP if malicious
4. Rotate API key if compromised

### Rate Limit Violations

1. Review the source IP and patterns
2. Determine if it's abuse or legitimate high traffic
3. Adjust rate limits if needed
4. Implement IP blocking for persistent abuse

### Data Breach

1. Immediately rotate all API keys
2. Review audit logs for unauthorized access
3. Check encrypted token storage integrity
4. Notify affected users if needed
5. Conduct security review

## Compliance

This implementation addresses:
- **OWASP Top 10** security risks
- **CWE/SANS Top 25** vulnerabilities
- **GDPR** data protection requirements (PII masking)
- **SOC 2** logging and audit requirements

## Security Contact

For security issues, please contact: support@krysta.com

## Change Log

### Version 1.0.0 - Security Hardening
- Implemented API key authentication
- Added CORS protection
- Implemented rate limiting
- Enhanced error handling
- Encrypted OAuth token storage
- Added security headers
- Implemented audit logging
- Enhanced input validation
- Updated all dependencies
- Added HTTPS support

---

**Last Updated:** January 7, 2026
