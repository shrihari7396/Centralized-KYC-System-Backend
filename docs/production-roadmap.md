# Production Hardening Roadmap & Change Plan

**Project**: Centralized Distributed KYC & Consent Platform  
**Target Completion**: Production-Grade Distributed Cloud-Native Architecture  
**Execution Strategy**: Incremental, non-destructive hardening with automated verification  

---

## Phase 1: Secret Remediation & Environment Externalization

### Work Item 1.1: Purge Hard-Coded Credentials from Kubernetes Manifests & Configs
- **File Location**: `deploy/k8s/02-secrets.yaml`, `Backend/BankService/src/main/resources/application.properties`, `Backend/ConsentService/src/main/resources/application.yml`, `Backend/KycManagementService/src/main/resources/application.properties`, `Backend/NotificationSystem/src/main/resources/application.yaml`, `Backend/UserService/src/main/resources/application.yml`, `Backend/docker-compose.yml`
- **Severity**: **CRITICAL**
- **Security Impact**: Exposure of production database credentials, symmetric AES keys, and JWT secrets allows unauthorized full-system compromise and data exfiltration.
- **Implementation Steps**:
  1. Replace all hard-coded credentials with secure environment variable references with safe local fallbacks (e.g. `${MYSQL_PASSWORD:${DB_PASSWORD:}}`).
  2. Transform `deploy/k8s/02-secrets.yaml` into `02-secrets.example.yaml` containing placeholder instructions for External Secrets Operator / Vault.
  3. Create `.env.example` in `Backend/` defining required variables (`MYSQL_ROOT_PASSWORD`, `JWT_SECRET`, `ENCRYPTION_SECRET_KEY`, `GROQ_API_KEY`, etc.).
  4. Ensure `.gitignore` ignores all local `.env`, `*.secret.yaml`, and credentials.
- **Acceptance Criteria**: Zero plaintext passwords or cryptographic keys in source code or manifests. Services fail startup gracefully with informative error messages when required production secrets are missing.
- **Tests Required**: Unit tests verifying environment variable binding; startup failure tests when mandatory secrets are absent.
- **Breaking-Change Risk**: Low. Local development continues using `.env.example` templates.
- **Rollback Approach**: Revert configuration files using git commit rollback.

---

## Phase 2: Configuration & Profile Standardization

### Work Item 2.1: Establish Multi-Profile Hierarchy & Root Aggregation
- **File Location**: Root directory (`pom.xml`), all services (`pom.xml`, `src/main/resources/application*.yml`)
- **Severity**: **HIGH**
- **Security Impact**: Inconsistent dependencies, obsolete/experimental Spring Boot versions (e.g. 4.0.4, 3.5.10), and loose development settings active in production lead to unpatched CVEs and configuration drift.
- **Implementation Steps**:
  1. Create a root multi-module Maven parent `pom.xml` aggregating all 11 services with standardized Spring Boot LTS (3.4.x) and Spring Cloud dependencies.
  2. Standardize all microservices to use unified `application.yml` and profiles: `local`, `test`, `staging`, `production`.
  3. Remove embedded `127.0.0.1` and `localhost` addresses from production configuration sections.
  4. Implement a startup validator bean (`ProductionConfigValidator`) in each service that fails early if critical production infrastructure hosts are unconfigured.
- **Acceptance Criteria**: All services build cleanly under a single parent `pom.xml` via `mvn clean compile`. Profiles dynamically toggle configuration without modifying source code.
- **Tests Required**: Spring context load tests with `test` profile.
- **Breaking-Change Risk**: Medium. Requires updating build commands across scripts.
- **Rollback Approach**: Revert root `pom.xml` and keep service-level POMs intact.

---

## Phase 3: Security, Authentication & Authorization Hardening

