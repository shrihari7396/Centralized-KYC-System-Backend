# Repository Production Hardening Audit

**Repository**: `https://github.com/shrihari7396/Centralized-KYC-System-Backend`  
**Audit Date**: November 18, 2026  
**Auditor**: Senior Java / Spring Production-Hardening Engineering Specialist  
**Status**: Critical Production Readiness Deficits Identified  

---

## Executive Summary

A comprehensive, multi-phase forensic audit was conducted on the entire codebase, including all 11 microservices (`AiService`, `ApiGateway`, `BankService`, `ConsentService`, `DigilockerService`, `DiscoveryService`, `EmailService`, `KycManagementService`, `LogMonitoringSystem`, `NotificationSystem`, `UserService`), build configurations (`pom.xml`), application manifests (`application.properties`, `application.yml`), container profiles (`Dockerfile`, `docker-compose.yml`), deployment manifests (`deploy/k8s/`), utility scripts (`scripts/`, `Backend/*.sh`), CI/CD workflows (`.github/workflows/`), security layers, entities, repositories, consumers, producers, and test infrastructure.

The platform is currently an unhardened, proof-of-concept/hackathon prototype with multiple critical vulnerabilities:
1. **Critical Authentication & Authorization Bypasses**: The API Gateway (`JwtFilter`) permits completely unauthenticated traffic to proceed to downstream microservices when the `Authorization` header is omitted. Downstream services (`KycManagementService`, `UserService`, `AiService`, `DigilockerService`) lack Spring Security altogether, leaving sensitive KYC data endpoints exposed to anyone. `JwtParserUtil` in `KycManagementService` parses JWT payloads without cryptographically verifying signatures.
2. **Hard-coded Secrets Across Code and Manifests**: Identical hard-coded AES/JWT secrets (`404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970`), plain-text database passwords (`899961`), and gateway HMAC keys are committed in source code, properties, and Kubernetes manifests (`02-secrets.yaml`).
3. **Severe Information Leakage & PII Logging**: Plaintext OTP codes are explicitly logged to SLF4J loggers and printed to `System.out` in `UserServiceImpl` and `ConsentServiceImpl`. Full emails, phone numbers, and unmasked Aadhaar/PAN UUIDs are dispatched to Kafka log topics.
4. **Complete Absence of Automated Tests**: All `src/test` directories were removed via a maintenance script (`Backend/remove-tests.sh`). There are currently **0** unit or integration tests in the entire repository. The CI/CD workflow (`maven.yml`) targets a non-existent root `pom.xml` and fails immediately.
5. **Architectural & Schema Inconsistencies**: Database encryption was commented out in `UserEntity`. `TEXT` columns in MySQL are configured with unique constraints without key lengths. Mismatched Kafka topic names and missing cross-service JSON type mappings lead to broken consumer flows and silent event drops.

---

## 1. Implemented Features (Verified in Code)

