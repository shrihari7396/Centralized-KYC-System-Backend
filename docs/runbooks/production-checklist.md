# Production Readiness & Release Checklist

**Platform**: Centralized Distributed KYC & Consent Platform  
**Target Environment**: Staging & Production Kubernetes Clusters  
**Audience**: DevOps, SRE, Platform Security, and Backend Engineering  

---

## 1. Pre-Deployment Secret & Credential Audit

- [ ] **No Committed Credentials**: Run `git log -S "899961"` and secret scanning (e.g. `gitleaks` / `trufflehog`) to ensure no real passwords or keys exist in git history.
- [ ] **Kubernetes Secret Store**: Verify that `deploy/k8s/02-secrets.yaml` contains only placeholders or references an external secret store (HashiCorp Vault / AWS Secrets Manager / SealedSecrets / External Secrets Operator).
- [ ] **Secret Rotation Completed**: Ensure previously committed demo keys (`404E6352...`, `899961`, `dsfkjnas...`) have been fully rotated across all staging and production environments.
- [ ] **Dedicated AES Keys**: Verify that distinct, cryptographically random 256-bit keys (64-character hex) are assigned to `ENCRYPTION_SECRET_KEY` and `JWT_SECRET`.
- [ ] **Third-Party API Credentials**: Verify `GROQ_API_KEY` and `SMTP_PASSWORD` are provisioned exclusively through secure environment variables.

---

## 2. Configuration & Profile Verification

- [ ] **Spring Profile Segregation**:
  - `local`: Developer defaults, embedded or local containers, debug logs allowed.
  - `test`: Testcontainers, ephemeral databases, mock mailers.
  - `staging`: Staging cluster infrastructure, strict auth, masked logging.
  - `production`: High availability cluster, TLS/SASL, non-root, strict validation.
- [ ] **No Localhost Defaults in Production**: Verify `application-production.yml` contains zero occurrences of `localhost`, `127.0.0.1`, or hard-coded service ports.
- [ ] **Startup Property Validation**: Verify each service executes an `@PostConstruct` or `ApplicationRunner` configuration validator ensuring mandatory properties (`JWT_SECRET`, `SPRING_DATASOURCE_URL`, `SPRING_KAFKA_BOOTSTRAP_SERVERS`) are present and non-empty.

---

## 3. Database & Migration Validation

- [ ] **Flyway Migrations Implemented**:
  - Verify `db/migration/V1__*.sql` exists for each relational service (`UserService`, `KycManagementService`, `ConsentService`, `NotificationSystem`, `LogMonitoringSystem`).
  - Verify `spring.jpa.hibernate.ddl-auto=validate` (or `none`) is set for production profiles. `update` or `create-drop` must be strictly forbidden.
- [ ] **Field-Level Encryption Verified**:
  - Verify JPA `@Convert(converter = StringCryptoConverter.class)` is active on `UserEntity` (`first_name`, `last_name`, `phone`) and `KycEntity` (`aadhaar_no`, `pan`, `driving_license`).
  - Query MySQL directly after test ingestion to confirm fields store ciphertexts (`AES/GCM/NoPadding` Base64) rather than plaintext strings.
- [ ] **Index & Constraint Verification**:
  - Confirm `phone` and `email` columns have appropriate character lengths and indexes in relational schemas.
- [ ] **Polyglot Persistence Health**:
  - MySQL replica connectivity verified.
  - MongoDB replica set connectivity for `bank_customer_kyc_data` verified with authentication.

---

## 4. Kafka & Event Stream Hardening

- [ ] **Topic Alignment**:
  - `consent-otp-topic` -> Consumed by EmailService.
  - `userservice-otp-topic` -> Consumed by EmailService.
  - `bankservice-otp-topic` -> Consumed by EmailService.
  - `digilocker-otp-topic` -> Consumed by EmailService.
  - `kyc-verification-success` -> Consumed by UserService, EmailService.
  - `digilocker-data-fetched` -> Consumed by KycManagementService.
  - `consent-request-topic` -> Consumed by ConsentService.
  - `consent-response-topic` -> Consumed by KycManagementService, BankService.
  - `kyc-data-topic` -> Consumed by BankService.
  - `notifications` -> Consumed by NotificationSystem.
