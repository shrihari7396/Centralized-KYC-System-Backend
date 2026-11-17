#!/usr/bin/env bash
# ==============================================================================
# Centralized KYC System - macOS Kubernetes Deployer
# Deploys namespace, configs, secrets, infrastructure, and microservices to K8s
# Compatible with macOS default Bash 3.2 and Zsh
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
DEPLOY_DIR="${ROOT_DIR}/deploy.k8s"
NAMESPACE="kyc-system"
WAIT_ROLLOUT=true

while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-wait)
      WAIT_ROLLOUT=false
      shift
      ;;
    --namespace|-n)
      NAMESPACE="$2"
      shift 2
      ;;
    --help|-h)
      echo "Usage: $0 [options]"
      echo "Options:"
      echo "  --no-wait             Do not wait for pod rollouts to complete"
      echo "  --namespace <name>    Target namespace (default: kyc-system)"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

echo -e "${CYAN}${BOLD}====================================================================${NC}"
echo -e "${CYAN}${BOLD}     Centralized KYC System - Kubernetes Deployment (macOS)         ${NC}"
echo -e "${CYAN}${BOLD}====================================================================${NC}"
echo -e "Project Root : ${ROOT_DIR}"
echo -e "Manifest Dir : ${DEPLOY_DIR}"
echo -e "Namespace    : ${NAMESPACE}\n"

if ! command -v kubectl >/dev/null 2>&1; then
  echo -e "${RED}[ERROR] 'kubectl' is not installed or not in system PATH.${NC}"
  echo "Install with Homebrew: brew install kubectl"
  exit 1
fi

echo -e "${BLUE}Checking Kubernetes cluster connectivity...${NC}"
if ! kubectl cluster-info >/dev/null 2>&1; then
  echo -e "${RED}[ERROR] Cannot connect to Kubernetes cluster.${NC}"
  echo "Verify active context with: 'kubectl config current-context'"
  exit 1
fi
echo -e "${GREEN}✓ Connected to cluster: $(kubectl config current-context)${NC}\n"

echo -e "${CYAN}${BOLD}Applying manifests to Kubernetes...${NC}"
if kubectl apply -k "${DEPLOY_DIR}"; then
  echo -e "${GREEN}✓ Successfully applied manifests via Kustomize.${NC}\n"
else
  echo -e "${YELLOW}Kustomize apply encountered an issue. Falling back to ordered file apply...${NC}"
  
  FILES=(
    "00-namespace.yaml"
    "01-configmap.yaml"
    "02-secrets.yaml"
    "03-infrastructure.yaml"
    "04-discovery-service.yaml"
    "05-api-gateway.yaml"
    "06-user-service.yaml"
    "07-kyc-management-service.yaml"
    "08-consent-service.yaml"
    "09-bank-service.yaml"
    "10-digilocker-service.yaml"
    "11-email-service.yaml"
    "12-notification-system.yaml"
    "13-log-monitoring-system.yaml"
    "14-ai-service.yaml"
  )

  for FILE in "${FILES[@]}"; do
    FILEPATH="${DEPLOY_DIR}/${FILE}"
    if [ -f "${FILEPATH}" ]; then
      echo -e "${BLUE}Applying ${FILE}...${NC}"
      kubectl apply -f "${FILEPATH}"
    fi
  done
  echo -e "${GREEN}✓ Finished sequential apply.${NC}\n"
fi

if [ "${WAIT_ROLLOUT}" = true ]; then
  echo -e "${CYAN}${BOLD}Waiting for core infrastructure to become ready...${NC}"
  
  CORE_DEPLOYS=("mysql" "mongodb" "redis" "zookeeper" "kafka")
  for DEP in "${CORE_DEPLOYS[@]}"; do
    echo -e "${BLUE}Waiting for deployment/${DEP}...${NC}"
    kubectl rollout status "deployment/${DEP}" -n "${NAMESPACE}" --timeout=180s || true
  done

  echo -e "\n${CYAN}${BOLD}Waiting for service discovery & gateway...${NC}"
  kubectl rollout status deployment/discovery-service -n "${NAMESPACE}" --timeout=180s || true
  kubectl rollout status deployment/api-gateway -n "${NAMESPACE}" --timeout=180s || true

  echo -e "\n${CYAN}${BOLD}Waiting for microservices...${NC}"
  MICROSERVICES=(
    "user-service"
    "kyc-management-service"
    "consent-service"
    "bank-service"
    "digilocker-service"
    "email-service"
    "notification-system"
    "log-monitoring-system"
    "ai-service"
  )
  for MS in "${MICROSERVICES[@]}"; do
    kubectl rollout status "deployment/${MS}" -n "${NAMESPACE}" --timeout=180s || true
  done
fi

echo -e "\n${CYAN}${BOLD}====================================================================${NC}"
echo -e "${CYAN}${BOLD}                    DEPLOYMENT COMPLETED                            ${NC}"
echo -e "${CYAN}${BOLD}====================================================================${NC}"
echo -e "${GREEN}Current Pod Status:${NC}"
kubectl get pods -n "${NAMESPACE}"

echo -e "\n${GREEN}Exposed Services:${NC}"
kubectl get svc -n "${NAMESPACE}"

echo -e "\n${BOLD}Access Points:${NC}"
echo -e "  - ${BOLD}API Gateway:${NC} Port 8083"
echo -e "    Run: ${CYAN}kubectl port-forward svc/api-gateway 8083:8083 -n ${NAMESPACE}${NC}"
echo -e "  - ${BOLD}Eureka Dashboard:${NC} Port 8761"
echo -e "    Run: ${CYAN}kubectl port-forward svc/discovery-service 8761:8761 -n ${NAMESPACE}${NC}"
echo -e "    URL: ${CYAN}http://localhost:8761${NC}"