### Work Item 3.1: Fix Gateway Authorization Bypass & Routing Prefix
- **File Location**: `Backend/ApiGateway/src/main/java/com/rbi/apigateway/filters/JwtFilter.java`, `Backend/ApiGateway/src/main/resources/application.yml`
- **Severity**: **CRITICAL**
- **Security Impact**: Allows complete unauthenticated access to protected microservice endpoints when the `Authorization` header is omitted or when route prefixes mismatch (`/user/**` vs `/users/`).
- **Implementation Steps**:
  1. Refactor `JwtFilter.java` to enforce an explicit whitelist of public endpoints (`/user/register`, `/user/otp/**`, `/bank/kyc/otp/**`, `/bank/kyc/banks`, `/bank/kyc/register`).
  2. Reject any request to non-whitelisted paths without a valid `Bearer` token with HTTP 401 Unauthorized.
  3. Correct path prefix check from `/users/` to `/user/`.
  4. Parse JWT claims safely, validate `exp`, `iss`, `aud`, and verify token revocation against Redis `blacklist:{jti}`.
  5. Inject trusted downstream headers: `X-User-Id`, `X-User-Role`, `X-Bank-Id`, and `X-Correlation-Id`.
- **Acceptance Criteria**: Unauthenticated requests to `/api/kyc/**`, `/bank/kyc/customers/**`, `/consents/**`, and `/user/details` receive HTTP 401. Valid JWTs pass with downstream context headers.
- **Tests Required**: MockServer / WebTestClient tests verifying 401 on missing auth, 401 on expired auth, 403 on role mismatch, and 200 on authorized request.
- **Breaking-Change Risk**: High for unauthenticated clients; expected and mandatory for production security.
- **Rollback Approach**: Git revert on `JwtFilter.java`.

### Work Item 3.2: Implement Service-Level Spring Security & Signature Verification
- **File Location**: `Backend/KycManagementService`, `Backend/UserService`, `Backend/BankService`, `Backend/ConsentService`
- **Severity**: **CRITICAL**
- **Security Impact**: Bypassing the gateway permits direct, unauthenticated access to internal services. Base64-only decoding in `KycManagementService` permits forged claims.
- **Implementation Steps**:
  1. Add `spring-boot-starter-security` to `KycManagementService` and `UserService` POMs.
  2. Replace `JwtParserUtil` in `KycManagementService` with cryptographic JJWT signature verification.
  3. Implement internal `JwtAuthenticationFilter` in each service to validate JWT signature, extract `UserPrincipal`, and populate Spring `SecurityContextHolder`.
  4. Secure `KycUserDashboardController` (`/api/kyc/{userId}/**`) with ownership checks (`hasRole('ADMIN') or principal == #userId`).
  5. In `UserServiceImpl.java`, populate JWT roles strictly from database `UserEntity.getRoles()` rather than client DTO.
- **Acceptance Criteria**: Direct requests to microservice ports without valid JWT tokens are rejected with 401/403. Signature forgery attacks fail.
- **Tests Required**: Spring Security `@SpringBootTest` / `@WebMvcTest` with `@WithMockUser` testing RBAC and IDOR protection.
- **Breaking-Change Risk**: Low. Legitimate authenticated flows remain functional.
- **Rollback Approach**: Git revert security configuration additions.

### Work Item 3.3: OTP Brute-Force Protection, Masking & Rate Limiting
- **File Location**: `UserServiceImpl.java`, `ConsentServiceImpl.java`, `DigilockerServiceImpl.java`, `BankServiceImpl.java`
- **Severity**: **CRITICAL**
- **Security Impact**: Plaintext OTP logging permits credential theft from logs; lack of attempt tracking permits brute-force guessing of 6-digit OTPs.
- **Implementation Steps**:
  1. Remove all `System.out.println(finalotp)` and `log.info("OTP: {}", ...)` statements.
  2. Implement an OTP attempt counter in Redis (`otp_attempts:{key}`).
  3. Invalidate OTP and enforce a 15-minute temporary lockout after 3 consecutive failed verification attempts.
  4. Reduce OTP validity window from 10 minutes to 3 minutes.
- **Acceptance Criteria**: OTP codes never appear in logs or stdout. 4th incorrect OTP attempt returns 429 Too Many Requests or 401 Account Locked.
- **Tests Required**: Unit tests verifying OTP attempt increment, lockout on 3rd failure, and log verification tests.
- **Breaking-Change Risk**: None.
- **Rollback Approach**: Revert OTP service methods.

---

## Phase 4: KYC & Data Protection (Encryption & Consent Enforcement)

