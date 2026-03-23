# BankService - KYC Management Service

## Project Overview

**BankService** is a microservice that manages Know Your Customer (KYC) workflows and user verification processes within the Centralized KYC System. It handles customer registration, OTP verification, KYC data management, and consent-based user data access for banking partners.

### Key Responsibilities
- Manage bank customer KYC requests and verification status
- Handle OTP generation and verification for user authentication
- Manage customer consent records and approve/deny requests
- Fetch and expose verified KYC data to authorized bank partners
- Support administrative bypass operations for KYC requirements
- Coordinate with other microservices (DigiLocker, KYC Management, Consent Service)

---

## Technology Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| **Java** | 21 | Backend programming language |
| **Spring Boot** | 4.0.2 | Framework for building microservices |
| **Spring Cloud** | 2025.1.0 | Microservices architecture support |
| **MongoDB** | 7.0+ | NoSQL document database |
| **Apache Kafka** | 9092 | Event streaming and async messaging |
| **Redis** | In-Memory | Caching and session management |
| **Eureka** | Netflix | Service discovery/registration |
| **OpenFeign** | Spring Cloud | Declarative REST client |
| **JUnit 5** | Testing | Unit and integration testing |
| **Testcontainers** | Docker-based | Container-based testing |

---

## Service Configuration

### Port
- **Server Port**: `8091`

### Service Name (Eureka)
- **Service ID**: `BANKSERVICE`

### Database
- **Type**: MongoDB
- **URL**: `mongodb://root:899961@localhost:27017/kyc?authSource=admin`
- **Collection**: `bank_customers`

### Message Broker (Kafka)
- **Bootstrap Servers**: `localhost:9092`
- **Consumer Group**: `bank-service-group`
- **Auto Offset Reset**: `earliest`

### Service Discovery (Eureka)
- **Eureka URL**: `http://localhost:8761/eureka`
- **Register with Eureka**: `true`
- **Fetch Registry**: `true`

---

## API Documentation

### Base URL
```
http://localhost:8091/bank/kyc
```

---

## 📋 API Endpoints

### 1. **Initiate KYC Request**

**Endpoint**: `POST /bank/kyc/initiate`

**Description**: Initiates a new KYC request for a user. This endpoint starts the KYC verification process by capturing the bank's requirements for customer verification.

**Request Body** (`BankKycInitiateRequest`):
```json
{
  "bankId": "550e8400-e29b-41d4-a716-446655440000",
  "requestedDocuments": [
    "AADHAAR",
    "PAN",
    "BANK_STATEMENT"
  ],
  "email": "user@example.com"
}
```

**Request Parameters**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `bankId` | UUID | Yes | Unique identifier of the bank |
| `requestedDocuments` | List<DocumentType> | Yes | List of document types needed for verification. Valid values: `AADHAAR`, `PAN`, `DRIVING_LICENSE`, `BANK_STATEMENT`, `ADDRESS_PROOF`, `INCOME_PROOF`, `PHOTO` |
| `email` | String | Yes | Email address of the user initiating KYC |

**Response** (`BankCustomer`):
```json
{
  "id": "507f1f77bcf86cd799439011",
  "userId": "user-123",
  "bankId": "550e8400-e29b-41d4-a716-446655440000",
  "consentIds": [
    "consent-001",
    "consent-002"
  ],
  "kycRequestId": 12345,
  "riskLevel": "LOW",
  "overrideReason": null,
  "verifiedAt": null,
  "createdAt": "2024-03-23T10:30:00"
}
```

**Response Parameters**:
| Field | Type | Description |
|-------|------|-------------|
| `id` | String | MongoDB document ID |
| `userId` | String | User's unique identifier |
| `bankId` | String | Bank identifier |
| `consentIds` | List<String> | Associated consent request IDs |
| `kycRequestId` | Long | KYC request tracking ID |
| `riskLevel` | String | Risk assessment level (LOW, MEDIUM, HIGH) |
| `overrideReason` | String | Administrative override reason (if applicable) |
| `verifiedAt` | DateTime | Timestamp when KYC was verified |
| `createdAt` | DateTime | Timestamp when record was created |

