# Consent Service

The **Consent Service** is a core component of the Centralized KYC System. It manages the lifecycle of KYC consent requests initiated by Banks/Organizations seeking user data access, ensuring secure approval with OTP authentication.

---

## 🏗️ Architecture Layout

Below is a Mermaid chart describing the architecture and module relationships:

```mermaid
graph TD
    B[Bank Service] -->|1. ConsentRequestEvent| K[(Kafka: consent-request-topic)]
    K --> CS[Consent Service]
    CS -->|2. Save Record| DB[(MySQL DB)]
    CS -->|3. Cache OTP| R[(Redis)]
    CS -->|4. NotificationEvent| KN[(Kafka: notification-events-topic)]
    CS -->|5. CreateOtpEvent| KE[(Kafka: otp-created-topic)]
    KE --> ES[Email Service]
    ES -->|6. Email dispatch| U[User / Client]
    U -->|7. Approve Consent| CS
    CS -->|8. ConsentApprovedEvent| B
```

---

## 🔄 Lifecycle Sequence

The standard request-approval flow executing over event-driven hooks executed inside this service:

```mermaid
sequenceDiagram
    participant B as Bank Service
    participant K as Kafka Broker
    participant C as Consent Service
    participant R as Redis
    participant E as Email Service
    participant U as User / Client

    B->>K: Publish ConsentRequestEvent
    K->>C: Consume ConsentRequestEvent
    Note over C: Creates ConsentRecord (PENDING)
    C->>R: Cache 6-digit OTP (TTL 10m)
    C->>K: Trigger CreateOtpEvent
    C->>K: Trigger NotificationEvent
    K->>E: Consume CreateOtpEvent
    E-->>U: Dispatches Email with OTP
    U->>C: POST /consents/{id}/status?action=Accept&otp=123456
    C->>R: Verify OTP matching
    Note over C: Generate Secure Consent Token (JWT)
    C->>C: Update status to APPROVED
    C->>K: Publish ConsentApprovedEvent to Bank
```

---

## 📌 Features

- **Event-Driven Intake**: Automatically processes incoming KYC requests without synchronous blocking setups.
- **Auto OTP Generation**: Immediately caches a Redis variable and triggers an `EmailService` endpoint dispatch for every request intakes.
- **Mapped URLs**: Forwards actionable endpoints to push Notification system redirects directly to approval hooks.
- **Secure Consent Token (JWT)**: Mints standard tokens bundling the absolute fields approved for secure downstream accessing workflows.

---

## 🛠️ API Endpoints

All endpoints use base routing prefix: `/consents`

| Endpoint | Method | Params | Description |
| :--- | :--- | :--- | :--- |
| `/request` | `POST` | `@RequestBody ConsentRequestDto` | (Internal/Bank trigger) Receives trigger requests. |
| `/send-otp/{consentId}` | `POST` | `@PathVariable UUID` | Manually triggers/resends the verification OTP. |
| `/approve/{consentId}` | `POST` | `@RequestParam String otp` | Approves and generates access JWT tokens. |
| `/reject/{consentId}` | `POST` | `@RequestParam String otp` | Rejects targeting consent scope. |
| `/{consentId}/status` | `POST` | `action=[Accept/Decline], otp=123456` | **Unified Endpoint**: Route unified form/link actions. |
| `/user/{userId}` | `GET` | `@PathVariable String` | Returns all consent records bound to a specific user. |

---

## ⚙️ Prerequisites Configuration (`application.yml`)

The following variables support JSON serialisation for `@KafkaListener` consumption securely:

```yaml
spring:
  kafka:
    consumer:
      bootstrap-servers: localhost:9092
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
```