### Work Item 4.1: Reactivate & Standardize Field-Level Database Encryption
- **File Location**: `Backend/UserService/src/main/java/org/rbi/userservice/Model/UserEntity.java`, `Backend/KycManagementService/src/main/java/org/rbi/kycmanagementservice/entity/KycRecord.java`, `Backend/BankService/src/main/java/org/rbi/bankservice/entity/BankCustomerKycData.java`
- **Severity**: **HIGH**
- **Security Impact**: Cleartext storage of Aadhaar, PAN, phone numbers, and full names violates data privacy regulations (e.g. GDPR, India DPDP Act).
- **Implementation Steps**:
  1. Re-enable `@Convert(converter = StringCryptoConverter.class)` on `UserEntity` for `firstName`, `lastName`, and `phone`.
  2. Fix MySQL phone column definition to `VARCHAR(255)` so unique constraint succeeds with encryption.
  3. Implement field-level encryption for `aadharNumber`, `panNumber`, and `address` in `KycRecord` and `BankCustomerKycData`.
  4. Ensure distinct initialization vectors (IVs) are generated via `SecureRandom` for each AES-256-GCM encryption operation.
- **Acceptance Criteria**: Sensitive database columns contain Base64 ciphertext in MySQL and MongoDB. Services decrypt values transparently on read.
- **Tests Required**: JPA repository integration tests verifying ciphertext in database and transparent decrypted entity retrieval.
- **Breaking-Change Risk**: High for existing databases with plaintext data. Managed through Flyway migration scripts.
- **Rollback Approach**: Rollback JPA entity annotations and database migration.

### Work Item 4.2: Consent Verification & Tenant-Isolated Data Access
- **File Location**: `Backend/BankService`, `Backend/ConsentService`, `Backend/KycManagementService`
- **Severity**: **HIGH**
- **Security Impact**: Cross-tenant data leakage where one bank can access other banks' customer consents.
- **Implementation Steps**:
  1. Enforce strict `bankId` scoping across all repository queries in `BankCustomerController` and `BankConsentController`.
  2. Validate that consent has `status == APPROVED` and `expiryAt > now()` prior to releasing customer data.
  3. Unify `KycKafkaConsumer` and `ConsentResponseConsumer` to update both `KycRecord` and `KycEntity` consistently.
- **Acceptance Criteria**: Bank agents can only access consents and KYC payloads assigned to their authenticated `bankId`. Expired consents reject data release.
- **Tests Required**: Multi-tenant authorization tests with mock bank users.
- **Breaking-Change Risk**: Low.
- **Rollback Approach**: Revert controller/service query filters.

---

## Phase 5: Database Migrations with Flyway

### Work Item 5.1: Replace Hibernate DDL-Auto with Flyway Migrations
- **File Location**: `UserService`, `KycManagementService`, `ConsentService`, `NotificationSystem`, `LogMonitoringSystem` (`pom.xml`, `src/main/resources/db/migration/`)
- **Severity**: **HIGH**
- **Security Impact**: Hibernate `ddl-auto: update` causes uncontrolled schema drift, race conditions in clustered deployments, and data corruption.
- **Implementation Steps**:
  1. Add `org.flywaydb:flyway-core` and `flyway-mysql` dependencies to relational services.
  2. Create baseline migration `V1__initial_schema.sql` for each service defining tables, indexes, constraints, and foreign keys.
  3. Set `spring.jpa.hibernate.ddl-auto: validate` across all profiles.
- **Acceptance Criteria**: Clean database bootstraps reliably via Flyway on startup. No schema alterations executed by Hibernate.
- **Tests Required**: Clean database migration test via Testcontainers.
- **Breaking-Change Risk**: Medium. Requires database initialization on new environments.
- **Rollback Approach**: Revert `ddl-auto` to `none` and remove migration runner.

---

## Phase 6: Kafka & Event Stream Reliability