**HTTP Status Codes**:
- `200 OK`: KYC request initiated successfully
- `400 Bad Request`: Invalid input or validation failed
- `500 Internal Server Error`: Server error occurred

---

### 2. **Get Customer Status**

**Endpoint**: `GET /bank/kyc/status/{userId}/{bankId}`

**Description**: Retrieves the current KYC verification status of a customer for a specific bank.

**Path Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | String | Yes | User's unique identifier |
| `bankId` | String | Yes | Bank's unique identifier |

**Query Parameters**: None

**Response** (`BankCustomer`):
```json
{
  "id": "507f1f77bcf86cd799439011",
  "userId": "user-123",
  "bankId": "550e8400-e29b-41d4-a716-446655440000",
  "consentIds": [
    "consent-001"
  ],
  "kycRequestId": 12345,
  "riskLevel": "LOW",
  "overrideReason": null,
  "verifiedAt": "2024-03-23T11:45:00",
  "createdAt": "2024-03-23T10:30:00"
}
```

**HTTP Status Codes**:
- `200 OK`: Status retrieved successfully
- `404 Not Found`: Customer not found for the given userId and bankId
- `500 Internal Server Error`: Server error occurred

**Example Request**:
```bash
GET http://localhost:8091/bank/kyc/status/user-123/550e8400-e29b-41d4-a716-446655440000
```

---

### 3. **Admin Bypass KYC Requirement**

**Endpoint**: `POST /bank/kyc/bypass`

**Description**: Administrative endpoint to bypass KYC verification requirements for a customer. This should only be accessible to authorized admin users.

**Request Body** (`BypassRequestDto`):
```json
{
  "userId": "user-123",
  "reason": "Pre-verified customer from legacy system"
}
```

**Request Parameters**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `userId` | String | Yes | User's unique identifier to bypass verification for |
| `reason` | String | Yes | Administrative reason for bypassing KYC |

**Response** (`BankCustomer`):
```json
{
  "id": "507f1f77bcf86cd799439011",
  "userId": "user-123",
  "bankId": "550e8400-e29b-41d4-a716-446655440000",
  "consentIds": [],
  "kycRequestId": null,
  "riskLevel": "BYPASSED",
  "overrideReason": "Pre-verified customer from legacy system",
  "verifiedAt": "2024-03-23T10:35:00",
  "createdAt": "2024-03-23T10:30:00"
}
```

**HTTP Status Codes**:
- `200 OK`: Bypass applied successfully
- `400 Bad Request`: Invalid input or validation failed
- `403 Forbidden`: Unauthorized access
- `500 Internal Server Error`: Server error occurred

---

### 4. **Request OTP**

**Endpoint**: `POST /bank/kyc/request-otp`

**Description**: Sends an OTP (One-Time Password) to the user's email for authentication/verification purposes.

**Request Body** (`OtpRequestDto`):
```json
{
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Request Parameters**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `email` | String | Yes | User's email address where OTP will be sent |
| `firstName` | String | Yes | User's first name |
| `lastName` | String | Yes | User's last name |

**Response**:
```json
{
  "message": "OTP Sent successfully ..."
}
```

**HTTP Status Codes**:
- `201 Created`: OTP generated and sent successfully
- `400 Bad Request`: OTP sending failed or invalid input
- `500 Internal Server Error`: Server error occurred

**Example Request**:
```bash
curl -X POST http://localhost:8091/bank/kyc/request-otp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

---

### 5. **Verify OTP & Get Login Token**

**Endpoint**: `POST /bank/kyc/otp-verification`

**Description**: Verifies the OTP provided by the user and returns an authentication token if verification is successful.

