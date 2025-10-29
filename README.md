# Peach

A production-ready, secure authentication service built with Spring Boot, featuring JWT token-based authentication, password encryption, and comprehensive security measures.

## Features

- ✅ **JWT Token Authentication** - Stateless authentication using JSON Web Tokens
- ✅ **Secure Password Storage** - BCrypt hashing with configurable strength
- ✅ **Account Security** - Automatic lockout after failed login attempts
- ✅ **Role-Based Access Control (RBAC)** - Flexible permission management
- ✅ **Input Validation** - Comprehensive request validation
- ✅ **Audit Logging** - Track user creation, updates, and login activity
- ✅ **RESTful API** - Clean, well-documented endpoints
- ✅ **Exception Handling** - Proper error responses with appropriate HTTP status codes

## Tech Stack

- **Java 25**
- **Spring Boot 3.5.7**
- **Spring Security 6**
- **Spring Data JPA**
- **PostgreSQL 18**
- **Flyway**
- **Docker**
- **JWT (JJWT 0.12.5)**
- **Gradle (Kotlin)**

## Prerequisites

- JDK 17 or higher
- Gradle (Kotlin)
- PostgreSQL 16+
- Your favorite IDE (IntelliJ IDEA, Eclipse, VS Code)

## Quick Start

### 1. Clone the Repository

```bash
git clone <https://github.com/belikesnab/peach.git>
cd peach
```

### 2. Start Database

Make sure docker is running

```bash
docker-compose up -d
```

### 3. Set Environment Variables

For production, set your JWT secret and postgres credentials as an environment variable in .env:

```
POSTGRES_USERNAME=YourPostgresUsername
POSTGRES_PASSWORD=YourPostgresPassword
JWT_SECRET="YourVerySecureSecretKeyThatIsAtLeast256BitsLong"
```

### 4. Build and Run

```bash
./gradlew clean build
./gradlew bootRun
```

The application will start on `http://localhost:8080`

## API Documentation

### Base URL

```
http://localhost:8080/api/auth
```

### Endpoints

#### 1. Register New User

**POST** `/api/auth/register`

Creates a new user account.

**Request Body:**

```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Validation Rules:**
- `username`: 3-50 characters, required, unique
- `email`: Valid email format, required, unique
- `password`: 8-100 characters, required

**Success Response (200 OK):**

```json
{
  "message": "User registered successfully"
}
```

**Error Responses:**

```json
// 400 Bad Request - Validation Error
{
  "username": "Username must be between 3 and 50 characters",
  "email": "Email must be valid",
  "password": "Password must be between 8 and 100 characters"
}

// 400 Bad Request - Duplicate User
{
  "message": "Username is already taken"
}
```

#### 2. Login

**POST** `/api/auth/login`

Authenticates user and returns JWT token.

**Request Body:**

```json
{
  "username": "johndoe",
  "password": "SecurePass123!"
}
```

**Success Response (200 OK):**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "id": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "roles": ["USER"]
}
```

**Error Responses:**

```json
// 401 Unauthorized - Invalid Credentials
{
  "message": "Invalid username or password"
}

// 423 Locked - Account Locked
{
  "message": "Account is locked due to too many failed login attempts"
}
```

#### 3. Get Current User

**GET** `/api/auth/me`

Returns information about the currently authenticated user.

**Headers:**

```
Authorization: Bearer <your-jwt-token>
```

**Success Response (200 OK):**

```json
"Authenticated user: johndoe"
```

**Error Response:**

```json
// 401 Unauthorized
{
  "message": "Unauthorized"
}
```

## Authentication Flow

### Registration Flow

```
1. User submits registration form
2. System validates input (username length, email format, password strength)
3. System checks for duplicate username/email
4. Password is hashed using BCrypt (strength: 12)
5. User is created with default "USER" role
6. Success message returned
```

### Login Flow

```
1. User submits credentials
2. System checks if account exists and is not locked
3. Password is verified against BCrypt hash
4. On success:
   - Failed login attempts reset to 0
   - Last login timestamp updated
   - JWT token generated and returned
5. On failure:
   - Failed login attempts incremented
   - Account locked after 5 failed attempts
   - Error message returned
```

### Request Authentication Flow

```
1. Client includes JWT token in Authorization header
2. JwtAuthenticationFilter extracts and validates token
3. If valid, user details loaded and authentication set
4. Request proceeds to controller
5. @PreAuthorize annotations check permissions
```

## Security Features

### Password Security

- **BCrypt Hashing**: Passwords hashed with BCrypt (strength: 12)
- **Minimum Length**: 8 characters enforced
- **Never Stored Plain**: Original passwords never persisted

### Account Protection

- **Failed Login Tracking**: Counts failed authentication attempts
- **Automatic Lockout**: Account locked after 5 failed attempts
- **Manual Unlock**: Requires admin intervention to unlock

### Token Security

