# Centralized KYC System - Kubernetes Deployment Manifests

This directory (`deploy.k8s`) contains enterprise-standard Kubernetes manifests for deploying the entire **Centralized KYC System** microservices architecture.

---

## 1. Architecture & Port Mapping Reference

| Order | Service | Port | Base Image | Protocol / Role | Backing Infrastructure | Manifest File |
| :---: | :--- | :---: | :--- | :--- | :--- | :--- |
| **0** | **Namespace** | - | - | Core isolation (`kyc-system`) | - | `00-namespace.yaml` |
| **1** | **ConfigMap** | - | - | Centralized environment parameters | - | `01-configmap.yaml` |
| **2** | **Secret** | - | - | Secure keys & DB credentials | - | `02-secrets.yaml` |
| **3** | **Backing Infrastructure** | 3306, 27017, 6379, 9092, 2181 | Official Images | MySQL, MongoDB, Redis, Kafka, Zookeeper | PV / PVC | `03-infrastructure.yaml` |
| **4** | **Discovery Service** | `8761` | `eclipse-temurin:21-jre-alpine` | Spring Cloud Netflix Eureka Registry | - | `04-discovery-service.yaml` |
| **5** | **API Gateway** | `8083` | `eclipse-temurin:21-jre-alpine` | Reactive Gateway (LoadBalancer) | Redis, Kafka, Eureka | `05-api-gateway.yaml` |
| **6** | **User Service** | `8090` | `eclipse-temurin:21-jre-alpine` | Auth, OTP, User Lifecycle | MySQL (`users`), Redis, Kafka, Eureka | `06-user-service.yaml` |
| **7** | **KYC Management** | `8086` | `eclipse-temurin:21-jre-alpine` | Core KYC Aggregation & Lifecycle | MySQL (`kyc`), Redis, Kafka, Eureka | `07-kyc-management-service.yaml` |
| **8** | **Consent Service** | `8084` | `eclipse-temurin:21-jre-alpine` | Consent Lifecycles & Bank Sharing | MySQL (`kyc_consent_db`), Redis, Kafka, Eureka | `08-consent-service.yaml` |
| **9** | **Bank Service** | `8091` | `eclipse-temurin:21-jre-alpine` | Banking Onboarding & Ingestion | MongoDB (`kyc_db`), Kafka, Eureka | `09-bank-service.yaml` |
| **10** | **Digilocker Service** | `8088` | `eclipse-temurin:21-jre-alpine` | DigiLocker Proof Validation | Redis, Kafka, Eureka | `10-digilocker-service.yaml` |
| **11** | **Email Service** | `8092` | `eclipse-temurin:21-jre-alpine` | Asynchronous OTP / Alerts | Kafka, SMTP, Eureka | `11-email-service.yaml` |
| **12** | **Notification System** | `8087` | `eclipse-temurin:21-jre-alpine` | Multi-channel Alerts & WebSockets | MySQL (`kyc_notification_db`), Redis, Kafka, Eureka | `12-notification-system.yaml` |
| **13** | **Log Monitoring (SentientGate)** | `8085` | `eclipse-temurin:21-jre-alpine` | Traffic Profiling & Threat Detection | MySQL (`kyc`), Kafka, Eureka | `13-log-monitoring-system.yaml` |
| **14** | **AI Service** | `8089` | `eclipse-temurin:21-jre-alpine` | AI Verification & Fraud Detection | Groq API, Eureka | `14-ai-service.yaml` |

---

## 2. Dockerfile Analysis Across All Microservices

Each service utilizes a standardized container build configuration:
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE <PORT>
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Key Highlights & Container Optimizations:
1. **Base Image (`eclipse-temurin:21-jre-alpine`)**:
   - Ultra-compact Alpine footprint (~150MB - 200MB) keeping image pull latency minimal.
   - Eclipse Temurin OpenJDK 21 JRE runtime provides modern GC algorithms (ZGC, G1) and Project Loom Virtual Threads.
2. **Container Awareness & JVM Memory Tuning**:
   - The Kubernetes manifests inject `JAVA_TOOL_OPTIONS`:
     ```text
     -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom
     ```
   - This ensures the JVM dynamically respects pod cgroup memory limits rather than attempting to allocate host memory, avoiding `OOMKilled` pod restarts.