### Work Item 6.1: Align Topics, Add Dead-Letter Queues (DLT) & Consumer Idempotency
- **File Location**: `EmailService`, `NotificationSystem`, `ConsentService`, `BankService`, `KycManagementService`
- **Severity**: **HIGH**
- **Security Impact**: Unhandled consumer exceptions drop messages silently; poison messages crash listeners; mismatched topic names prevent notifications.
- **Implementation Steps**:
  1. Align topic constants across services (`notifications`, `notification-events-topic` unified to `kyc.notification.events`).
  2. Configure unified type mappings in Spring Kafka deserializers or standardize on JSON payload envelopes with header metadata.
  3. Add `@RetryableTopic(attempts = "3", backoff = @Backoff(delay = 1000, multiplier = 2.0))` with Dead Letter Topics (DLT) across all consumers.
  4. Implement consumer idempotency by storing processed event IDs in Redis with 24-hour TTL.
- **Acceptance Criteria**: Zero lost events on deserialization errors. Poison messages route to `.DLT`. Duplicate Kafka deliveries do not create duplicate database records.
- **Tests Required**: EmbeddedKafka / Testcontainers Kafka consumer tests simulating network failure and poison payloads.
- **Breaking-Change Risk**: Low.
- **Rollback Approach**: Revert listener annotations.

---

## Phase 7: API Reliability & Unified Error Contract

### Work Item 7.1: Implement RFC 7807 Global Error Handling & Request Validation
- **File Location**: All microservices (`GlobalExceptionHandler.java`, `dto/*`)
- **Severity**: **MEDIUM**
- **Security Impact**: Unhandled exceptions leak internal stack traces, SQL syntax, and server implementation details to external clients.
- **Implementation Steps**:
  1. Implement a unified `GlobalExceptionHandler` adhering to `docs/api-error-contract.md` across all 11 services.
  2. Add `@Valid` and Jakarta validation annotations (`@NotBlank`, `@Email`, `@Size`, `@Pattern`) to all request DTOs.
  3. Replace raw text response entities with structured `ApiErrorResponse` objects.
  4. Configure Feign client timeouts (Connect 3s, Read 5s) and Resilience4j circuit breakers for inter-service calls.
- **Acceptance Criteria**: All 4xx and 5xx responses return standardized JSON error bodies. No stack traces exposed in responses.
- **Tests Required**: Controller unit tests asserting error response schema for validation failures and custom exceptions.
- **Breaking-Change Risk**: Low. Clients receive structured JSON instead of arbitrary strings.
- **Rollback Approach**: Revert exception handler classes.

---

## Phase 8: Testing Infrastructure & Test Suite Restoration

### Work Item 8.1: Rebuild Unit, Integration & Security Test Suites
- **File Location**: `Backend/*/src/test/`
- **Severity**: **HIGH**
- **Security Impact**: Complete lack of tests means vulnerabilities and regressions cannot be detected automatically during CI.
- **Implementation Steps**:
  1. Create `src/test/java` directories for all microservices.
  2. Implement unit tests for Controllers, Services, Security filters, and DTO validations using JUnit 5 and Mockito.
  3. Implement integration tests using Testcontainers (MySQL, MongoDB, Redis, Kafka) testing end-to-end user registration, OTP verification, consent approval, and KYC data access.
  4. Implement security tests verifying unauthorized access rejection, role boundaries, and input sanitization.
  5. Add JaCoCo plugin configured with an 80% coverage threshold for business logic.
- **Acceptance Criteria**: `mvn test` executes and passes across all 11 microservices. JaCoCo coverage reports generated.
- **Tests Required**: Self-validating test suite.
- **Breaking-Change Risk**: None.
- **Rollback Approach**: Delete or disable failing tests.

---

## Phase 9: Observability, Metrics & Structured Logging

### Work Item 9.1: Spring Boot Actuator, Prometheus & Masked JSON Logging
- **File Location**: All microservices (`pom.xml`, `src/main/resources/logback-spring.xml`)
- **Severity**: **MEDIUM**
- **Security Impact**: Inability to monitor container health, detect intrusion attempts, or audit security events.
- **Implementation Steps**:
  1. Add `spring-boot-starter-actuator` and `micrometer-registry-prometheus` to all services.
  2. Expose `/actuator/health`, `/actuator/info`, and `/actuator/prometheus`.
  3. Configure `logback-spring.xml` with Logstash JSON encoder for structured logging.
  4. Implement log pattern masking for PII (Aadhaar, PAN, email, phone, OTP, tokens).
  5. Propagate `X-Correlation-Id` across incoming HTTP requests and Kafka headers.
