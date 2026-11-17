#!/usr/bin/env bash
# ==============================================================================
# Centralized KYC System - Linux Image Builder
# Packages JARs and builds Docker images for all 11 microservices
# ==============================================================================

set -euo pipefail

# ANSI Color Codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\133[1;33m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
BACKEND_DIR="${ROOT_DIR}/Backend"

IMAGE_PREFIX="kyc-system"
IMAGE_TAG="latest"
SKIP_PACKAGE=false
TARGET_SERVICE=""

# Parse flags
while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-package)
      SKIP_PACKAGE=true
      shift
      ;;
    --tag)
      IMAGE_TAG="$2"
      shift 2
      ;;
    --prefix)
      IMAGE_PREFIX="$2"
      shift 2
      ;;
    --help|-h)
      echo "Usage: $0 [options] [service_name]"
      echo "Options:"
      echo "  --skip-package        Skip Maven clean package step (build Docker images only)"
      echo "  --prefix <name>       Docker image prefix/repository (default: kyc-system)"
      echo "  --tag <version>       Docker image tag (default: latest)"
      exit 0
      ;;
    *)
      TARGET_SERVICE="$1"
      shift
      ;;
  esac
done

echo -e "${CYAN}${BOLD}====================================================================${NC}"
echo -e "${CYAN}${BOLD}     Centralized KYC System - Docker Image Builder (Linux)          ${NC}"
echo -e "${CYAN}${BOLD}====================================================================${NC}"
echo -e "Project Root   : ${ROOT_DIR}"
echo -e "Image Tag      : ${IMAGE_PREFIX}/<service-name>:${IMAGE_TAG}"
echo -e "Skip Packaging : ${SKIP_PACKAGE}\n"

# Check Docker daemon availability
if ! docker info >/dev/null 2>&1; then
  echo -e "${RED}[ERROR] Docker daemon is not running or current user lacks docker permissions.${NC}"
  echo -e "Please start Docker and verify with: 'docker ps'"
  exit 1
fi

# Service Directory Name -> Docker Image Name mapping
declare -A SERVICE_MAP=(
  ["AiService"]="ai-service"
  ["ApiGateway"]="api-gateway"
  ["BankService"]="bank-service"
  ["ConsentService"]="consent-service"
  ["DigilockerService"]="digilocker-service"
  ["DiscoveryService"]="discovery-service"
  ["EmailService"]="email-service"
  ["KycManagementService"]="kyc-management-service"
  ["LogMonitoringSystem"]="log-monitoring-system"
  ["NotificationSystem"]="notification-system"
  ["UserService"]="user-service"
)

# Determine services to build
if [ -n "${TARGET_SERVICE}" ]; then
  if [[ -z "${SERVICE_MAP[${TARGET_SERVICE}]+isset}" ]]; then
    echo -e "${RED}[ERROR] Unknown service name: '${TARGET_SERVICE}'${NC}"
    echo "Available services: ${!SERVICE_MAP[*]}"
    exit 1
  fi
  SELECTED_SERVICES=("${TARGET_SERVICE}")
else
  SELECTED_SERVICES=(
    "DiscoveryService"
    "ApiGateway"
    "UserService"
    "KycManagementService"
    "ConsentService"
    "BankService"
    "DigilockerService"
    "EmailService"
    "NotificationSystem"
    "LogMonitoringSystem"
    "AiService"
  )
fi

BUILT_IMAGES=()
FAILED_BUILDS=()
START_TIME=$(date +%s)

for SERVICE in "${SELECTED_SERVICES[@]}"; do
  SERVICE_DIR="${BACKEND_DIR}/${SERVICE}"
  IMAGE_NAME="${SERVICE_MAP[${SERVICE}]}"
  FULL_IMAGE="${IMAGE_PREFIX}/${IMAGE_NAME}:${IMAGE_TAG}"

  echo -e "\n${BLUE}${BOLD}====================================================================${NC}"
  echo -e "${BLUE}${BOLD}>>> Building: ${SERVICE} -> ${FULL_IMAGE}${NC}"
  echo -e "${BLUE}${BOLD}====================================================================${NC}"

  if [ ! -d "${SERVICE_DIR}" ]; then
    echo -e "${RED}[ERROR] Directory not found: ${SERVICE_DIR}${NC}"
    FAILED_BUILDS+=("${SERVICE}")
    continue
  fi

  cd "${SERVICE_DIR}"

  # Step 1: Package JAR with Maven (if not skipped)
  if [ "${SKIP_PACKAGE}" = false ]; then
    echo -e "${CYAN}Step 1/2: Compiling & packaging fat JAR...${NC}"
    if [ -f "./mvnw" ]; then
      chmod +x ./mvnw
      MVN_CMD="./mvnw"
    else
      MVN_CMD="mvn"
    fi

    if ${MVN_CMD} clean package -DskipTests; then
      echo -e "${GREEN}✓ JAR package successful.${NC}"
    else
      echo -e "${RED}✗ Maven build failed for ${SERVICE}.${NC}"
      FAILED_BUILDS+=("${SERVICE}")
      continue
    fi
  else
    echo -e "${YELLOW}Step 1/2: Skipped packaging (using existing target/*.jar).${NC}"
  fi

  # Step 2: Build Docker container image
  echo -e "${CYAN}Step 2/2: Building Docker image ${FULL_IMAGE}...${NC}"
  if docker build -t "${FULL_IMAGE}" .; then
    echo -e "${GREEN}✓ Successfully built Docker image: ${FULL_IMAGE}${NC}"
    BUILT_IMAGES+=("${FULL_IMAGE}")
  else
    echo -e "${RED}✗ Docker build failed for ${FULL_IMAGE}.${NC}"
    FAILED_BUILDS+=("${SERVICE}")
  fi
done

END_TIME=$(date +%s)
ELAPSED=$((END_TIME - START_TIME))

echo -e "\n${CYAN}${BOLD}====================================================================${NC}"
echo -e "${CYAN}${BOLD}                      BUILD SUMMARY                                 ${NC}"
echo -e "${CYAN}${BOLD}====================================================================${NC}"
echo -e "Total Time Elapsed : ${ELAPSED} seconds"
echo -e "Images Built       : ${GREEN}${BOLD}${#BUILT_IMAGES[@]}${NC}"
echo -e "Build Failures     : ${RED}${BOLD}${#FAILED_BUILDS[@]}${NC}"

if [ ${#BUILT_IMAGES[@]} -gt 0 ]; then
  echo -e "\n${GREEN}Built Images List:${NC}"
  for IMG in "${BUILT_IMAGES[@]}"; do
    echo -e "  - ${IMG}"
  done
fi

if [ ${#FAILED_BUILDS[@]} -gt 0 ]; then
  echo -e "\n${RED}Failed Services:${NC}"
  for FAIL in "${FAILED_BUILDS[@]}"; do
    echo -e "  - ${FAIL}"
  done
  exit 1
fi

# Detect Minikube or Kind and provide guidance
if command -v minikube >/dev/null 2>&1 && minikube status >/dev/null 2>&1; then
  echo -e "\n${YELLOW}${BOLD}[NOTICE] Minikube is active.${NC}"
  echo "If your pods cannot pull local images, run:"
  echo "  eval \$(minikube docker-env)"
  echo "and rebuild, or load them via:"
  echo "  for img in \$(docker images 'kyc-system/*' --format '{{.Repository}}:{{.Tag}}'); do minikube image load \$img; done"
fi

echo -e "\n${GREEN}${BOLD}✓ ALL REQUESTED IMAGES BUILT SUCCESSFULLY!${NC}"