| Service | Implemented Components | Verified Details |
| :--- | :--- | :--- |
| **ApiGateway** | Route definitions, JWT parsing filter, Rate limit configuration (dormant), Kafka log producer | WebFlux reactive gateway routing to downstream Eureka service IDs (`lb://USERSERVICE`, `lb://BANKSERVICE`, etc.). |
| **DiscoveryService** | Eureka server registration | Standalone Netflix Eureka registry running on port 8761. |
| **UserService** | User registration, OTP login flow, JWT generation, Profile fetch | Generates 6-digit OTPs via `SecureRandom`, stores in Redis (10 min TTL), sends `CreateOtpEvent` via Kafka, generates JWT upon verification. |
| **BankService** | Bank registration, Bank login via OTP, Customer onboarding, KYC initiation, Manual bypass override, Timeline audit | MongoDB customer store (`BankCustomer`, `BankCustomerKycData`), Redis-backed OTP verification, Feign client to `UserService`, admin bypass flag. |
| **ConsentService** | Consent request handling, Consent approval/rejection via OTP, Consent queries | JPA-backed `ConsentRecord`, Redis OTP verification, Kafka events for `CreateOtpEvent` and `ConsentResponseEvent`. |
| **DigilockerService**| Mock DigiLocker OAuth/redirect, Mobile OTP verification, Mock data generation | Issues mock Aadhaar/PAN payload event `DigiLockerDataFetchedEvent` to Kafka upon OTP validation. |
| **EmailService** | Kafka consumer for OTP events, HTML email template dispatch | Listens to OTP topics, parses `CreateOtpEvent`, delegates to JavaMailSender using modular template strategies (`Consent`, `Digilocker`, `Login`). |
| **KycManagementService** | Ingestion from DigiLocker event, Redis KYC token caching, Field-level disclosure filtering | Persists `KycRecord` and `KycEntity`, issues temporary token IDs stored in Redis. |
| **LogMonitoringSystem**| Kafka batch listener for `log-info`, `log-warning`, `log-error` | Persists log events into MySQL `logs` table via `LogRepository`. |
| **NotificationSystem** | STOMP WebSocket over SockJS, Redis-backed session registry | Redis channel subscriber for real-time WebSocket notifications to connected frontend clients. |
| **AiService** | REST proxy to Groq Chat API | Exposes `/ai/api/groq` using `RestTemplate` to query Groq LLaMA models with a hardcoded context prompt. |

---

## 2. Missing or Incomplete Features

1. **Root Build Orchestration**: No root `pom.xml` exists. Each of the 11 microservices has an independent, unparented `pom.xml`, preventing unified multi-module builds, unified dependency management, and monorepo CI.
2. **Automated Test Suite**: Zero automated tests exist. All test suites were wiped by `Backend/remove-tests.sh`. No unit, integration, Testcontainers, or contract tests remain.
3. **Document / Artifact Storage**: Despite documentation claiming secure document storage, no file upload endpoints (`MultipartFile`), object storage abstractions (AWS S3, MinIO, Azure Blob), or signed upload/download URL workflows exist. The platform purely operates on mock string JSON attributes.
4. **Database Migrations (Flyway/Liquibase)**: Zero migration scripts exist. All relational microservices rely on Hibernate runtime schema mutation (`hibernate.ddl-auto: update`), violating enterprise database change management controls.
5. **Consent Expiry & Revocation Lifecycle Enforcement**: Consents store an `expiryAt` timestamp, but no scheduled cron or background worker invalidates expired consents or alerts downstream institutions.
6. **Token Refresh & Key Rotation**: JWTs are issued without refresh tokens. Expirations vary wildly across services (from 2.3 hours to 10 days). No key rotation mechanism or JWKS endpoint exists.
7. **Distributed Tracing & Correlation IDs**: No OpenTelemetry or Micrometer Tracing integration exists. HTTP requests and Kafka event chains do not propagate `X-Correlation-Id` or W3C TraceContext headers.
8. **Threat Assessment & Profiling Logic**: `LogMonitoringSystem` defines entity classes `ThreatAssessment` and `TrafficProfileEntity`, but contains zero business logic, controllers, or processors to calculate scores or profile traffic.

---

## 3. Hard-Coded Secrets & Insecure Credentials

