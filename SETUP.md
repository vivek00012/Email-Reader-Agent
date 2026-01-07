# Quick Setup Guide

This is a condensed setup guide to get you started quickly.

## 5-Minute Setup

### 1. Prerequisites Check

```bash
java -version   # Should show Java 17 or higher
mvn -version    # Should show Maven 3.6+
```

### 2. Gmail API Setup

1. **Go to:** https://console.cloud.google.com/
2. **Create new project** → Enter name → Create
3. **Enable Gmail API:**
   - APIs & Services → Library
   - Search "Gmail API" → Enable
4. **Create OAuth credentials:**
   - APIs & Services → Credentials
   - Configure consent screen (External, add test user)
   - Create Credentials → OAuth Client ID → Desktop app
   - Download JSON → Save as `src/main/resources/credentials.json`

### 3. Build & Run

```bash
# Clone and navigate to project
cd Email-Reader-Agent

# Build
mvn clean install

# Run
mvn spring-boot:run
```

### 4. First-Time Authorization

- Browser will open automatically
- Select your Google account
- Click "Allow"
- Close browser when done

### 5. Test the API

```bash
# Health check
curl http://localhost:8080/api/v1/emails/health

# Count emails
curl "http://localhost:8080/api/v1/emails/count?senderEmail=test@example.com"

# Open Swagger UI
open http://localhost:8080/swagger-ui.html
```

## Quick Reference

### Important URLs

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **API Docs:** http://localhost:8080/v3/api-docs
- **Health Check:** http://localhost:8080/actuator/health
- **Google Cloud Console:** https://console.cloud.google.com/

### Configuration Files

- **Main config:** `src/main/resources/application.yml`
- **Credentials:** `src/main/resources/credentials.json` (download from Google)
- **Tokens:** `tokens/` (auto-generated after first auth)

### Common Commands

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Run tests
mvn test

# Package JAR
mvn clean package

# Run JAR
java -jar target/email-reader-agent-1.0.0.jar
```

### Troubleshooting

| Issue | Solution |
|-------|----------|
| "Credentials not found" | Add `credentials.json` to `src/main/resources/` |
| "Port already in use" | Change port in `application.yml` or stop conflicting process |
| OAuth doesn't open | Check port 8888 availability, use URL from console |
| "Invalid grant" | Delete `tokens/` folder and re-authorize |

## Need More Help?

See the full [README.md](README.md) for detailed documentation.
