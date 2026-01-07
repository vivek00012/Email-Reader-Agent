# Email Reader Agent - Quick Reference Card

## 🚀 Quick Start Commands

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Or use the quick start script
./run.sh

# Run tests
mvn test
```

## 🌐 Important URLs

| Service | URL |
|---------|-----|
| **Swagger UI** | http://localhost:8080/swagger-ui.html |
| **API Docs** | http://localhost:8080/v3/api-docs |
| **Health Check** | http://localhost:8080/actuator/health |
| **Service Health** | http://localhost:8080/api/v1/emails/health |

## 📡 API Endpoints

### Count Emails
```bash
GET /api/v1/emails/count?senderEmail={email}
```

**Example:**
```bash
curl "http://localhost:8080/api/v1/emails/count?senderEmail=superman@example.com"
```

**Response:**
```json
{
  "senderEmail": "superman@example.com",
  "emailCount": 10,
  "cachedResult": false,
  "timestamp": "2026-01-07T10:30:00"
}
```

### Upload Credentials
```bash
POST /api/v1/emails/credentials
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/emails/credentials \
  -F "file=@/path/to/credentials.json"
```

### Clear Credentials
```bash
DELETE /api/v1/emails/credentials
```

**Example:**
```bash
curl -X DELETE http://localhost:8080/api/v1/emails/credentials
```

## 📝 Response Codes

| Code | Meaning | Description |
|------|---------|-------------|
| **200** | OK | Request successful |
| **400** | Bad Request | Invalid email format |
| **401** | Unauthorized | Gmail authentication failed |
| **429** | Too Many Requests | Rate limit exceeded |
| **500** | Server Error | Internal server error |

## 🔧 Configuration Files

| File | Purpose |
|------|---------|
| `pom.xml` | Maven dependencies and build config |
| `src/main/resources/application.yml` | Main configuration |
| `src/main/resources/application-dev.yml` | Development settings |
| `src/main/resources/credentials.json` | Gmail OAuth credentials (not in git) |
| `.gitignore` | Git ignore rules |

## 🗂️ Project Structure

```
src/main/java/com/krysta/emailreader/
├── EmailReaderApplication.java     # Main app
├── controller/                     # REST endpoints
├── service/                        # Business logic
├── dto/                            # Data transfer objects
├── config/                         # Configuration
└── exception/                      # Error handling
```

## 🔐 Gmail API Setup (TL;DR)

1. Go to https://console.cloud.google.com/
2. Create project → Enable Gmail API
3. Create OAuth 2.0 credentials (Desktop app)
4. Download as `src/main/resources/credentials.json`
5. Run app → Authorize in browser

## 🧪 Testing Commands

```bash
# All tests
mvn test

# Specific test
mvn test -Dtest=EmailServiceTest

# With coverage report
mvn clean test jacoco:report

# Skip tests (quick build)
mvn install -DskipTests
```

## 📦 Build Commands

```bash
# Clean and build
mvn clean install

# Package JAR
mvn clean package

# Run JAR
java -jar target/email-reader-agent-1.0.0.jar

# Build without tests
mvn clean install -DskipTests
```

## 🐛 Troubleshooting Quick Fixes

| Problem | Solution |
|---------|----------|
| Credentials not found | Upload via API: `curl -X POST /api/v1/emails/credentials -F "file=@credentials.json"` OR add to `src/main/resources/` |
| Port 8080 in use | Change port in `application.yml` or kill process: `lsof -ti:8080 \| xargs kill -9` |
| OAuth won't open | Check port 8888, use URL from console |
| Invalid grant error | Clear credentials: `curl -X DELETE /api/v1/emails/credentials` OR delete `tokens/` folder |
| Tests failing | Ensure no credentials required for unit tests |

## 💾 Cache Configuration

**Default Settings:**
- **TTL:** 5 minutes
- **Max Size:** 500 entries
- **Type:** In-memory (Caffeine)

**Customize in `application.yml`:**
```yaml
spring:
  cache:
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=10m
```

## 📊 Logging Levels

**Default:** INFO

**Change in `application.yml`:**
```yaml
logging:
  level:
    com.krysta.emailreader: DEBUG
    com.google.api: DEBUG
```

## 🔑 Key Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Spring Boot | 3.2.1 | Framework |
| Java | 17 | Language |
| Google API Client | 2.2.0 | Gmail integration |
| Caffeine | Latest | Caching |
| SpringDoc OpenAPI | 2.3.0 | API docs |
| Lombok | Latest | Code simplification |

## 📚 Documentation Files

| File | Content |
|------|---------|
| **README.md** | Complete documentation |
| **SETUP.md** | Quick setup guide |
| **IMPLEMENTATION_COMPLETE.md** | Implementation status and examples |
| **SECURITY.md** | Security documentation |
| **CONTRIBUTING.md** | Contribution guidelines |
| **PROJECT_SUMMARY.md** | Project overview |
| **QUICK_REFERENCE.md** | This file |

## 🎯 Common Use Cases

### 1. Check if app is running
```bash
curl http://localhost:8080/actuator/health
```

### 2. Upload credentials via API
```bash
curl -X POST http://localhost:8080/api/v1/emails/credentials \
  -F "file=@/path/to/credentials.json"
```

### 3. Count emails from sender
```bash
curl "http://localhost:8080/api/v1/emails/count?senderEmail=test@example.com"
```

### 4. View API documentation
```bash
open http://localhost:8080/swagger-ui.html
```

### 5. Clear credentials and tokens
```bash
curl -X DELETE http://localhost:8080/api/v1/emails/credentials
```

### 6. Run in development mode
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 7. Check cache statistics
Enable DEBUG logging for `org.springframework.cache`

## 🔄 Development Workflow

1. **Make changes** to source code
2. **Run tests:** `mvn test`
3. **Check for errors:** `mvn clean compile`
4. **Run locally:** `mvn spring-boot:run`
5. **Test endpoint:** Use Swagger UI or curl
6. **Build package:** `mvn clean package`

## 🌟 Pro Tips

- **Use Swagger UI** for interactive testing
- **Enable DEBUG logging** for troubleshooting
- **Check cache hits** in response (`cachedResult` field)
- **Monitor actuator** endpoints for health
- **Adjust cache TTL** based on usage patterns
- **Use `run.sh`** for quick starts

## 📞 Getting Help

1. Check **README.md** for detailed docs
2. See **SETUP.md** for setup issues
3. View **IMPLEMENTATION_COMPLETE.md** for examples
4. Open **Swagger UI** for API reference
5. Check **SECURITY.md** for security features
6. Check logs in console for errors

## 🎨 Useful Maven Commands

```bash
# Show dependency tree
mvn dependency:tree

# Update dependencies
mvn versions:display-dependency-updates

# Check for plugin updates
mvn versions:display-plugin-updates

# Generate project info
mvn site

# Clean target directory
mvn clean
```

## 🔍 Verify Installation

```bash
# 1. Check Java
java -version

# 2. Check Maven
mvn -version

# 3. Build project
mvn clean install

# 4. Run tests
mvn test

# 5. Start application
mvn spring-boot:run

# 6. Test endpoint
curl http://localhost:8080/api/v1/emails/health
```

---

**Quick Start:** `./run.sh` → **Swagger:** http://localhost:8080/swagger-ui.html

**Full Docs:** See README.md | **Support:** support@krysta.com