- [ ] **Dead Letter Topics (DLT) & Retries**:
  - All listeners configured with `@RetryableTopic` or `DefaultErrorHandler` with exponential backoff (e.g., 3 attempts, 1s delay, 2.0x multiplier).
  - Dead letter topics monitored with alert rules on message count > 0.
- [ ] **Consumer Idempotency**:
  - Consumers verify event uniqueness via Redis key `processed_event:{eventId}` before executing business writes.
- [ ] **Transport Security**:
  - SASL/SCRAM authentication and SSL/TLS encryption enabled for production brokers.

---

## 5. Security & Access Control Verification

- [ ] **Gateway Authorization Enforcement**:
  - Sending a request to `/api/kyc/**`, `/bank/**`, or `/consents/**` without an `Authorization` header returns HTTP `401 Unauthorized`.
  - Sending an invalid or expired token returns HTTP `401 Unauthorized`.
  - Tampered JWT payload returns HTTP `401 Unauthorized`.
- [ ] **Service-Level Spring Security**:
  - `UserService`, `BankService`, `ConsentService`, and `KycManagementService` independently enforce security filters. Bypassing the gateway cannot compromise backend services.
- [ ] **Zero PII & OTP Logging**:
  - Grep test logs for OTP patterns `[0-9]{6}` and ensure zero matches.
  - Verify that Logback configuration masks email, phone, and national ID patterns.
- [ ] **Brute-Force OTP Protection**:
  - Verify that entering 3 invalid OTPs locks the OTP key in Redis and requires requesting a new OTP.
- [ ] **Role & Tenant Enforcement**:
  - Calling `/bank/kyc/bypass` with a non-ADMIN token returns HTTP `403 Forbidden`.
  - Bank A querying Bank B's customers or stats returns HTTP `403 Forbidden`.

---

## 6. Container & Kubernetes Hardening

- [ ] **Non-Root Execution**:
  - Inspect running container: `kubectl exec -it <pod> -- id` must output `uid=10001(appuser) gid=10001(appgroup)`.
- [ ] **Minimal Digest-Pinned Base Images**:
  - Base image uses `eclipse-temurin:21-jre-alpine` or distroless, pinned with SHA256 digest.
- [ ] **Resource Limits & Requests**:
  - Every deployment specifies `resources.requests.cpu`, `resources.requests.memory`, `resources.limits.cpu`, `resources.limits.memory`.
- [ ] **Actuator Probes Configured**:
  - `livenessProbe` points to `/actuator/health/liveness`.
  - `readinessProbe` points to `/actuator/health/readiness`.
- [ ] **Network Segmentation**:
  - `NetworkPolicy` applied ensuring databases (`mysql`, `mongodb`, `redis`) only receive traffic from approved application pods.
- [ ] **TLS Ingress**:
  - Ingress configured with cert-manager TLS secret, replacing plaintext LoadBalancer service.

---

## 7. Observability & Operational Readiness

- [ ] **Actuator Prometheus Endpoint**:
  - `GET /actuator/prometheus` returns metrics across all 11 services.
- [ ] **Structured JSON Logging**:
  - Production logs formatted as JSON with fields `timestamp`, `level`, `service`, `traceId`, `spanId`, `correlationId`, `message`.
- [ ] **Health Dashboards & Alerts**:
  - Alert rules configured for: High Error Rate (> 1% 5xx), High P99 Latency (> 2s), Kafka Consumer Lag (> 500), Pod Restarts (> 3 in 10m).

---

## 8. Verification & Smoke Test Commands

```bash
# 1. Verify compilation and all tests across modules
mvn clean test

# 2. Check for dependency vulnerabilities
mvn org.owasp:dependency-check-maven:check

# 3. Validate Kubernetes manifests with dry-run
kubectl apply --dry-run=client -k deploy/k8s/

# 4. Probe Gateway unauthenticated rejection (Must return 401)
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8083/api/kyc/user123

# 5. Probe Actuator health endpoints across services
curl -s http://localhost:8083/actuator/health/liveness
curl -s http://localhost:8090/actuator/health/readiness
```
