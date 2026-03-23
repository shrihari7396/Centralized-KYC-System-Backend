# KycManagementService - API Documentation

This README documents all controller APIs in `KycManagementService` and the DTOs used for request and response bodies.

## Base URL
```
http://localhost:8086/api/kyc
```

## Data Models (DTOs / Entities)

### DigilockerDataRequest (Request)
```json
{
  "userId": "user-123",
  "aadhaarNo": "123456789012",
  "pan": "ABCDE1234F",
  "drivingLicense": "DL-0420110149646",
  "address": {
    "street": "12 MG Road",
    "city": "Bengaluru",
    "state": "Karnataka",
    "zipCode": "560001",
    "country": "India"
  }
}
```

### Address (Embedded)
```json
{
  "street": "12 MG Road",
  "city": "Bengaluru",
  "state": "Karnataka",
  "zipCode": "560001",
  "country": "India"
}
```

### KycEntity (Response)
```json
{
  "userId": "user-123",
  "aadhaarNo": "123456789012",
  "pan": "ABCDE1234F",
  "drivingLicense": "DL-0420110149646",
  "address": {
    "street": "12 MG Road",
    "city": "Bengaluru",
    "state": "Karnataka",
    "zipCode": "560001",
    "country": "India"
  },
  "status": "VERIFIED"
}
```

### UserProfileResponseDto (Response)
```json
{
  "status": "VERIFIED",
  "identity": {
    "fullName": "Aarav Sharma",
    "dob": "1992-08-15",
    "gender": "MALE"
  },
  "address": {
    "street": "12 MG Road",
    "city": "Bengaluru",
    "state": "Karnataka",
    "zipCode": "560001",
    "country": "India"
  }
}
```

### VerifiedIdentity (Embedded)
```json
{
  "fullName": "Aarav Sharma",
  "dob": "1992-08-15",
  "gender": "MALE"
}
```

### DocumentProofResponseDto (Response)
```json
{
  "documentType": "PAN",
  "issuer": "Income Tax Department",
  "maskedId": "ABCDE1234F",
  "verifiedAt": "2026-03-23T10:30:00"
}
```

### KycTokenResponseDto (Response)
```json
{
  "tokenId": "token-001",
  "allowedData": ["PAN", "AADHAAR"],
  "issuedAt": "2026-03-23T10:30:00",
  "expiresAt": "2026-04-22T10:30:00",
  "status": "ACTIVE"
}
```

### Map<String, Object> (Dynamic Response)
Used by `GET /bank/documents`. This response is dynamic and assembled from verified data. Example:
```json
{
  "identity": {
    "fullName": "Aarav Sharma",
    "dob": "1992-08-15",
    "gender": "MALE",
    "pan": "ABCDE1234F"
  },
  "documents": [
    {
      "type": "AADHAAR",
      "maskedId": "XXXX-XXXX-9012",
      "verifiedAt": "2026-03-23T10:30:00"
    }
  ],
  "status": "VERIFIED"
}
```

## API Endpoints

### 1) Store Digilocker Data
**Endpoint**: `POST /digilocker`

**Description**: Stores verified Digilocker data for a user.

**Request Body**: `DigilockerDataRequest`

**Response**: `KycEntity`

**Example Request**:
```bash
curl -X POST http://localhost:8086/api/kyc/digilocker \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "aadhaarNo": "123456789012",
    "pan": "ABCDE1234F",
    "drivingLicense": "DL-0420110149646",
    "address": {
      "street": "12 MG Road",
      "city": "Bengaluru",
      "state": "Karnataka",
      "zipCode": "560001",
      "country": "India"
    }
  }'
```

**Example Response**:
```json
{
  "userId": "user-123",
  "aadhaarNo": "123456789012",
  "pan": "ABCDE1234F",
  "drivingLicense": "DL-0420110149646",
  "address": {
    "street": "12 MG Road",
    "city": "Bengaluru",
    "state": "Karnataka",
    "zipCode": "560001",
    "country": "India"
  },
  "status": "VERIFIED"
}
```