**Request Body** (`OtpVerificationDto`):
```json
{
  "email": "user@example.com",
  "otp": "123456",
  "role": "CUSTOMER",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Request Parameters**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `email` | String | Yes | User's email address |
| `otp` | String | Yes | 6-digit OTP received by user |
| `role` | String | Yes | User's role in the system (e.g., CUSTOMER, ADMIN) |
| `firstName` | String | Yes | User's first name |
| `lastName` | String | Yes | User's last name |

**Response** (`LoginResponseDto`):
```json
{
  "email": "user@example.com",
  "role": "CUSTOMER",
  "firstName": "John",
  "lastName": "Doe",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lI..."
}
```

**Response Parameters**:
| Field | Type | Description |
|-------|------|-------------|
| `email` | String | User's email address |
| `role` | String | User's role in the system |
| `firstName` | String | User's first name |
| `lastName` | String | User's last name |
| `token` | String | JWT authentication token for subsequent API calls |

**HTTP Status Codes**:
- `200 OK`: OTP verified, token issued successfully
- `400 Bad Request`: Invalid OTP or verification failed
- `401 Unauthorized`: Authentication failed
- `500 Internal Server Error`: Server error occurred

**Example Request**:
```bash
curl -X POST http://localhost:8091/bank/kyc/otp-verification \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "otp": "123456",
    "role": "CUSTOMER",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

---

### 6. **Logout User**

**Endpoint**: `POST /bank/kyc/logout`

**Description**: Logs out the user by invalidating their authentication token and clearing session data.

**Headers**:
| Header | Value | Required | Description |
|--------|-------|----------|-------------|
| `Authorization` | Bearer <token> | Yes | Valid JWT token from login/OTP verification |

**Request Body**: Empty (no body required)

**Response**:
```json
{
  "message": "Logged out successfully"
}
```

**HTTP Status Codes**:
- `200 OK`: User logged out successfully
- `400 Bad Request`: Invalid authorization header or token format
- `401 Unauthorized`: Invalid or expired token
- `500 Internal Server Error`: Server error occurred

