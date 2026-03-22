# BankService Test Suite

This directory contains comprehensive, production-ready test coverage for the BankService microservice.

## Test Structure

### 1. Unit Tests (`BankServiceImplTest.java`)
- **Framework**: JUnit 5 + Mockito
- **Coverage**: Service layer business logic
- **Mocking**: All external dependencies (Repository, Kafka, Redis, Feign clients)
- **Tests Include**:
  - Happy path scenarios
  - Error handling and edge cases
  - OTP generation and verification
  - JWT token management
  - KYC data fetching
  - Administrative bypass logic

### 2. Controller Tests (`BankControllerTest.java`)
- **Framework**: Spring Boot WebMvcTest
- **Coverage**: REST API endpoints
- **Mocking**: Service layer
- **Tests Include**:
  - All HTTP methods (GET, POST, PUT)
  - Request validation
  - Response status codes
  - JSON serialization/deserialization
  - Error responses

### 3. Integration Tests (`BankServiceIntegrationTest.java`)
- **Framework**: Spring Boot Test + Testcontainers
- **Coverage**: Full request flow (Controller → Service → Repository → Database)
- **Infrastructure**: MongoDB Testcontainer
- **Tests Include**:
  - End-to-end KYC workflow
  - Database persistence
  - OTP workflow simulation
  - Administrative operations
  - Data consistency validation

### 4. End-to-End Tests (`BankServiceEndToEndTest.java`)
- **Framework**: Spring Boot Test + Testcontainers
- **Coverage**: Complete user journeys
- **Infrastructure**: MongoDB Testcontainer
- **Tests Include**:
  - Full KYC lifecycle (Initiate → Submit → Verify → Fetch)
  - Error handling scenarios
  - Concurrent user operations
  - Administrative workflows

### 5. Kafka Integration Tests (`BankServiceKafkaIntegrationTest.java`)
- **Framework**: Spring Boot Test + Embedded Kafka + Testcontainers
- **Coverage**: Asynchronous messaging
- **Infrastructure**: Kafka Testcontainer
- **Tests Include**:
  - Event publishing
  - Message serialization
  - Async processing verification

## Test Categories

### Unit Tests (Fast, Isolated)
- `BankServiceImplTest` - Service business logic
- `KycOrchestrationServiceTest` - Orchestration logic

### Integration Tests (Full Stack)
- `BankServiceIntegrationTest` - API to Database
- `BankServiceEndToEndTest` - Complete workflows

### Async Tests (Event-Driven)
- `BankServiceKafkaIntegrationTest` - Message processing

## Prerequisites

1. **Java 21** or higher
2. **Maven 3.6+**
3. **Docker** (for Testcontainers)
4. **All microservices running** (for full integration tests)

## Dependencies Added

```xml
<!-- Test Dependencies -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mongodb</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <version>5.7.0</version>
    <scope>test</scope>
</dependency>
```

## Running Tests

### Run All Tests
```bash
mvn test
```

### Run Specific Test Classes
```bash
# Unit tests only
mvn test -Dtest="*Test"

# Integration tests only
mvn test -Dtest="*IntegrationTest"

# End-to-end tests only
mvn test -Dtest="*EndToEndTest"

# Kafka tests only
mvn test -Dtest="*Kafka*Test"
```

### Run with Coverage
```bash
mvn test jacoco:report
```

### Run Tests in IDE
- All test classes are JUnit 5 compatible
- Can be run directly from IntelliJ IDEA or Eclipse
- Testcontainers tests require Docker to be running

## Test Data

### Sample Test Data Used
- **Users**: John Doe, Jane Smith with various risk levels
- **Documents**: Passport, utility bills, bank statements
- **Bank IDs**: UUID-generated for uniqueness
- **OTP Codes**: 6-digit codes (123456 for testing)
- **JWT Tokens**: Mock tokens for authentication testing

## Coverage Goals

- **Unit Tests**: >90% method coverage
- **Integration Tests**: Full workflow coverage
- **Controller Tests**: All endpoints and error scenarios
- **Async Tests**: Event publishing and consumption

## Best Practices Implemented

1. **AAA Pattern**: Arrange, Act, Assert
2. **Descriptive Test Names**: Clear intent and expectations
3. **Independent Tests**: No test dependencies
4. **Fast Execution**: Unit tests run in milliseconds
5. **Realistic Data**: Production-like test data
6. **Error Scenarios**: Comprehensive exception handling
7. **Container Management**: Proper Testcontainers lifecycle

## CI/CD Integration

These tests are designed for:
- **GitHub Actions**
- **Jenkins**
- **GitLab CI**
- **Azure DevOps**

## Troubleshooting

### Common Issues

1. **Docker not running**: Start Docker Desktop for Testcontainers
2. **Port conflicts**: Tests use dynamic ports
3. **Memory issues**: Increase JVM memory for large test suites
4. **Kafka timeouts**: Increase timeout values in test configuration

### Test Isolation

- Each test method is independent
- Database is cleaned between tests
- No shared state between test methods
- Mocks are reset automatically

## Maintenance

- **Update test data** when business rules change
- **Add new tests** for new features
- **Review coverage** regularly
- **Keep tests fast** and reliable

## Performance

- **Unit Tests**: < 1 second total
- **Integration Tests**: < 30 seconds total
- **End-to-End Tests**: < 60 seconds total
- **Parallel Execution**: Tests can run in parallel

This test suite provides comprehensive coverage for the BankService microservice, ensuring reliability, maintainability, and confidence in deployments.