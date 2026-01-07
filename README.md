# Email Reader Agent

A production-ready Spring Boot REST API service that integrates with Gmail to count emails from specific senders. The service features OAuth 2.0 authentication, intelligent caching, comprehensive error handling, and interactive Swagger documentation.

## Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Gmail API Setup](#gmail-api-setup)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [API Usage Examples](#api-usage-examples)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Architecture](#architecture)
- [License](#license)

## Features

- ✅ **Gmail Integration** - Seamlessly integrates with Gmail API using OAuth 2.0
- ✅ **Credentials Management** - Upload/delete credentials via API (file or in-memory)
- ✅ **Email Counting** - Accurately counts emails from any specified sender
- ✅ **Intelligent Caching** - Reduces API calls with Caffeine cache (5-minute TTL)
- ✅ **RESTful API** - Clean, well-documented REST endpoints
- ✅ **Swagger UI** - Interactive API documentation and testing interface
- ✅ **Error Handling** - Comprehensive exception handling with meaningful error messages
- ✅ **Input Validation** - Validates email formats before processing
- ✅ **Pagination Support** - Handles large email volumes efficiently
- ✅ **Production Ready** - Includes logging, health checks, and monitoring endpoints

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java 17** or higher ([Download](https://adoptium.net/))
- **Maven 3.6+** ([Download](https://maven.apache.org/download.cgi))
- **Gmail Account** with API access
- **Google Cloud Platform Account** (free tier available)

## Gmail API Setup

Follow these steps to set up Gmail API access:

### Step 1: Create a Google Cloud Project

1. Go to the [Google Cloud Console](https://console.cloud.google.com/)
2. Click on **"Select a project"** dropdown at the top
3. Click **"New Project"**
4. Enter a project name (e.g., "Email Reader Agent")
5. Click **"Create"**

### Step 2: Enable Gmail API

1. In your project, navigate to **"APIs & Services"** > **"Library"**
2. Search for **"Gmail API"**
3. Click on **"Gmail API"** from the results
4. Click **"Enable"**

### Step 3: Create OAuth 2.0 Credentials

1. Navigate to **"APIs & Services"** > **"Credentials"**
2. Click **"Create Credentials"** > **"OAuth client ID"**
3. If prompted, configure the OAuth consent screen:
   - Choose **"External"** user type
   - Fill in the required fields (App name, User support email, Developer contact)
   - Add your email to **"Test users"**
   - Click **"Save and Continue"** through the remaining steps
4. Back in the credentials page, click **"Create Credentials"** > **"OAuth client ID"**
5. Select **"Desktop app"** as the application type
6. Enter a name (e.g., "Email Reader Desktop Client")
7. Click **"Create"**

### Step 4: Download Credentials

1. After creating the OAuth client, click the **Download** button (⬇️) next to your credential
2. Save the file as **`credentials.json`**
3. Move `credentials.json` to: `src/main/resources/credentials.json`

**⚠️ IMPORTANT:** Never commit `credentials.json` to version control. It's already included in `.gitignore`.

### Step 5: First-Time OAuth Authorization

When you run the application for the first time:

1. The application will open a browser window automatically
2. Select your Google account
3. Click **"Allow"** to grant read-only Gmail access
4. The authorization token will be saved in the `tokens/` directory
5. Subsequent runs will use the saved token

## Installation

### Clone the Repository

```bash
git clone <repository-url>
cd Email-Reader-Agent
```

### Build the Project

```bash
mvn clean install
```

This will:
- Download all dependencies
- Compile the source code
- Run tests
- Package the application

## Configuration

### Application Properties

The main configuration is in `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: email-reader-agent
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=500,expireAfterWrite=5m

server:
  port: 8080

gmail:
  application-name: Email Reader Agent
  credentials-file: /credentials.json
  tokens-directory: tokens
  scopes:
    - https://www.googleapis.com/auth/gmail.readonly
```

### Customization Options

You can override settings by creating `application-local.yml`:

```yaml
server:
  port: 9090  # Change port

spring:
  cache:
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=10m  # Adjust cache settings

logging:
  level:
    com.krysta.emailreader: TRACE  # More verbose logging
```

## Running the Application

### Option 1: Using Maven

```bash
mvn spring-boot:run
```

### Option 2: Using Java JAR

```bash
mvn clean package
java -jar target/email-reader-agent-1.0.0.jar
```

### Option 3: Development Mode

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Verify the Application is Running

```bash
curl http://localhost:8080/api/v1/emails/health
```

Expected response: `Email Reader Agent is running`

## API Documentation

### Swagger UI

Once the application is running, access the interactive API documentation:

**URL:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

The Swagger UI provides:
- Complete API documentation
- Interactive testing interface
- Request/response examples
- Schema definitions

### OpenAPI Specification

**URL:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## API Usage Examples

### Count Emails from a Sender

#### Using cURL

```bash
curl -X GET "http://localhost:8080/api/v1/emails/count?senderEmail=superman@example.com"
```

#### Response (200 OK)

```json
{
  "senderEmail": "superman@example.com",
  "emailCount": 10,
  "cachedResult": false,
  "timestamp": "2026-01-07T10:30:00"
}
```

#### Using HTTPie

```bash
http GET "http://localhost:8080/api/v1/emails/count" senderEmail=="superman@example.com"
```

#### Using JavaScript (Fetch API)

```javascript
fetch('http://localhost:8080/api/v1/emails/count?senderEmail=superman@example.com')
  .then(response => response.json())
  .then(data => console.log(`Email count: ${data.emailCount}`))
  .catch(error => console.error('Error:', error));
```

#### Using Python (Requests)

```python
import requests

response = requests.get(
    'http://localhost:8080/api/v1/emails/count',
    params={'senderEmail': 'superman@example.com'}
)

if response.status_code == 200:
    data = response.json()
    print(f"Email count: {data['emailCount']}")
```

### Error Responses

#### Invalid Email Format (400 Bad Request)

```json
{
  "status": 400,
  "message": "Invalid email format: not-an-email",
  "timestamp": "2026-01-07T10:30:00",
  "path": "/api/v1/emails/count"
}
```

#### Authentication Failure (401 Unauthorized)

```json
{
  "status": 401,
  "message": "Gmail authentication failed. Please check your credentials.",
  "timestamp": "2026-01-07T10:30:00",
  "path": "/api/v1/emails/count"
}
```

#### Rate Limit Exceeded (429 Too Many Requests)

```json
{
  "status": 429,
  "message": "Gmail API rate limit exceeded. Please try again later.",
  "timestamp": "2026-01-07T10:30:00",
  "path": "/api/v1/emails/count"
}
```

### Upload Gmail Credentials

Upload credentials.json file via API:

```bash
curl -X POST http://localhost:8080/api/v1/emails/credentials \
  -F "file=@/path/to/credentials.json"
```

#### Response (200 OK)

```json
{
  "status": "success",
  "message": "Credentials stored successfully",
  "filename": "credentials.json",
  "size": "542",
  "timestamp": "2026-01-08T01:30:00"
}
```

### Clear Credentials

Remove stored credentials and delete OAuth tokens:

```bash
curl -X DELETE http://localhost:8080/api/v1/emails/credentials
```

#### Response (200 OK)

```json
{
  "status": "success",
  "message": "Credentials cleared, tokens deleted, and cache invalidated successfully",
  "timestamp": "2026-01-08T01:30:00"
}
```

### Health Check

```bash
curl http://localhost:8080/api/v1/emails/health
```

### Actuator Endpoints

```bash
# Application health
curl http://localhost:8080/actuator/health

# Application info
curl http://localhost:8080/actuator/info
```

## Testing

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=EmailControllerTest
```

### Test Coverage

```bash
mvn clean test jacoco:report
```

View coverage report: `target/site/jacoco/index.html`

### Manual Testing with Swagger

1. Open [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
2. Navigate to **"Email Operations"** section
3. Test any endpoint:
   - **GET /api/v1/emails/count** - Count emails from sender
   - **POST /api/v1/emails/credentials** - Upload credentials file
   - **DELETE /api/v1/emails/credentials** - Clear credentials
4. Click **"Try it out"**
5. Enter required parameters or upload file
6. Click **"Execute"**
7. View the response

## Troubleshooting

### Issue: "Credentials file not found"

**Solution Option 1 - File-based (Traditional):**
- Ensure `credentials.json` is in `src/main/resources/credentials.json`
- Verify the file was downloaded from Google Cloud Console
- Check file permissions

**Solution Option 2 - API Upload (Recommended):**
Upload credentials via API instead:
```bash
curl -X POST http://localhost:8080/api/v1/emails/credentials \
  -F "file=@/path/to/credentials.json"
```
This stores credentials in memory (cleared on restart)

### Issue: OAuth Browser Window Doesn't Open

**Solution:**
- Check if port 8888 is available (used for OAuth callback)
- Manually navigate to the URL shown in the console
- Try running: `lsof -i :8888` to check port availability

### Issue: "Invalid grant" Error

**Solution:**
- Delete the `tokens/` directory
- Restart the application to re-authorize
- Ensure your system clock is synchronized

### Issue: Gmail API Rate Limit Exceeded

**Solution:**
- Wait for the rate limit window to reset (usually 1 minute)
- The API has a quota of 1 billion queries per day
- Per-user limit is 250 queries per second
- Consider increasing cache TTL in `application.yml`

### Issue: Application Won't Start

**Solution:**
1. Check Java version: `java -version` (should be 17+)
2. Verify port 8080 is available: `lsof -i :8080`
3. Check logs: `tail -f logs/spring.log`
4. Rebuild: `mvn clean install`

### Issue: Cache Not Working

**Solution:**
- Verify `@EnableCaching` is present in `EmailReaderApplication.java`
- Check cache configuration in `application.yml`
- View cache statistics in logs (DEBUG level)

### Issue: Tests Failing

**Solution:**
- Ensure you're not running integration tests without credentials
- Unit tests should pass without Gmail API setup
- Run with: `mvn test -DskipIntegrationTests`

### Enable Debug Logging

Add to `application.yml`:

```yaml
logging:
  level:
    com.krysta.emailreader: DEBUG
    com.google.api: DEBUG
    org.springframework.cache: DEBUG
```

## Architecture

### Technology Stack

- **Framework:** Spring Boot 3.2.1
- **Language:** Java 17
- **Build Tool:** Maven
- **Cache:** Caffeine
- **API Documentation:** SpringDoc OpenAPI 3.0
- **Gmail Integration:** Google API Client Libraries
- **Testing:** JUnit 5, Mockito, MockMvc

### Project Structure

```
src/
├── main/
│   ├── java/com/krysta/emailreader/
│   │   ├── EmailReaderApplication.java       # Main application class
│   │   ├── controller/
│   │   │   └── EmailController.java          # REST endpoints
│   │   ├── service/
│   │   │   ├── EmailService.java             # Business logic with caching
│   │   │   └── GmailService.java             # Gmail API integration
│   │   ├── dto/
│   │   │   ├── EmailCountResponse.java       # Response DTO
│   │   │   └── ErrorResponse.java            # Error response DTO
│   │   ├── config/
│   │   │   ├── SwaggerConfig.java            # API documentation config
│   │   │   ├── GmailConfig.java              # Gmail API config
│   │   │   └── CacheConfig.java              # Caching configuration
│   │   └── exception/
│   │       ├── GmailApiException.java        # Gmail API errors
│   │       ├── InvalidEmailException.java    # Validation errors
│   │       └── GlobalExceptionHandler.java   # Centralized error handling
│   └── resources/
│       ├── application.yml                    # Main configuration
│       ├── application-dev.yml                # Development profile
│       └── credentials.json                   # Gmail OAuth credentials (not in git)
└── test/
    └── java/com/krysta/emailreader/
        ├── controller/
        │   └── EmailControllerTest.java       # Controller tests
        └── service/
            ├── EmailServiceTest.java          # Service tests
            └── GmailServiceTest.java          # Gmail service tests
```

### Key Design Patterns

- **Dependency Injection:** Spring's IoC container
- **DTO Pattern:** Separate data transfer objects
- **Service Layer Pattern:** Business logic isolation
- **Repository Pattern:** Gmail API abstraction
- **Exception Handling:** Global exception handler with `@ControllerAdvice`
- **Caching:** Spring Cache abstraction with Caffeine

### Caching Strategy

- **Cache Name:** `emailCounts`
- **Key:** Sender email address
- **TTL:** 5 minutes (configurable)
- **Max Size:** 500 entries (configurable)
- **Eviction:** Time-based (expireAfterWrite)

### Security Considerations

- OAuth 2.0 tokens stored securely in `tokens/` directory
- Credentials file excluded from version control
- Read-only Gmail scope (minimal permissions)
- Input validation for all user inputs
- No sensitive data in logs

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature-name`
3. Commit your changes: `git commit -am 'Add feature'`
4. Push to the branch: `git push origin feature-name`
5. Submit a pull request

## License

This project is licensed under the MIT License. See the LICENSE file for details.

---

## Support

For issues, questions, or contributions:

- **Issues:** [GitHub Issues](https://github.com/your-repo/issues)
- **Email:** support@krysta.com
- **Documentation:** [Swagger UI](http://localhost:8080/swagger-ui.html)

---

**Built with ❤️ by Krysta Software**