- **Acceptance Criteria**: Metrics scraped by Prometheus. Container logs output single-line JSON with masked PII and trace IDs.
- **Tests Required**: Actuator endpoint integration tests and log output verification tests.
- **Breaking-Change Risk**: None.
- **Rollback Approach**: Revert Logback configuration.

---

## Phase 10: Docker Container Hardening

### Work Item 10.1: Multi-Stage, Non-Root, Minimal Distroless/Alpine Dockerfiles
- **File Location**: `Backend/*/Dockerfile`
- **Severity**: **HIGH**
- **Security Impact**: Running containers as `root` with pre-built JAR dependencies increases container escape risks and vulnerabilities.
- **Implementation Steps**:
  1. Rewrite Dockerfiles to use multi-stage builds (`eclipse-temurin:21-jdk-alpine` as builder, `eclipse-temurin:21-jre-alpine` as runtime).
  2. Pin base image versions with SHA256 digests.
  3. Create and enforce a non-privileged system user (`appuser:appgroup` with UID 10001).
  4. Add container JVM resource flags (`-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`).
  5. Add Docker `HEALTHCHECK` instructions.
- **Acceptance Criteria**: Containers build self-contained from source without requiring host pre-builds, run as UID 10001, and pass Trivy container scans with 0 HIGH/CRITICAL vulnerabilities.
- **Tests Required**: Docker build and container run tests.
- **Breaking-Change Risk**: Low.
- **Rollback Approach**: Revert Dockerfile files.

---

## Phase 11: Kubernetes Manifest Hardening

### Work Item 11.1: Production-Grade K8s Configurations
- **File Location**: `deploy/k8s/*.yaml`
- **Severity**: **HIGH**
- **Security Impact**: Unhardened pod specs, missing resource limits, lack of ingress TLS, and missing network policies risk cluster instability and lateral movement.
- **Implementation Steps**:
  1. Add `securityContext` (`runAsNonRoot: true`, `allowPrivilegeEscalation: false`, `readOnlyRootFilesystem: false`, `capabilities.drop: ["ALL"]`) to all deployment specs.
  2. Replace TCP socket probes with HTTP Actuator probes (`/actuator/health/liveness`, `/actuator/health/readiness`).
  3. Define `HorizontalPodAutoscaler` (HPA) and `PodDisruptionBudget` (PDB) for critical services (`api-gateway`, `user-service`, `kyc-management-service`).
  4. Create `NetworkPolicy` resources isolating database and messaging tiers.
  5. Add TLS Ingress resource with cert-manager annotations for `api-gateway`.
- **Acceptance Criteria**: `kubectl apply --dry-run=client -k deploy/k8s/` validates without schema warnings. Manifests comply with Pod Security Standards (Restricted/Baseline).
- **Tests Required**: `kubeval` or `kubectl apply --dry-run=client` validation.
- **Breaking-Change Risk**: Medium. Requires Kubernetes cluster support for Ingress and NetworkPolicies.
- **Rollback Approach**: Revert manifests.

---

## Phase 12: Modernized CI/CD Workflow

### Work Item 12.1: Modernize GitHub Actions Pipeline
- **File Location**: `.github/workflows/maven.yml`
- **Severity**: **HIGH**
- **Security Impact**: Broken CI allows vulnerabilities, build errors, and broken contracts to reach main branch unchecked.
- **Implementation Steps**:
  1. Update `.github/workflows/maven.yml` to build all modules using the new root POM.
  2. Add automated test execution with Testcontainers and JaCoCo coverage report publishing.
  3. Add OWASP Dependency Check step for CVE detection.
  4. Add CodeQL static analysis and Trivy container image scanning.
  5. Add Kubernetes manifest validation step via `kubeval` / `kubectl`.
- **Acceptance Criteria**: GitHub Actions pipeline runs all stages cleanly: compile -> test -> security scan -> container build -> manifest validation.
- **Tests Required**: GitHub Actions workflow dry-run / push trigger.
- **Breaking-Change Risk**: None.
- **Rollback Approach**: Revert workflow YAML.