| File Location | Severity | Hard-coded Value / Finding | Security Impact |
| :--- | :--- | :--- | :--- |
| `deploy/k8s/02-secrets.yaml`:13-26 | **CRITICAL** | `MYSQL_ROOT_PASSWORD: "899961"`<br>`JWT_SECRET: "404E63526655..."`<br>`ENCRYPTION_SECRET_KEY: "404E6352..."`<br>`APIGATEWAY_SECURITY_SECRET_KEY: "dsfkjnas..."` | Plaintext secrets committed to version control; immediate cluster compromise if deployed. |
| `Backend/BankService/src/main/resources/application.properties`:9, 24 | **CRITICAL** | `spring.mongodb.uri=mongodb://root:899961@localhost:27017...`<br>`encryption.secret-key=404E6352...` | Direct database credential exposure and shared static symmetric key. |
| `Backend/BankService/src/main/java/org/rbi/bankservice/security/JwtValidator.java`:15-16 | **CRITICAL** | `private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";` | JWT validation key compiled directly into bytecode. |
| `Backend/ConsentService/src/main/resources/application.yml`:15, 65 | **CRITICAL** | `password: 899961`<br>`encryption.secret-key: 404E6352...` | Database password and encryption key committed in plain text. |
| `Backend/DigilockerService/src/main/resources/application.yaml`:37 | **CRITICAL** | `secret.key: 404E6352...` | Static key committed in config. |
| `Backend/KycManagementService/src/main/resources/application.properties`:9, 57, 68, 116 | **CRITICAL** | `spring.datasource.password=899961`<br>`encryption.secret-key=404E6352...` | Repeated plaintext database and AES encryption keys. |
| `Backend/LogMonitoringSystem/src/main/resources/application.yml`:40 | **CRITICAL** | `password: 899961` | Plaintext database root credentials. |
| `Backend/NotificationSystem/src/main/resources/application.yaml`:13, 45 | **CRITICAL** | `password: 899961`<br>`jwt.secret: 404E6352...` | Plaintext database and JWT secret keys. |
| `Backend/UserService/src/main/resources/application.yml`:12, 42, 55 | **CRITICAL** | `password: 899961`<br>`jwt.secret: 404E6352...`<br>`encryption.secret-key: 404E6352...` | Plaintext credentials for user identity store. |
| `Backend/docker-compose.yml`:10 | **HIGH** | `MYSQL_ROOT_PASSWORD: root` | Default weak credentials in local orchestrator. |

---

## 4. Broken or Inconsistent APIs

1. **ApiGateway Routing vs Filter Prefix Mismatch**:
   - `Backend/ApiGateway/src/main/resources/application.yml` routes `Path=/user/**` to `USERSERVICE`.
   - `Backend/ApiGateway/src/main/java/com/rbi/apigateway/filters/JwtFilter.java`:48 checks `if (path.startsWith("/users/") ...)`.
   - **Result**: The condition NEVER evaluates to true because the actual route prefix is `/user/`, completely breaking the intended gateway role check.
2. **KycManagementService Search Logic Defect**:
   - `Backend/KycManagementService/src/main/java/org/rbi/kycmanagementservice/consumer/KycKafkaConsumer.java`:51-53:
     ```java
     KycRecord record = kycRecordRepository.findByEmail(event.getMobileNumber()).orElse(new KycRecord());
     ```
   - Searches the `email` column using a mobile phone number. Existing records are never matched, producing duplicate orphan records on every sync.
3. **Disjoint Entity Models in KycManagementService**:
   - `KycKafkaConsumer` stores data exclusively in the `KycRecord` table (`kyc_records`).
   - `KycService.getVerifiedDataForBank()` and `getUserProfile()` read exclusively from `KycEntity` (`kyc_entities`).
   - Ingestion through DigiLocker Kafka never updates `KycEntity`. When banks query verified data, it returns empty or unverified status.
4. **Mismatched Notification Kafka Topics**:
   - `ConsentServiceImpl` publishes notification alerts to topic `notification-events-topic` (`KafkaTopicNameConstants.NOTIFICATION_EVENTS_TOPIC`).
   - `NotificationKafkaConsumer` subscribes to topic `notifications` (`@KafkaListener(topics = "notifications")`).
   - **Result**: WebSocket alerts for consent requests are never delivered to end users.