**HTTP Status Codes**:
- `200 OK`: Data stored successfully
- `500 Internal Server Error`: Server error occurred

---

### 2) Fetch Verified Documents for Bank
**Endpoint**: `GET /bank/documents`

**Description**: Returns verified user data for a bank based on the JWT token details.

**Headers**:
- `Authorization: Bearer <jwt_token>`

**Response**: `Map<String, Object>` (dynamic)

**Example Request**:
```bash
curl -X GET http://localhost:8086/api/kyc/bank/documents \
  -H "Authorization: Bearer <jwt_token>"
```

**Example Response**:
```json
{
  "identity": {
    "fullName": "Aarav Sharma",
    "dob": "1992-08-15",
    "gender": "MALE",
    "pan": "ABCDE1234F"
  },
  "documents": [
    {
      "type": "AADHAAR",
      "maskedId": "XXXX-XXXX-9012",
      "verifiedAt": "2026-03-23T10:30:00"
    }
  ],
  "status": "VERIFIED"
}
```

**HTTP Status Codes**:
- `200 OK`: Data retrieved successfully
- `400 Bad Request`: Invalid token or request context
- `403 Forbidden`: Access denied
- `500 Internal Server Error`: Server error occurred

---

### 3) Get User Profile (Transparency Dashboard)
**Endpoint**: `GET /{userId}`

**Description**: Returns a summarized KYC profile for a user.

**Path Parameters**:
- `userId` (String)

**Response**: `UserProfileResponseDto`

**Example Request**:
```bash
curl -X GET http://localhost:8086/api/kyc/user-123
```

**Example Response**:
```json
{
  "status": "VERIFIED",
  "identity": {
    "fullName": "Aarav Sharma",
    "dob": "1992-08-15",
    "gender": "MALE"
  },
  "address": {
    "street": "12 MG Road",
    "city": "Bengaluru",
    "state": "Karnataka",
    "zipCode": "560001",
    "country": "India"
  }
}
```

**HTTP Status Codes**:
- `200 OK`: Profile found
- `404 Not Found`: User profile not found

---

### 4) Get Document Proofs (Transparency Dashboard)
**Endpoint**: `GET /{userId}/documents`

**Description**: Returns a list of verified document proofs for a user.

**Path Parameters**:
- `userId` (String)

**Response**: `List<DocumentProofResponseDto>`

**Example Request**:
```bash
curl -X GET http://localhost:8086/api/kyc/user-123/documents
```

**Example Response**:
```json
[
  {
    "documentType": "PAN",
    "issuer": "Income Tax Department",
    "maskedId": "ABCDE1234F",
    "verifiedAt": "2026-03-23T10:30:00"
  }
]
```

**HTTP Status Codes**:
- `200 OK`: Documents found
- `404 Not Found`: No documents found for user

---

### 5) Get KYC Token (Transparency Dashboard)
**Endpoint**: `GET /{userId}/token`

**Description**: Returns the current KYC token details for a user.

**Path Parameters**:
- `userId` (String)

**Response**: `KycTokenResponseDto`

**Example Request**:
```bash
curl -X GET http://localhost:8086/api/kyc/user-123/token
```

**Example Response**:
```json
{
  "tokenId": "token-001",
  "allowedData": ["PAN", "AADHAAR"],
  "issuedAt": "2026-03-23T10:30:00",
  "expiresAt": "2026-04-22T10:30:00",
  "status": "ACTIVE"
}
```

**HTTP Status Codes**:
- `200 OK`: Token retrieved
- `500 Internal Server Error`: Server error occurred

---

### 6) Revoke KYC Token (Transparency Dashboard)
**Endpoint**: `POST /{userId}/token/revoke`

**Description**: Revokes a user's KYC token.

**Path Parameters**:
- `userId` (String)

**Request Body**: None

**Response**: Empty body

**Example Request**:
```bash
curl -X POST http://localhost:8086/api/kyc/user-123/token/revoke
```

**HTTP Status Codes**:
- `200 OK`: Token revoked
- `500 Internal Server Error`: Server error occurred
