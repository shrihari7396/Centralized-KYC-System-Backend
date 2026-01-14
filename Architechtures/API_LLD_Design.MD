
# Low Level Design (LLD) – API Design Documentation

---

## 2.2 User Service – LLD

### APIs

#### POST /users/register
- Purpose: Register a new user

#### POST /users/verify-otp
- Purpose: Verify OTP for a registered user

#### GET /users/{id}
- Purpose: Fetch user details by user ID

---

### Data Model

#### users
- Stores basic user information

#### user_status
- Stores current status of the user (e.g. pending, active)

---

### Kafka Integration
- **Produces:** `otp.requested`
- **Consumes:** `otp.verified`

---

### Diagrams
- Registration sequence diagram
- OTP verification sequence diagram

---

## 2.3 OTP Service – LLD

### APIs (Internal)

#### POST /otp/generate
- Purpose: Generate OTP for a user

#### POST /otp/verify
- Purpose: Verify OTP submitted by user

---

### Redis Design
- **Key format:** `otp:{userId}`
- **TTL strategy:** OTP expires after configured time
- **Retry counters:** Maintains retry count per OTP

---

### Kafka Integration
- **Consumes:** `otp.requested`
- **Produces:** `otp.verified`, `otp.failed`

---

### Diagrams
- OTP lifecycle state machine

---

## 2.4 KYC Management Service – LLD

### APIs

#### POST /kyc/submit
- Purpose: Submit KYC data for a user

#### GET /kyc/{userId}
- Purpose: Fetch KYC details for a user

---

### Data Model

#### kyc_profile
- Stores core KYC profile information

#### kyc_documents
- Stores document metadata related to KYC

#### kyc_status
- Stores KYC status for the user

---

### Integrations
- DigiLocker client
- Circuit breaker configuration for external calls

---

### Diagrams
- KYC submission sequence diagram
- KYC state transition diagram

---

## 2.5 DigiLocker Integration Service – LLD

### APIs

#### POST /digilocker/fetch
- Purpose: Fetch KYC documents from DigiLocker

---

### Vault Integration
- Token retrieval
- Token refresh

---

### Failure Handling
- Retry mechanism
- Fallback handling
- Asynchronous failure events

---

## 2.6 Consent Management Service – LLD

### APIs

#### POST /consents/request
- Purpose: Create a consent request

#### POST /consents/approve
- Purpose: Approve a consent request

#### GET /consents/{userId}
- Purpose: Fetch consents for a user

---

### Data Model

#### consents
- Stores consent records

#### consent_audit
- Stores audit trail of consent actions

---

### Kafka Integration
- **Produces:** `consent.approved`, `consent.rejected`

---

### Diagrams
- Consent lifecycle state machine

---

## 2.7 Bank Integration Service – LLD

### APIs

#### POST /banks/kyc/request
- Purpose: Bank requests KYC for a user

#### GET /banks/kyc/{userId}
- Purpose: Fetch KYC details for a user

---

### Security
- Bank authentication
- Scope-based authorization

---

### Diagrams
- Bank-to-KYC access sequence diagram

---

## 2.8 Audit & Logging Service – LLD

### Kafka Integration
- **Consumes:** `audit.*`

---

### Data Model

#### audit_events
- Stores audit event records

---

### Non-Functional Characteristics
- High throughput
- No business logic
