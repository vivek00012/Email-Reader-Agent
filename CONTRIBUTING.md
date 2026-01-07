# Contributing to Email Reader Agent

Thank you for considering contributing to Email Reader Agent! This document provides guidelines and instructions for contributing.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Pull Request Process](#pull-request-process)

## Code of Conduct

This project adheres to a code of conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to support@krysta.com.

### Our Standards

- Be respectful and inclusive
- Welcome newcomers and help them learn
- Focus on what is best for the community
- Show empathy towards other community members

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Git
- Your favorite IDE (IntelliJ IDEA, Eclipse, or VS Code recommended)

### Setup Development Environment

1. **Fork the repository** on GitHub

2. **Clone your fork:**
   ```bash
   git clone https://github.com/YOUR_USERNAME/Email-Reader-Agent.git
   cd Email-Reader-Agent
   ```

3. **Add upstream remote:**
   ```bash
   git remote add upstream https://github.com/ORIGINAL_OWNER/Email-Reader-Agent.git
   ```

4. **Build the project:**
   ```bash
   mvn clean install
   ```

5. **Setup Gmail API credentials** (see SETUP.md)

6. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

## Development Workflow

### 1. Create a Feature Branch

```bash
git checkout -b feature/your-feature-name
```

Branch naming conventions:
- `feature/` - New features
- `bugfix/` - Bug fixes
- `docs/` - Documentation updates
- `refactor/` - Code refactoring
- `test/` - Test additions/improvements

### 2. Make Your Changes

- Write clean, readable code
- Follow the existing code style
- Add comments for complex logic
- Update documentation as needed

### 3. Test Your Changes

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=EmailServiceTest

# Run with coverage
mvn clean test jacoco:report
```

### 4. Commit Your Changes

Use meaningful commit messages:

```bash
git add .
git commit -m "feat: add email filtering by date range"
```

Commit message format:
- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation changes
- `style:` - Code style changes (formatting, etc.)
- `refactor:` - Code refactoring
- `test:` - Test additions/changes
- `chore:` - Build process or auxiliary tool changes

### 5. Keep Your Branch Updated

```bash
git fetch upstream
git rebase upstream/main
```

### 6. Push to Your Fork

```bash
git push origin feature/your-feature-name
```

## Coding Standards

### Java Code Style

- **Indentation:** 4 spaces (no tabs)
- **Line length:** Max 120 characters
- **Naming:**
  - Classes: `PascalCase`
  - Methods: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Variables: `camelCase`

### Best Practices

1. **Single Responsibility Principle:** Each class/method should have one clear purpose
2. **DRY (Don't Repeat Yourself):** Avoid code duplication
3. **KISS (Keep It Simple, Stupid):** Prefer simple solutions
4. **Dependency Injection:** Use Spring's DI instead of manual instantiation
5. **Logging:** Use SLF4J with appropriate log levels
6. **Exception Handling:** Use custom exceptions where appropriate

### Code Example

```java
/**
 * Service for managing email operations.
 * Provides business logic for email counting with caching support.
 */
@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final GmailService gmailService;
    
    public EmailService(GmailService gmailService) {
        this.gmailService = gmailService;
    }
    
    /**
     * Gets the count of emails from a specific sender.
     * 
     * @param senderEmail The email address of the sender
     * @return The count of emails
     * @throws InvalidEmailException if email format is invalid
     */
    @Cacheable(value = "emailCounts", key = "#senderEmail")
    public long getEmailCount(String senderEmail) {
        logger.debug("Getting email count for: {}", senderEmail);
        // Implementation
    }
}
```

## Testing Guidelines

### Test Structure

```java
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
    
    @Mock
    private GmailService gmailService;
    
    @InjectMocks
    private EmailService emailService;
    
    @Test
    void testGetEmailCount_ValidEmail_ReturnsCount() {
        // Arrange
        String senderEmail = "test@example.com";
        long expectedCount = 10L;
        when(gmailService.countEmailsFromSender(senderEmail))
            .thenReturn(expectedCount);
        
        // Act
        long actualCount = emailService.getEmailCount(senderEmail);
        
        // Assert
        assertEquals(expectedCount, actualCount);
        verify(gmailService, times(1))
            .countEmailsFromSender(senderEmail);
    }
}
```

### Test Requirements

- **Unit Tests:** Required for all new business logic
- **Integration Tests:** Required for API endpoints
- **Test Coverage:** Aim for 80% or higher
- **Test Naming:** `test<Method>_<Scenario>_<ExpectedResult>`

### Running Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=EmailServiceTest

# With coverage report
mvn clean test jacoco:report

# Skip tests (for quick builds only)
mvn install -DskipTests
```

## Pull Request Process

### Before Submitting

- [ ] Code compiles without errors
- [ ] All tests pass
- [ ] New tests added for new functionality
- [ ] Code follows style guidelines
- [ ] Documentation updated
- [ ] No linter warnings
- [ ] Commit messages are clear

### Submitting a Pull Request

1. **Push your branch** to your fork

2. **Create Pull Request** on GitHub:
   - Click "New Pull Request"
   - Select your feature branch
   - Fill in the PR template

3. **PR Title Format:**
   ```
   [Type] Brief description
   ```
   Examples:
   - `[Feature] Add date range filtering for emails`
   - `[Bugfix] Fix cache expiration issue`
   - `[Docs] Update Gmail API setup instructions`

4. **PR Description Template:**
   ```markdown
   ## Description
   Brief description of changes
   
   ## Type of Change
   - [ ] Bug fix
   - [ ] New feature
   - [ ] Documentation update
   - [ ] Refactoring
   
   ## Testing
   - [ ] Unit tests added/updated
   - [ ] Integration tests added/updated
   - [ ] Manual testing performed
   
   ## Checklist
   - [ ] Code follows style guidelines
   - [ ] Self-review completed
   - [ ] Comments added for complex code
   - [ ] Documentation updated
   - [ ] No new warnings
   - [ ] Tests pass locally
   ```

### Review Process

1. **Automated Checks:** CI/CD will run tests automatically
2. **Code Review:** Maintainers will review your code
3. **Feedback:** Address any requested changes
4. **Approval:** Once approved, your PR will be merged

### After Merging

1. **Delete your branch:**
   ```bash
   git branch -d feature/your-feature-name
   git push origin --delete feature/your-feature-name
   ```

2. **Update your local main:**
   ```bash
   git checkout main
   git pull upstream main
   ```

## Areas to Contribute

### Good First Issues

- Documentation improvements
- Test coverage improvements
- Code comments and examples
- Bug fixes

### Feature Ideas

- Email filtering by date range
- Bulk email counting
- Email statistics dashboard
- Performance optimizations
- Additional authentication methods
- Rate limiting improvements

### Documentation Needs

- More usage examples
- Video tutorials
- API integration guides
- Troubleshooting guides

## Questions?

- **GitHub Issues:** For bugs and feature requests
- **Email:** support@krysta.com
- **Documentation:** See README.md and SETUP.md

## Recognition

Contributors will be recognized in:
- README.md Contributors section
- Release notes
- Project website (if applicable)

Thank you for contributing! 🎉
