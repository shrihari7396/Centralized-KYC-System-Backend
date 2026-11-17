#!/usr/bin/env bash
# ==============================================================================
# Centralized KYC System - macOS Kubernetes Status Checker
# Displays detailed health and runtime status of pods, services, and volumes
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

NAMESPACE="kyc-system"

if [ $# -ge 1 ] && [ -n "$1" ]; then
  NAMESPACE="$1"
fi

echo -e "${CYAN}${BOLD}====================================================================${NC}"
echo -e "${CYAN}${BOLD}     Centralized KYC System - Cluster Status (Namespace: ${NAMESPACE})${NC}"
echo -e "${CYAN}${BOLD}====================================================================${NC}"

if ! command -v kubectl >/dev/null 2>&1; then
  echo -e "${RED}[ERROR] 'kubectl' not found in PATH.${NC}"
  exit 1
fi

if ! kubectl get namespace "${NAMESPACE}" >/dev/null 2>&1; then
  echo -e "${RED}[ERROR] Namespace '${NAMESPACE}' does not exist.${NC}"
  exit 1
fi

echo -e "\n${BOLD}${BLUE}>>> POD STATUS:${NC}"
kubectl get pods -n "${NAMESPACE}" -o wide

echo -e "\n${BOLD}${BLUE}>>> DEPLOYMENTS STATUS:${NC}"
kubectl get deployments -n "${NAMESPACE}"

echo -e "\n${BOLD}${BLUE}>>> SERVICES & EXPOSED PORTS:${NC}"
kubectl get svc -n "${NAMESPACE}"

echo -e "\n${BOLD}${BLUE}>>> PERSISTENT VOLUME CLAIMS:${NC}"
kubectl get pvc -n "${NAMESPACE}"

NON_RUNNING=$(kubectl get pods -n "${NAMESPACE}" --no-headers | grep -v -E "Running|Completed" || true)
if [ -n "${NON_RUNNING}" ]; then
  echo -e "\n${YELLOW}${BOLD}[WARNING] The following pods are in abnormal/transitional states:${NC}"
  echo "${NON_RUNNING}"
  echo -e "\n${CYAN}To inspect logs for a problematic pod, run:${NC}"
  echo -e "  kubectl logs <pod-name> -n ${NAMESPACE}"
  echo -e "${CYAN}To inspect pod events/failures, run:${NC}"
  echo -e "  kubectl describe pod <pod-name> -n ${NAMESPACE}"
else
  echo -e "\n${GREEN}${BOLD}✓ ALL PODS IN '${NAMESPACE}' ARE RUNNING OR COMPLETED HEALTHILY!${NC}"
fi
