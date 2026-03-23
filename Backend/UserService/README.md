# UserService - API Documentation

This README documents all controller APIs in `UserService` and the DTOs used for request and response bodies.

## Base URL
```
http://localhost:8080/user
```

## Data Models (DTOs / Enums)

### RegisterDTO (Request)
```json
{
  "email": "user@example.com",
  "firstName": "Aarav",
  "lastName": "Sharma",
  "mobileNumber": "9876543210"
}
```

### OtpRequestDto (Request)
```json
{
  "firstName": "Aarav",
  "lastName": "Sharma",
  "email": "user@example.com"
}
```

### OtpVerificationDto (Request)
```json
{
  "username": "aarav",
  "firstName": "Aarav",
  "lastName": "Sharma",
  "email": "user@example.com",
  "role": "USER",
  "otp": "123456"
}
```

### LoginResponseDto (Response)
```json
{
  "firstName": "Aarav",
  "lastName": "Sharma",
  "email": "user@example.com",
  "role": "USER",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### UserDto (Response)
```json
{
  "userId": "user-123",
  "firstName": "Aarav",
  "lastName": "Sharma",
  "email": "user@example.com",
  "phone": "9876543210",
  "roles": ["USER"],
  "status": "ACTIVE"
}
```

### UserStatus (Enum)
```
KYC_PENDING
KYC_REJECTED
ACTIVE
```

### Role (Enum)
```
USER
```

## API Endpoints

### 1) Register User
**Endpoint**: `POST /register`

**Description**: Registers a new user with basic profile details.

**Request Body**: `RegisterDTO`

**Response**: `String` message

**Example Request**:
```bash
curl -X POST http://localhost:8080/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "firstName": "Aarav",
    "lastName": "Sharma",
    "mobileNumber": "9876543210"
  }'
```

**Example Response**:
```json
"User registered successfully ..."
```

**HTTP Status Codes**:
- `201 Created`: User registered successfully
- `400 Bad Request`: Registration failed

---

### 2) Request OTP
**Endpoint**: `POST /request-otp`

**Description**: Sends an OTP to the user's email if the user exists.

**Request Body**: `OtpRequestDto`

**Response**: `String` message

**Example Request**:
```bash
curl -X POST http://localhost:8080/user/request-otp \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Aarav",
    "lastName": "Sharma",
    "email": "user@example.com"
  }'
```

**Example Response**:
```json
"OTP Sent successfully ..."
```

**HTTP Status Codes**:
- `201 Created`: OTP sent
- `400 Bad Request`: OTP send failed
- `404 Not Found`: User not found (throws `UserNotFoundException`)

---

### 3) OTP Verification (Login)
**Endpoint**: `POST /otp-verification`

**Description**: Verifies the OTP and returns a login token.

**Request Body**: `OtpVerificationDto`

**Response**: `LoginResponseDto`

**Example Request**:
```bash
curl -X POST http://localhost:8080/user/otp-verification \
  -H "Content-Type: application/json" \
  -d '{
    "username": "aarav",
    "firstName": "Aarav",
    "lastName": "Sharma",
    "email": "user@example.com",
    "role": "USER",
    "otp": "123456"
  }'
```

**Example Response**:
```json
{
  "firstName": "Aarav",
  "lastName": "Sharma",
  "email": "user@example.com",
  "role": "USER",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**HTTP Status Codes**:
- `201 Created`: OTP verified and token issued
- `400 Bad Request`: Verification failed

---

### 4) Logout
**Endpoint**: `POST /logout`

**Description**: Logs out a user by invalidating their JWT token.

**Headers**:
- `Authorization: Bearer <jwt_token>`

**Response**: `String` message

**Example Request**:
```bash
curl -X POST http://localhost:8080/user/logout \
  -H "Authorization: Bearer <jwt_token>"
```

**Example Response**:
```json
"Logged out successfully"
```

**HTTP Status Codes**:
- `200 OK`: Logout success
- `400 Bad Request`: Invalid Authorization header
- `500 Internal Server Error`: Logout failed

---

### 5) Get User Details
**Endpoint**: `GET /details`

**Description**: Fetches user profile details by email.

**Query Parameters**:
- `email` (String)

**Response**: `UserDto`

**Example Request**:
```bash
curl -X GET "http://localhost:8080/user/details?email=user@example.com"
```

**Example Response**:
```json
{
  "userId": "user-123",
  "firstName": "Aarav",
  "lastName": "Sharma",
  "email": "user@example.com",
  "phone": "9876543210",
  "roles": ["USER"],
  "status": "ACTIVE"
}
```

**HTTP Status Codes**:
- `200 OK`: User details returned
- `404 Not Found`: User not found

---

### 6) Get KYC Status by Email
**Endpoint**: `GET /get-kyc-status/{email}`

**Description**: Returns the KYC status for a user by email.

**Path Parameters**:
- `email` (String)

**Response**: `UserStatus`

**Example Request**:
```bash
curl -X GET http://localhost:8080/user/get-kyc-status/user@example.com
```

**Example Response**:
```json
"ACTIVE"
```

**HTTP Status Codes**:
- `200 OK`: Status returned
- `404 Not Found`: User not found
