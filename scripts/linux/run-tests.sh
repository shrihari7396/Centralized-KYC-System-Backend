#!/usr/bin/env bash
# ==============================================================================
# Centralized KYC System - Linux Test Runner
# Runs unit/integration tests across all 11 Spring Boot microservices
# ==============================================================================

set -euo pipefail

# ANSI Color Codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Determine project root directory regardless of current working directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
BACKEND_DIR="${ROOT_DIR}/Backend"

echo -e "${CYAN}${BOLD}====================================================================${NC}"
echo -e "${CYAN}${BOLD}     Centralized KYC System - Microservice Test Suite (Linux)       ${NC}"
echo -e "${CYAN}${BOLD}====================================================================${NC}"
echo -e "Project Root: ${ROOT_DIR}\n"

# Array of all 11 backend microservices
ALL_SERVICES=(
  "AiService"
  "ApiGateway"
  "BankService"
  "ConsentService"
  "DigilockerService"
  "DiscoveryService"
  "EmailService"
  "KycManagementService"
  "LogMonitoringSystem"
  "NotificationSystem"
  "UserService"
)

# Allow selecting a single service via CLI argument
if [ $# -ge 1 ] && [ -n "$1" ]; then
  TARGET_SERVICES=("$1")
  echo -e "${YELLOW}Targeting specific service:${NC} ${BOLD}$1${NC}\n"
else
  TARGET_SERVICES=("${ALL_SERVICES[@]}")
  echo -e "${BLUE}Running test suites for all ${#ALL_SERVICES[@]} microservices...${NC}\n"
fi

PASSED_SERVICES=()
FAILED_SERVICES=()
SKIPPED_SERVICES=()

START_TIME=$(date +%s)

for SERVICE in "${TARGET_SERVICES[@]}"; do
  SERVICE_DIR="${BACKEND_DIR}/${SERVICE}"
  
  if [ ! -d "${SERVICE_DIR}" ]; then
    echo -e "${RED}[ERROR] Service directory not found: ${SERVICE_DIR}${NC}"
    SKIPPED_SERVICES+=("${SERVICE}")
    continue
  fi

  echo -e "${BLUE}${BOLD}>>> [TESTING] ${SERVICE}...${NC}"
  cd "${SERVICE_DIR}"

  # Ensure wrapper is executable
  if [ -f "./mvnw" ]; then
    chmod +x ./mvnw
    MVN_CMD="./mvnw"
  else
    MVN_CMD="mvn"
  fi

  # Execute tests with Maven
  if ${MVN_CMD} test; then
    echo -e "${GREEN}${BOLD}✓ [PASSED] ${SERVICE}${NC}\n"
    PASSED_SERVICES+=("${SERVICE}")
  else
    echo -e "${RED}${BOLD}✗ [FAILED] ${SERVICE}${NC}\n"
    FAILED_SERVICES+=("${SERVICE}")
  fi
done

END_TIME=$(date +%s)
ELAPSED=$((END_TIME - START_TIME))

# Display Final Summary
echo -e "\n${CYAN}${BOLD}====================================================================${NC}"
echo -e "${CYAN}${BOLD}                         TEST SUMMARY                               ${NC}"
echo -e "${CYAN}${BOLD}====================================================================${NC}"
echo -e "Total Time Elapsed : ${ELAPSED} seconds"
echo -e "Total Executed     : ${#TARGET_SERVICES[@]}"
echo -e "Passed             : ${GREEN}${BOLD}${#PASSED_SERVICES[@]}${NC}"
echo -e "Failed             : ${RED}${BOLD}${#FAILED_SERVICES[@]}${NC}"

if [ ${#SKIPPED_SERVICES[@]} -gt 0 ]; then
  echo -e "Skipped/Not Found  : ${YELLOW}${#SKIPPED_SERVICES[@]}${NC}"
fi

echo -e "--------------------------------------------------------------------"
if [ ${#FAILED_SERVICES[@]} -eq 0 ]; then
  echo -e "${GREEN}${BOLD}✓ ALL TEST SUITES PASSED SUCCESSFULLY!${NC}"
  exit 0
else
  echo -e "${RED}${BOLD}✗ FAILURES DETECTED IN:${NC}"
  for FAIL in "${FAILED_SERVICES[@]}"; do
    echo -e "  - ${RED}${FAIL}${NC}"
  done
  exit 1
fi