- **JWT-Based**: Stateless authentication tokens
- **Expiration**: Tokens expire after 24 hours (configurable)
- **Signature Verification**: HS256 algorithm with secret key
- **Bearer Scheme**: Industry-standard token format

### Input Validation

- **Bean Validation**: All inputs validated with Jakarta Validation
- **Sanitization**: Automatic protection against injection attacks
- **Unique Constraints**: Database-level uniqueness enforcement

## Database Schema

### Users Table

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    last_login TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### User Roles Table

```sql
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);
```

## Configuration

### Application Properties

Key configuration options in `application.yml`:

```yaml
spring:
  application:
    name: peach
  datasource:
    url: jdbc:postgresql://localhost:5432/auth_db
    username: postgres
    password: peach@admin123
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    url: jdbc:postgresql://localhost:5432/auth_db
    user: postgres
    password: peach@admin123
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true

app:
  jwt:
    secret: SecretKeyThatIsAtLeast256BitsLongForHS256Algorithm
    expiration-ms: 86400000

logging:
  level:
    com.belikesnab.peach: DEBUG
    org.springframework.security: DEBUG
```

### Environment Variables

| Variable            | Description | Required | Default |
|---------------------|-------------|----------|---------|
| `JWT_SECRET`        | Secret key for JWT signing | Yes (prod) | Dev fallback |
| `POSTGRES_URL`      | Database connection URL | No | localhost:5432 |
| `POSTGRES_USERNAME` | Database username | No | postgres |
| `POSTGRES_PASSWORD` | Database password | Yes | - |

## Usage Examples

### Using cURL

**Register a new user:**

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "SecurePass123!"
  }'
```

**Login:**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "SecurePass123!"
  }'
```

**Access protected endpoint:**

```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Using JavaScript (Fetch API)

```javascript
// Register
const register = async () => {
  const response = await fetch('http://localhost:8080/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username: 'johndoe',
      email: 'john@example.com',
      password: 'SecurePass123!'
    })
  });
  return await response.json();
};

// Login
const login = async () => {
  const response = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username: 'johndoe',
      password: 'SecurePass123!'
    })
  });
  const data = await response.json();
  localStorage.setItem('token', data.token);
  return data;
};

// Authenticated request
const getCurrentUser = async () => {
  const token = localStorage.getItem('token');
  const response = await fetch('http://localhost:8080/api/auth/me', {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  return await response.text();
};
```

## Role-Based Access Control

### Default Roles

- `USER`: Standard user role (assigned on registration)
- `ADMIN`: Administrative privileges

### Adding Custom Roles

Update the `User` entity roles during registration or via admin endpoint:

```java
user.setRoles(Set.of("USER", "PREMIUM"));
```

### Protecting Endpoints

Use `@PreAuthorize` annotation:

```java
@GetMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<List<User>> getAllUsers() {
    // Only accessible by ADMIN role
}

@GetMapping("/premium/content")
@PreAuthorize("hasAnyRole('ADMIN', 'PREMIUM')")
public ResponseEntity<String> getPremiumContent() {
    // Accessible by ADMIN or PREMIUM roles
}
```

## Error Handling

The service provides consistent error responses:

### Validation Errors (400)

```json
{
  "username": "Username must be between 3 and 50 characters",
  "password": "Password is required"
}
```

### Authentication Errors (401)

```json
{
  "message": "Invalid username or password"
}
```

### Account Locked (423)

```json
{
  "message": "Account is locked due to too many failed login attempts"
}
```

### Internal Errors (500)

```json
{
  "message": "An error occurred: <error details>"
}
```

## Testing

### Unit Tests

```bash
./gradlew test
```

### Integration Tests

```bash
./gradlew check
```

### Manual Testing with Postman

Import the provided Postman collection (if available) or create requests following the API documentation above.

## Deployment

### Production Checklist

- [ ] Change `ddl-auto` to `validate` or `none`
- [ ] Set strong JWT secret via environment variable
- [ ] Use HTTPS/TLS for all connections
- [ ] Configure database connection pooling
- [ ] Set up database backups
- [ ] Enable production logging (remove DEBUG)
- [ ] Configure CORS for your frontend domain
- [ ] Set up monitoring and alerting
- [ ] Use secrets management (AWS Secrets Manager, HashiCorp Vault)
- [ ] Enable rate limiting
- [ ] Configure firewall rules

## Troubleshooting

### Common Issues

**Issue**: `JwtException: JWT signature does not match`
- **Solution**: Ensure JWT_SECRET is consistent across restarts

**Issue**: `LockedException: Account is locked`
- **Solution**: Reset failed login attempts in database or implement unlock endpoint

**Issue**: `BadCredentialsException: Invalid username or password`
- **Solution**: Verify password meets minimum requirements and credentials are correct

**Issue**: `DataIntegrityViolationException: duplicate key value`
- **Solution**: Username or email already exists in database

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues, questions, or contributions, please open an issue on GitHub.