3. **Graceful Termination**:
   - The exec form `["java", "-jar", "app.jar"]` ensures the JVM runs as PID 1, allowing Kubernetes `SIGTERM` signals to propagate cleanly for zero-downtime rolling updates.

---

## 3. How to Build & Tag Images

Run the following commands from the project root (`Centralized-KYC-System-Backend`) or inside each individual service directory:

### Step 1: Package JARs
```powershell
# In PowerShell:
$services = @("AiService", "ApiGateway", "BankService", "ConsentService", "DigilockerService", "DiscoveryService", "EmailService", "KycManagementService", "LogMonitoringSystem", "NotificationSystem", "UserService")
foreach ($s in $services) {
    Set-Location "Backend\$s"
    ./mvnw clean package -DskipTests
    Set-Location "..\.."
}
```

### Step 2: Build and Tag Docker Images
```bash
docker build -t kyc-system/ai-service:latest Backend/AiService
docker build -t kyc-system/api-gateway:latest Backend/ApiGateway
docker build -t kyc-system/bank-service:latest Backend/BankService
docker build -t kyc-system/consent-service:latest Backend/ConsentService
docker build -t kyc-system/digilocker-service:latest Backend/DigilockerService
docker build -t kyc-system/discovery-service:latest Backend/DiscoveryService
docker build -t kyc-system/email-service:latest Backend/EmailService
docker build -t kyc-system/kyc-management-service:latest Backend/KycManagementService
docker build -t kyc-system/log-monitoring-system:latest Backend/LogMonitoringSystem
docker build -t kyc-system/notification-system:latest Backend/NotificationSystem
docker build -t kyc-system/user-service:latest Backend/UserService
```

> **Note for Minikube users:** Run `eval $(minikube docker-env)` or `minikube image load <image-name>` before running `docker build` so images are directly visible to your cluster.

---

## 4. Deploying to Kubernetes

### Option A: Using Kustomize (Recommended)
Deploy everything in the correct dependency order with a single command:
```bash
kubectl apply -k deploy.k8s
```

### Option B: Using Standard `kubectl apply -f`
```bash
# 1. Create Namespace
kubectl apply -f deploy.k8s/00-namespace.yaml

# 2. Apply ConfigMap and Secrets
kubectl apply -f deploy.k8s/01-configmap.yaml
kubectl apply -f deploy.k8s/02-secrets.yaml

# 3. Start Backing Infrastructure (Databases, Kafka, Redis)
kubectl apply -f deploy.k8s/03-infrastructure.yaml

# 4. Start Service Discovery
kubectl apply -f deploy.k8s/04-discovery-service.yaml

# 5. Start API Gateway & Business Microservices
kubectl apply -f deploy.k8s/05-api-gateway.yaml
kubectl apply -f deploy.k8s/06-user-service.yaml
kubectl apply -f deploy.k8s/07-kyc-management-service.yaml
kubectl apply -f deploy.k8s/08-consent-service.yaml
kubectl apply -f deploy.k8s/09-bank-service.yaml
kubectl apply -f deploy.k8s/10-digilocker-service.yaml
kubectl apply -f deploy.k8s/11-email-service.yaml
kubectl apply -f deploy.k8s/12-notification-system.yaml
kubectl apply -f deploy.k8s/13-log-monitoring-system.yaml
kubectl apply -f deploy.k8s/14-ai-service.yaml
```

---

## 5. Verification & Cluster Status

### Check Pod Status
```bash
kubectl get pods -n kyc-system
```

### Check Services & External Gateway IP
```bash
kubectl get svc -n kyc-system
```

### Port-Forwarding (Local Testing)
Access the API Gateway from your local machine:
```bash
kubectl port-forward svc/api-gateway 8083:8083 -n kyc-system
```
Access the Eureka Discovery Dashboard:
```bash
kubectl port-forward svc/discovery-service 8761:8761 -n kyc-system
```
Then navigate to: `http://localhost:8761` to view all microservice instances registered in Eureka.

### Stream Service Logs
```bash
kubectl logs -f deployment/api-gateway -n kyc-system
kubectl logs -f deployment/user-service -n kyc-system
kubectl logs -f deployment/kyc-management-service -n kyc-system
```

---

## 6. Teardown / Cleanup

To delete all resources created in the `kyc-system` namespace:
```bash
kubectl delete -k deploy.k8s
```
or
```bash
kubectl delete namespace kyc-system
```
