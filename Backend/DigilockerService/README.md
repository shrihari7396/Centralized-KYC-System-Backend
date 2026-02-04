# Mock DigiLocker Service

This is a **Mock DigiLocker Service** built to simulate DigiLocker APIs for development and testing of the KYC Management System. It generates synthetic user data and documents at runtime without connecting to real government servers.

## 🚀 How to Run

1. Navigate to the service directory:
   ```bash
   cd Backend/DigilockerService
   ```
2. Start the service using Maven Wrapper:
   ```bash
   ./mvnw spring-boot:run
   ```
   
The service will start on **Port 8083** (default).
- Base URL: `http://localhost:8083`

## 🔗 APIs

| Method | Endpoint | Description |
|Args|---|---|
| POST | `/digilocker/consent` | Grant consent and receive a User/Consent ID. |
| GET | `/digilocker/user/{id}` | Get User Profile (Name, Address, generic Aadhaar). |
| GET | `/digilocker/documents/{id}` | Get Documents (Aadhaar, PAN, DL) with Token Hash. |

---

## 🛠️ How to Integrate (Step-by-Step)

Follow these steps to consume this service from your **KycManagementService** or any other microservice.

### Step 1: Add Configuration
Add the mock service base URL to your application properties (`application.properties` or `application.yml`).

```yaml
integration:
  digilocker:
    url: http://localhost:8083/digilocker
```

### Step 2: Create DTOs
Create the necessary Data Transfer Objects in your project to map the mock responses.

**ConsentResponse.java**
```java
public class ConsentResponse {
    private String consentId;
    private String status;
    // getters and setters
}
```

**DigilockerUser.java**
```java
public class DigilockerUser {
    private String digilockerUserId;
    private String name;
    private String dob;
    private String gender;
    private String aadhaar;
    // getters and setters
}
```

**DigilockerDocument.java**
```java
public class DigilockerDocument {
    private String type;
    private String content; // Base64
    private String documentHash; // SHA-256 for Tokenisation
    // getters and setters
}
```

**DocumentResponse.java**
```java
public class DocumentResponse {
    private List<DigilockerDocument> documents;
    // getters and setters
}
```

### Step 3: Implement the Integration Service
Use `RestTemplate` or `WebClient` to call the mock APIs.

```java
@Service
public class DigiLockerIntegrationService {

    @Value("${integration.digilocker.url}")
    private String digilockerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public void processKyc(String requestorId) {
        // 1. Request Consent
        Map<String, String> payload = Map.of("purpose", "KYC", "requestedBy", requestorId);
        ConsentResponse consent = restTemplate.postForObject(digilockerUrl + "/consent", payload, ConsentResponse.class);
        
        if (consent != null && "GRANTED".equals(consent.getStatus())) {
            String userId = consent.getConsentId();
            
            // 2. Fetch User Profile
            DigilockerUser user = restTemplate.getForObject(digilockerUrl + "/user/" + userId, DigilockerUser.class);
            System.out.println("Verified User: " + user.getName());
            
            // 3. Fetch Documents
            DocumentResponse docs = restTemplate.getForObject(digilockerUrl + "/documents/" + userId, DocumentResponse.class);
            
            // 4. Process Documents (e.g. Tokenise)
            docs.getDocuments().forEach(doc -> {
                System.out.println("Processing " + doc.getType() + " with Hash: " + doc.getDocumentHash());
                // TODO: Send doc.getDocumentHash() to Web3/Smart Contract
            });
        }
    }
}
```

## 🔐 Notes
- **Security**: The mock endpoints allow anonymous access (`permitAll`) so you do not need to implement OAuth flows for this mock.
- **Data**: Every time you hit the APIs, new random data is generated.
