#!/usr/bin/env bash
# ==============================================================================
# Centralized KYC System - macOS Image Builder
# Packages JARs and builds Docker images for all 11 microservices
# Compatible with macOS default Bash 3.2, Zsh, Apple Silicon (M1/M2/M3/M4) & Intel
# ==============================================================================

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
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
PLATFORM=""

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
    --platform)
      PLATFORM="--platform $2"
      shift 2
      ;;
    --help|-h)
      echo "Usage: $0 [options] [service_name]"
      echo "Options:"
      echo "  --skip-package        Skip Maven clean package step (build Docker images only)"
      echo "  --prefix <name>       Docker image prefix/repository (default: kyc-system)"
      echo "  --tag <version>       Docker image tag (default: latest)"
      echo "  --platform <arch>     Target architecture (e.g., linux/amd64 or linux/arm64)"
      exit 0
      ;;
    *)
      TARGET_SERVICE="$1"
      shift
      ;;
  esac
done

echo -e "${CYAN}${BOLD}====================================================================${NC}"
echo -e "${CYAN}${BOLD}     Centralized KYC System - Docker Image Builder (macOS)          ${NC}"
echo -e "${CYAN}${BOLD}====================================================================${NC}"
echo -e "Project Root   : ${ROOT_DIR}"
echo -e "Image Tag      : ${IMAGE_PREFIX}/<service-name>:${IMAGE_TAG}"
if [ -n "${PLATFORM}" ]; then
  echo -e "Platform       : ${PLATFORM}"
fi
echo -e "Skip Packaging : ${SKIP_PACKAGE}\n"

# Verify Docker daemon
if ! docker info >/dev/null 2>&1; then
  echo -e "${RED}[ERROR] Docker daemon is not running.${NC}"
  echo -e "Please launch Docker Desktop, Colima, or OrbStack and verify with: 'docker ps'"
  exit 1
fi

# POSIX / Bash 3.2 compatible lookup function
get_image_name() {
  case "$1" in
    AiService) echo "ai-service" ;;
    ApiGateway) echo "api-gateway" ;;
    BankService) echo "bank-service" ;;
    ConsentService) echo "consent-service" ;;
    DigilockerService) echo "digilocker-service" ;;
    DiscoveryService) echo "discovery-service" ;;
    EmailService) echo "email-service" ;;
    KycManagementService) echo "kyc-management-service" ;;
    LogMonitoringSystem) echo "log-monitoring-system" ;;
    NotificationSystem) echo "notification-system" ;;
    UserService) echo "user-service" ;;
    *) echo "" ;;
  esac
}

ALL_SERVICES=(
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

if [ -n "${TARGET_SERVICE}" ]; then
  IMAGE_NAME=$(get_image_name "${TARGET_SERVICE}")
  if [ -z "${IMAGE_NAME}" ]; then
    echo -e "${RED}[ERROR] Unknown service name: '${TARGET_SERVICE}'${NC}"
    echo "Available services: ${ALL_SERVICES[*]}"
    exit 1
  fi
  SELECTED_SERVICES=("${TARGET_SERVICE}")
else
  SELECTED_SERVICES=("${ALL_SERVICES[@]}")
fi

BUILT_IMAGES=()
FAILED_BUILDS=()
START_TIME=$(date +%s)

for SERVICE in "${SELECTED_SERVICES[@]}"; do
  SERVICE_DIR="${BACKEND_DIR}/${SERVICE}"
  IMAGE_NAME=$(get_image_name "${SERVICE}")
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

  echo -e "${CYAN}Step 2/2: Building Docker image ${FULL_IMAGE}...${NC}"
  DOCKER_BUILD_CMD="docker build"
  if [ -n "${PLATFORM}" ]; then
    DOCKER_BUILD_CMD="${DOCKER_BUILD_CMD} ${PLATFORM}"
  fi
  DOCKER_BUILD_CMD="${DOCKER_BUILD_CMD} -t ${FULL_IMAGE} ."

  if eval "${DOCKER_BUILD_CMD}"; then
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

echo -e "\n${GREEN}${BOLD}✓ ALL REQUESTED IMAGES BUILT SUCCESSFULLY!${NC}"