5. **EmailService Deserialization Mapping Deficiency**:
   - `EmailService/src/main/resources/application.properties` configures `spring.kafka.consumer.properties.spring.json.type.mapping` only for `UserService` and `ConsentService`.
   - `BankService` and `DigilockerService` emit `CreateOtpEvent` using distinct package names (`org.rbi.bankservice.event.CreateOtpEvent` and `org.kyc.digilockerService.kafka.CreateOtpEvent`).
   - **Result**: EmailService crashes with a deserialization failure when receiving OTP events from Bank or DigiLocker services.
6. **Inconsistent Error Formats**:
   - Some endpoints return plain text strings (`ResponseEntity<String>`), others return `{ "error": "msg" }`, others return `{ "timestamp": ..., "status": 400, "error": ..., "message": ... }`.
   - `ConsentService`'s `GlobalExceptionHandler` is entirely commented out, returning raw Spring Whitelabel HTML error traces.

---

## 5. Authorization Bypasses & Insecure Endpoints

| Service & File | Endpoint / Method | Vulnerability Description | Severity |
| :--- | :--- | :--- | :--- |
| **ApiGateway**<br>`JwtFilter.java`:31-33 | All routes | `if (authheader == null \|\| !authheader.startsWith("Bearer ")) { return chain.filter(exchange); }`<br>If an incoming request omits the Authorization header, the filter passes it right through to downstream services without authentication. | **CRITICAL** |
| **KycManagementService**<br>`KycUserDashboardController.java`:19-58 | `GET /api/kyc/{userId}`<br>`GET /api/kyc/{userId}/documents`<br>`GET /api/kyc/{userId}/token`<br>`POST /api/kyc/{userId}/token/revoke` | Zero Spring Security configuration exists in this service. Any unauthenticated caller can query any user's profile, document proofs, tokens, or revoke their KYC status. | **CRITICAL** |
| **KycManagementService**<br>`KycDataIngestionController.java`:20-25 | `POST /api/kyc/digilocker` | Unauthenticated endpoint accepting arbitrary KYC claims directly into the core identity repository. | **CRITICAL** |
| **KycManagementService**<br>`JwtParserUtil.java`:26-28 | `parsePayload(String token)` | Base64-decodes JWT claims directly without calling cryptographic signature verification. Attackers can forge any claims with arbitrary user IDs. | **CRITICAL** |
| **UserService**<br>`UserProfileController.java`:20-33 | `GET /user/details?email=...`<br>`GET /user/details/{id}`<br>`GET /user/kyc-status/{email}` | Entire service lacks Spring Security. Full PII details (name, email, phone, status) can be harvested for any user. | **CRITICAL** |
| **UserService**<br>`UserServiceImpl.java`:129-130 | `POST /user/otp/verify` | Token role is extracted from request payload DTO (`otpVerificationDto.getRole()`) rather than the database entity (`user.getRoles()`), permitting instant client-driven privilege escalation to `ADMIN`. | **CRITICAL** |
| **BankService**<br>`BankKycController.java`:39-42 | `POST /bank/kyc/bypass` | `adminBypass()` has no method-level security check; while SecurityConfig mentions `.hasRole("ADMIN")`, the missing token claim validation and Gateway header bypass allow unauthorized users to trigger KYC bypasses. | **HIGH** |
| **BankService**<br>`BankConsentController.java`:22-25 | `GET /bank/kyc/consents/approved` | Returns approved consents across all financial institutions, completely violating multi-tenant bank isolation. | **HIGH** |

---

## 6. Unsafe Kafka, Redis, Database & File-Handling Behavior

1. **Unprotected Plaintext OTP Logging & Printing**:
   - `UserServiceImpl.java`:59-60: `log.info("OTP: {}", finalotp); System.out.println(finalotp);`
   - `ConsentServiceImpl.java`:205: `log.info("Generated OTP for consent {}: {}", consentIdValue, otp);`
   - `DigilockerServiceImpl.java`:49: `System.out.println(finalotp);`
   - Plaintext OTPs appear in container stdout and application logs, enabling credential theft.
