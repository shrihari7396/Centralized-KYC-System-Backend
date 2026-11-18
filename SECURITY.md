# Security Policy & Vulnerability Disclosure

## 1. Supported Versions

| Version | Status | Java Runtime | Spring Boot | Security Support |
| :--- | :--- | :--- | :--- | :--- |
| **1.0.x** | **Active Production** | **Java 21 LTS** | **3.4.2 LTS** | :white_check_mark: Maintained |
| < 1.0.0 | Unsupported (Legacy/Prototype) | Java 17 | Mixed (< 3.4.0) | :x: End of Life |

---

## 2. Platform Security Architecture

The Centralized KYC System enforces a **Zero-Trust Defense-in-Depth** model:
1. **Perimeter Security**: The [ApiGateway](file:///c:/Users/shrih/Desktop/Temp/Centralized-KYC-System-Backend/Backend/ApiGateway) terminates external TLS, enforces strict URL whitelisting, validates cryptographic JWT signatures, checks Redis token revocation blacklists, and applies IP rate-limiting.
2. **Zero-Trust Internal Service Mesh**: Downstream microservices never blindly trust ingress headers; each internal service independently parses and validates JWTs using injected `${jwt.secret}` HMAC-SHA keys.
3. **Data Protection & PII Encryption**: Sensitive database columns (`aadhaar_no`, `pan`, `phone`, `first_name`, `last_name`) are transparently encrypted at rest using AES-256-GCM with externally supplied keys via [StringCryptoConverter](file:///c:/Users/shrih/Desktop/Temp/Centralized-KYC-System-Backend/Backend/UserService/src/main/java/org/rbi/userservice/Security/StringCryptoConverter.java).
4. **Brute-Force & Lockout Protection**: OTP verification across User, Consent, and DigiLocker services enforces a 3-minute TTL and triggers a mandatory 15-minute Redis-backed lockout upon 3 consecutive failed attempts.
5. **No Raw Document Storage**: The platform adheres to strict data minimization principles; no raw biometric files or PDF scans are retained.
6. **Container & Pod Security Standards**: All Docker containers run as unprivileged `appuser` (UID 10001, GID 10001), drop all Linux kernel capabilities, and enforce Kubernetes Pod Security Admission (`restricted` profile).

For full technical specifications, review [docs/architecture/trust-boundaries.md](file:///c:/Users/shrih/Desktop/Temp/Centralized-KYC-System-Backend/docs/architecture/trust-boundaries.md).

---

## 3. Reporting a Security Vulnerability

If you identify a security vulnerability or credential exposure within this platform, **do not open a public GitHub issue**.

Please report vulnerabilities securely to the Centralized KYC Security Engineering Team:
* **Email:** `security@kyc.rbi.org.in`
* **PGP Key:** Available upon request or via institutional keyserver.
* **Encrypted Subject:** `[VULNERABILITY] Centralized-KYC: <Component Name>`

### Reporting Guidelines
Include the following details in your advisory:
1. Impacted microservice, endpoint, or configuration file.
2. Step-by-step reproduction instructions or proof-of-concept (PoC).
3. Estimated exploitability and threat impact (e.g., CVSS v3.1 vector).
4. Any proposed remediations.

### Response Timelines (SLA)
* **Initial Acknowledgment:** Within **24 hours**.
* **Triage & Severity Assessment:** Within **48 hours**.
* **Patch & Remediation Plan:** Within **72 hours** for Critical/High vulnerabilities.
* **Coordinated Disclosure:** Security advisories are published after patches have been verified in staging and rolled out to production.

---

## 4. Runbooks & Compliance References
* **Production Deployment Checklist:** [docs/runbooks/production-checklist.md](file:///c:/Users/shrih/Desktop/Temp/Centralized-KYC-System-Backend/docs/runbooks/production-checklist.md)
* **Forensic Audit & Remediation Log:** [docs/production-audit.md](file:///c:/Users/shrih/Desktop/Temp/Centralized-KYC-System-Backend/docs/production-audit.md)
* **API Error Contract (RFC 7807):** [docs/api-error-contract.md](file:///c:/Users/shrih/Desktop/Temp/Centralized-KYC-System-Backend/docs/api-error-contract.md)