**Example Request**:
```bash
curl -X POST http://localhost:8091/bank/kyc/logout \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### 7. **List Approved Consents**

**Endpoint**: `GET /bank/kyc/consents/approved`

**Description**: Retrieves a list of all customers with approved consent records. This endpoint is typically used by banks to identify customers who have approved data sharing.

**Query Parameters**: None

**Response** (List of `BankCustomer`):
```json
[
  {
    "id": "507f1f77bcf86cd799439011",
    "userId": "user-123",
    "bankId": "550e8400-e29b-41d4-a716-446655440000",
    "consentIds": [
      "consent-001"
    ],
    "kycRequestId": 12345,
    "riskLevel": "LOW",
    "overrideReason": null,
    "verifiedAt": "2024-03-23T11:45:00",
    "createdAt": "2024-03-23T10:30:00"
  },
  {
    "id": "507f1f77bcf86cd799439012",
    "userId": "user-456",
    "bankId": "550e8400-e29b-41d4-a716-446655440057",
    "consentIds": [
      "consent-002"
    ],
    "kycRequestId": 12346,
    "riskLevel": "MEDIUM",
    "overrideReason": null,
    "verifiedAt": "2024-03-22T14:20:00",
    "createdAt": "2024-03-22T13:15:00"
  }
]
```

**HTTP Status Codes**:
- `200 OK`: List retrieved successfully
- `500 Internal Server Error`: Server error occurred

**Example Request**:
```bash
curl -X GET http://localhost:8091/bank/kyc/consents/approved
```

---

### 8. **Fetch Customer KYC Data**

**Endpoint**: `POST /bank/kyc/fetch/{userId}/{bankId}`

**Description**: Fetches the verified KYC data of a customer for a specific bank. This endpoint returns customer identity information and approved documents.

**Path Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | String | Yes | User's unique identifier |
| `bankId` | String | Yes | Bank's unique identifier |

**Request Body**: Empty (no body required)

**Response** (Map<String, Object>):
```json
{
  "identity": {
    "firstName": "John",
    "lastName": "Doe",
    "dateOfBirth": "1990-01-01",
    "gender": "MALE",
    "panNumber": "ABCDE1234F"
  },
  "documents": [
    {
      "documentType": "AADHAAR",
      "documentNumber": "123456789012",
      "issuedDate": "2015-05-10",
      "expiryDate": "2025-05-10",
      "verificationStatus": "VERIFIED"
    },
    {
      "documentType": "PAN",
      "documentNumber": "ABCDE1234F",
      "issuedDate": "2013-03-15",
      "expiryDate": null,
      "verificationStatus": "VERIFIED"
    }
  ],
  "kycStatus": "VERIFIED",
  "riskLevel": "LOW",
  "verifiedAt": "2024-03-23T11:45:00"
}
```

**HTTP Status Codes**:
- `200 OK`: KYC data retrieved successfully
- `400 Bad Request`: Invalid parameters
- `403 Forbidden`: User doesn't have approval for this bank
- `404 Not Found`: Customer or KYC data not found
- `500 Internal Server Error`: Server error occurred

**Example Request**:
```bash
curl -X POST http://localhost:8091/bank/kyc/fetch/user-123/550e8400-e29b-41d4-a716-446655440000
```

---

## 📝 Data Types and Enums

### DocumentType Enum
Valid document types that customers can provide:
```
AADHAAR           - Indian national ID
PAN               - Permanent Account Number (tax ID)
DRIVING_LICENSE   - Government-issued driving license
BANK_STATEMENT    - Bank account statement (address/identity proof)
ADDRESS_PROOF     - Proof of address document
INCOME_PROOF      - Income verification document (salary slip, ITR)
PHOTO             - Photograph ID
```

### Risk Level
- `LOW`: Low risk customer
- `MEDIUM`: Medium risk customer
- `HIGH`: High risk customer requiring additional verification
- `BYPASSED`: KYC requirements administratively bypassed

---

## 🔒 Authentication & Security

### JWT Token Usage
All secure endpoints require a Bearer token in the Authorization header:

```bash
Authorization: Bearer <jwt_token>
```

### Token Generation
Tokens are issued after successful OTP verification via the `/otp-verification` endpoint.

### Token Validation
The service validates JWT tokens using configured secret keys and ensures:
- Token signature is valid
- Token has not expired
- User has appropriate role/permissions

---

## 🔄 Integration with Other Services

### Service Dependencies

| Service | Purpose | Integration |
|---------|---------|-------------|
| **Eureka** | Service Discovery | Service registration and lookup |
| **API Gateway** | Request Routing | Routes requests to BankService |
| **DigiLocker Service** | Document Verification | Fetches verified documents |
| **KYC Management Service** | KYC Token Management | Issues and manages KYC tokens |
| **Consent Service** | Consent Management | Manages user consent records |
| **User Service** | User Data | User profile and authentication |
| **Email Service** | OTP Delivery | Sends OTP emails to users |
| **MongoDB** | Data Persistence | Stores customer and KYC data |
| **Kafka** | Event Streaming | Publishes KYC events and logs |
| **Redis** | Caching | Caches OTP and session data |

---

## ⚙️ Configuration Guide

### Environment Variables
Configure these in `application.properties`:

```properties
# Service Configuration
spring.application.name=BANKSERVICE
server.port=8091

# Database
spring.data.mongodb.uri=mongodb://user:password@host:port/database

# Kafka Broker
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=bank-service-group

# Service Discovery
eureka.client.service-url.defaultZone=http://localhost:8761/eureka

# Encryption
encryption.secret-key=your_encryption_key_here
```

---

## 🚀 Installation & Running

### Prerequisites
- **Java 21** or higher
- **Maven 3.6+**
- **MongoDB 7.0+** running
- **Kafka 3.0+** running
- **Redis 6.0+** running (optional, for caching)
- **Eureka Service Registry** running on port 8761

### Build the Service
```bash
cd BankService
mvn clean install
```

### Run the Service
```bash
# Using Maven
mvn spring-boot:run

# Or using Java
java -jar target/BankService-0.0.1-SNAPSHOT.jar
```

### Verify Service is Running
```bash
curl http://localhost:8091/actuator/health
```

### Check Service Registration with Eureka
Open browser and navigate to: `http://localhost:8761/`

Look for `BANKSERVICE` in the registered services list.

---

## 📮 Request/Response Examples

### Example 1: Complete KYC Flow