2. **Missing Rate Limiting & Brute-Force Lockout on OTP Verification**:
   - OTPs have a 10-minute validity window. Failed verification attempts do not increment a counter or trigger lockout. A 6-digit numeric OTP can be brute-forced (1,000,000 attempts) across the 10-minute window.
3. **Database Level Encryption Commented Out**:
   - `UserService/src/main/java/org/rbi/userservice/Model/UserEntity.java`:34, 38, 42 has `@Convert(converter = StringCryptoConverter.class)` commented out. PII is written unencrypted to MySQL.
4. **Invalid MySQL Column Definition**:
   - `UserEntity.java`:43: `@Column(nullable = false, unique = true, columnDefinition = "TEXT") private String phone;`
   - MySQL rejects unique constraints on unindexed/unprefixed `TEXT` columns (`BLOB/TEXT column 'phone' used in key specification without a key length`).
5. **Full In-Memory Table Scans**:
   - `BankServiceImpl.java`:241: `repo.findAll().stream().filter(...)` loads the entire customer database table into JVM heap memory.
6. **Unsafe Kafka Consumer Exception Handling**:
   - `LogMonitoringSystem/src/main/java/org/rbi/logmonitoringsystem/listeners/EventListeners.java`:44-46 catches `Exception` and executes `ex.printStackTrace()`, committing offsets and permanently dropping lost audit logs.
   - Consumers across all services lack Dead Letter Queues (DLQ) and backoff retries.

---

## 7. Deployment and CI/CD Gaps

1. **Broken GitHub Actions Pipeline**:
   - `.github/workflows/maven.yml`:31 executes `mvn -B package --file pom.xml`. Because no `pom.xml` exists in the repository root, CI fails immediately upon every commit and PR.
2. **Kubernetes Security Violations**:
   - Manifests in `deploy/k8s/` run containers as `root` without a `securityContext`.
   - All deployments use floating or unpinned tags (`kyc-system/<service>:latest`).
   - No NetworkPolicies exist; internal databases (`mysql`, `mongodb`, `redis`, `kafka`) are accessible across the cluster without network segmentation.
   - Ingress is completely missing; `api-gateway` exposes an unencrypted plaintext `LoadBalancer` service.
   - No HorizontalPodAutoscalers (HPA) or PodDisruptionBudgets (PDB) are configured.
3. **Insecure Dockerfiles**:
   - Dockerfiles across all 11 services assume a pre-built JAR in `target/*.jar` and run as root (`eclipse-temurin:21-jre-alpine`).
   - No multi-stage build, no digest pinning, no non-root user creation, and no container resource flags.

---

## 8. Configuration Inconsistencies Across Services

1. **Spring Boot Version Divergence**:
   - `AiService`: 4.0.4 (Non-existent / experimental)
   - `ApiGateway`: 3.5.9
   - `BankService`: 4.0.2
   - `ConsentService`: 4.0.2
   - `DigilockerService`: 3.5.10
   - `DiscoveryService`: 3.5.9
   - `EmailService`: 4.0.2
   - `KycManagementService`: 3.4.2
   - `LogMonitoringSystem`: 3.4.2
   - `NotificationSystem`: 3.5.9
   - `UserService`: 3.5.9
   - *Production Standard*: Must be standardized to official, supported Spring Boot 3.4.x LTS.
2. **Format Inconsistency**: Services randomly mix `application.properties`, `application.yml`, and `application.yaml`. Some services contain redundant duplicate files (`ApiGateway` and `UserService` have both `.properties` and `.yml` with conflicting property keys).
3. **Hard-Coded Hostnames**: `localhost` and `127.0.0.1` are hard-coded in production configuration templates rather than using environment variables with profile-based fallbacks.
4. **JWT Expiration Variance**:
   - `ApiGateway`: 864,000,000 ms (10 days)
   - `NotificationSystem`: 86,400,000 ms (1 day)
   - `UserService`: 8,460,000 ms (~2.35 hours)
   - `BankService`: 3,600,000 ms (1 hour)
