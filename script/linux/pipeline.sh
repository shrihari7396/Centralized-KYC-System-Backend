#!/usr/bin/env bash
# ==============================================================================
# Centralized KYC System - Linux Master End-to-End Pipeline
# Runs tests -> Builds Docker images -> Deploys to Kubernetes -> Checks Status
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
SKIP_TESTS=false
SKIP_BUILD=false
NO_WAIT=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-tests)
      SKIP_TESTS=true
      shift
      ;;
    --skip-build)
      SKIP_BUILD=true
      shift
      ;;
    --no-wait)
      NO_WAIT=true
      shift
      ;;
    --help|-h)
      echo "Usage: $0 [options]"
      echo "Options:"
      echo "  --skip-tests   Skip running microservice unit tests"
      echo "  --skip-build   Skip building JARs and Docker container images"
      echo "  --no-wait      Do not wait for pod rollouts to complete"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

echo -e "${CYAN}${BOLD}====================================================================${NC}"
echo -e "${CYAN}${BOLD}   Centralized KYC System - Master CI/CD Pipeline (Linux)           ${NC}"
echo -e "${CYAN}${BOLD}====================================================================${NC}"

# Stage 1: Tests
if [ "${SKIP_TESTS}" = false ]; then
  echo -e "\n${BLUE}${BOLD}[STAGE 1/3] Running Microservice Test Suites...${NC}"
  bash "${SCRIPT_DIR}/run-tests.sh"
else
  echo -e "\n${YELLOW}${BOLD}[STAGE 1/3] Skipped Test Execution.${NC}"
fi

# Stage 2: Build Images
if [ "${SKIP_BUILD}" = false ]; then
  echo -e "\n${BLUE}${BOLD}[STAGE 2/3] Building JARs and Docker Container Images...${NC}"
  bash "${SCRIPT_DIR}/build-images.sh"
else
  echo -e "\n${YELLOW}${BOLD}[STAGE 2/3] Skipped Container Image Building.${NC}"
fi

# Stage 3: Kubernetes Deploy
echo -e "\n${BLUE}${BOLD}[STAGE 3/3] Deploying Manifests to Kubernetes...${NC}"
DEPLOY_ARGS=()
if [ "${NO_WAIT}" = true ]; then
  DEPLOY_ARGS+=("--no-wait")
fi

bash "${SCRIPT_DIR}/deploy-k8s.sh" "${DEPLOY_ARGS[@]}"

# Final Status Check
echo -e "\n${BLUE}${BOLD}[POST-DEPLOY] Checking Cluster Runtime Health...${NC}"
bash "${SCRIPT_DIR}/status-k8s.sh"

echo -e "\n${GREEN}${BOLD}====================================================================${NC}"
echo -e "${GREEN}${BOLD}   ✓ COMPLETE PIPELINE EXECUTED SUCCESSFULLY!                      ${NC}"
echo -e "${GREEN}${BOLD}====================================================================${NC}"