**Step 1: Initiate KYC**
```bash
curl -X POST http://localhost:8091/bank/kyc/initiate \
  -H "Content-Type: application/json" \
  -d '{
    "bankId": "550e8400-e29b-41d4-a716-446655440000",
    "requestedDocuments": ["AADHAAR", "PAN"],
    "email": "john@example.com"
  }'
```

**Step 2: Request OTP**
```bash
curl -X POST http://localhost:8091/bank/kyc/request-otp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

**Step 3: Verify OTP (User receives OTP via email)**
```bash
curl -X POST http://localhost:8091/bank/kyc/otp-verification \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "otp": "123456",
    "role": "CUSTOMER",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

**Step 4: Check Status (with token from Step 3)**
```bash
curl -X GET http://localhost:8091/bank/kyc/status/user-123/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <token_from_otp_verification>"
```

**Step 5: Fetch KYC Data (after approval)**
```bash
curl -X POST http://localhost:8091/bank/kyc/fetch/user-123/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <token>"
```

**Step 6: Logout**
```bash
curl -X POST http://localhost:8091/bank/kyc/logout \
  -H "Authorization: Bearer <token>"
```

---

## 🧪 Testing

### Run Unit Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn verify
```

### Test Classes
- **BankServiceImplTest.java**: Unit tests for service layer
- **BankControllerTest.java**: Unit tests for controller endpoints
- **BankServiceIntegrationTest.java**: Integration tests with database
- **BankServiceEndToEndTest.java**: End-to-end workflow tests

---

## 📊 Database Schema

### Bank Customers Collection
```javascript
db.bank_customers.createIndex({ userId: 1, bankId: 1 }, { unique: true })
db.bank_customers.createIndex({ consentIds: 1 })
db.bank_customers.createIndex({ createdAt: 1 })
```

### Sample Document
```json
{
  "_id": ObjectId("507f1f77bcf86cd799439011"),
  "userId": "user-123",
  "bankId": "550e8400-e29b-41d4-a716-446655440000",
  "consentIds": ["consent-001", "consent-002"],
  "kycRequestId": 12345,
  "riskLevel": "LOW",
  "overrideReason": null,
  "verifiedAt": ISODate("2024-03-23T11:45:00Z"),
  "createdAt": ISODate("2024-03-23T10:30:00Z")
}
```

---

## 🔗 API Response Codes Reference

| Code | Status | Description |
|------|--------|-------------|
| 200 | OK | Request successful |
| 201 | Created | Resource created successfully |
| 400 | Bad Request | Invalid request data or validation failed |
| 401 | Unauthorized | Invalid or missing authentication token |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 500 | Internal Server Error | Server error occurred |
| 503 | Service Unavailable | Service temporarily unavailable |

---

## 📞 Support & Troubleshooting

### Common Issues

**Q: Service not registering with Eureka**
- Ensure Eureka server is running on `http://localhost:8761`
- Check `eureka.client.service-url.defaultZone` in `application.properties`
- Verify network connectivity

**Q: MongoDB connection fails**
- Ensure MongoDB is running on the configured URI
- Verify credentials and database name
- Check network access to MongoDB host

**Q: OTP not being sent**
- Verify Kafka is running properly
- Check Email Service is registered and running
- Inspect logs for Kafka producer errors

**Q: JWT token validation fails**
- Ensure token format is `Bearer <token>` in Authorization header
- Verify token hasn't expired
- Check encryption secret key matches

---

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [MongoDB Documentation](https://docs.mongodb.com/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [JWT Introduction](https://jwt.io/introduction)

---

## 📄 Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2024-03-23 | Initial release with core KYC endpoints |

---

## 👨‍💼 Author & Maintainer

**Senior Backend Developer**  
Centralized KYC System Team

---

## 📋 DTO Class Hierarchy

```
bankservice/dto/
├── BankKycInitiateRequest.java      (KYC Initiation Request)
├── BypassRequestDto.java             (Admin Bypass Request)
├── OtpRequestDto.java                (OTP Request)
├── OtpVerificationDto.java           (OTP Verification Request)
├── LoginResponseDto.java             (Login Response with Token)
└── UserDto.java                      (User Profile DTO)
```

---

**Last Updated**: March 23, 2024  
**Status**: ✅ Production Ready
