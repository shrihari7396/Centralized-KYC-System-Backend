#!/usr/bin/env bash
# ==============================================================================
# Centralized KYC System - Linux Kubernetes Teardown
# Deletes deployments, services, configs, and namespace from Kubernetes
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
FORCE=false
DELETE_NAMESPACE=true

while [[ $# -gt 0 ]]; do
  case "$1" in
    --force|-f|-y)
      FORCE=true
      shift
      ;;
    --keep-namespace)
      DELETE_NAMESPACE=false
      shift
      ;;
    --help|-h)
      echo "Usage: $0 [options]"
      echo "Options:"
      echo "  --force, -f, -y       Skip confirmation prompt"
      echo "  --keep-namespace      Delete resources but keep the namespace intact"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

echo -e "${RED}${BOLD}====================================================================${NC}"
echo -e "${RED}${BOLD}     Centralized KYC System - Kubernetes Teardown (Linux)           ${NC}"
echo -e "${RED}${BOLD}====================================================================${NC}"
echo -e "Namespace: ${NAMESPACE}"

if [ "${FORCE}" = false ]; then
  read -r -p "Are you sure you want to delete all resources in '${NAMESPACE}'? [y/N]: " CONFIRM
  if [[ ! "${CONFIRM}" =~ ^[yY](es)?$ ]]; then
    echo -e "${YELLOW}Teardown aborted.${NC}"
    exit 0
  fi
fi

if ! command -v kubectl >/dev/null 2>&1; then
  echo -e "${RED}[ERROR] 'kubectl' not found.${NC}"
  exit 1
fi

if [ "${DELETE_NAMESPACE}" = true ]; then
  echo -e "${BLUE}Deleting entire namespace '${NAMESPACE}' and all contained resources...${NC}"
  if kubectl get namespace "${NAMESPACE}" >/dev/null 2>&1; then
    kubectl delete namespace "${NAMESPACE}" --timeout=120s
    echo -e "${GREEN}✓ Namespace '${NAMESPACE}' successfully deleted.${NC}"
  else
    echo -e "${YELLOW}Namespace '${NAMESPACE}' does not exist.${NC}"
  fi
else
  echo -e "${BLUE}Deleting resources using Kustomize manifests...${NC}"
  kubectl delete -k "${DEPLOY_DIR}" --ignore-not-found=true
  echo -e "${GREEN}✓ Resources deleted.${NC}"
fi

echo -e "\n${GREEN}${BOLD}✓ TEARDOWN COMPLETE!${NC}"
