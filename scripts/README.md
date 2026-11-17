# Centralized KYC System - Cross-Platform Automation Scripts

This folder contains automated operations scripts organized by Operating System:
- **`linux/`**: Bash scripts (`.sh`) optimized for GNU/Linux environments.
- **`windows/`**: PowerShell scripts (`.ps1`) for Windows environments.
- **`mac/`**: Bash/Zsh scripts (`.sh`) tailored for macOS (compatible with Apple Silicon M1/M2/M3/M4 & Intel, Bash 3.2+).

---

## Script Inventory

| Operation | Linux Script | Windows Script | macOS Script | Description |
| :--- | :--- | :--- | :--- | :--- |
| **Run Tests** | `linux/run-tests.sh` | `windows/run-tests.ps1` | `mac/run-tests.sh` | Runs unit & integration test suites for all 11 microservices (or a selected one). |
| **Build Images** | `linux/build-images.sh` | `windows/build-images.ps1` | `mac/build-images.sh` | Compiles fat JARs (`./mvnw clean package -DskipTests`) and builds Docker images. |
| **Deploy K8s** | `linux/deploy-k8s.sh` | `windows/deploy-k8s.ps1` | `mac/deploy-k8s.sh` | Deploys namespace, configs, secrets, databases, Kafka, and all services to Kubernetes. |
| **Cluster Status** | `linux/status-k8s.sh` | `windows/status-k8s.ps1` | `mac/status-k8s.sh` | Displays real-time pod health, deployment status, and service endpoints. |
| **Teardown K8s** | `linux/teardown-k8s.sh` | `windows/teardown-k8s.ps1` | `mac/teardown-k8s.sh` | Deletes the `kyc-system` deployment and namespace cleanly. |
| **Master Pipeline** | `linux/pipeline.sh` | `windows/pipeline.ps1` | `mac/pipeline.sh` | Complete end-to-end execution: Test -> Build -> Deploy -> Health Check. |

---

## 1. Linux Usage

Run these from the repository root:

```bash
# Make executable
chmod +x scripts/linux/*.sh

# Run all microservice tests
./scripts/linux/run-tests.sh

# Run tests for a specific service
./scripts/linux/run-tests.sh UserService

# Build all Docker images
./scripts/linux/build-images.sh

# Build a single Docker image (e.g., API Gateway)
./scripts/linux/build-images.sh ApiGateway

# Deploy to Kubernetes (waits for all pods to become ready)
./scripts/linux/deploy-k8s.sh

# Check cluster runtime status
./scripts/linux/status-k8s.sh

# Teardown deployment
./scripts/linux/teardown-k8s.sh

# Run end-to-end automated pipeline
./scripts/linux/pipeline.sh
```

---

## 2. Windows Usage (PowerShell)

Run these from PowerShell in the repository root:

```powershell
# Run all microservice tests
.\scripts\windows\run-tests.ps1

# Run tests for a specific service
.\scripts\windows\run-tests.ps1 -Service UserService

# Build all Docker images
.\scripts\windows\build-images.ps1

# Build without re-packaging existing JARs
.\scripts\windows\build-images.ps1 -SkipPackage

# Deploy to Kubernetes
.\scripts\windows\deploy-k8s.ps1

# Check cluster runtime status
.\scripts\windows\status-k8s.ps1

# Teardown deployment
.\scripts\windows\teardown-k8s.ps1

# Run end-to-end automated pipeline
.\scripts\windows\pipeline.ps1
```

---

## 3. macOS Usage

Run these from Terminal in the repository root:

```bash
# Make executable
chmod +x scripts/mac/*.sh

# Run all microservice tests
./scripts/mac/run-tests.sh

# Build all Docker images (supports optional --platform flag)
./scripts/mac/build-images.sh --platform linux/amd64

# Deploy to Kubernetes
./scripts/mac/deploy-k8s.sh

# Check cluster runtime status
./scripts/mac/status-k8s.sh

# Teardown deployment
./scripts/mac/teardown-k8s.sh

# Run end-to-end automated pipeline
./scripts/mac/pipeline.sh
```
