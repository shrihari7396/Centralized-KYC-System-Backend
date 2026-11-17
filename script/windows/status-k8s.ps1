<#
==============================================================================
Centralized KYC System - Windows Kubernetes Status Checker
Displays detailed health and runtime status of pods, services, and volumes
==============================================================================
#>

[CmdletBinding()]
param(
    [string]$Namespace = "kyc-system"
)

$ErrorActionPreference = "Continue"

Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "     Centralized KYC System - Cluster Status (Namespace: $Namespace)" -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan

try {
    $null = kubectl version --client 2>&1
} catch {
    Write-Host "[ERROR] 'kubectl' not found in PATH." -ForegroundColor Red
    exit 1
}

$nsCheck = kubectl get namespace $Namespace 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Namespace '$Namespace' does not exist." -ForegroundColor Red
    exit 1
}

Write-Host "`n>>> POD STATUS:" -ForegroundColor Blue
& kubectl get pods -n $Namespace -o wide

Write-Host "`n>>> DEPLOYMENTS STATUS:" -ForegroundColor Blue
& kubectl get deployments -n $Namespace

Write-Host "`n>>> SERVICES & EXPOSED PORTS:" -ForegroundColor Blue
& kubectl get svc -n $Namespace

Write-Host "`n>>> PERSISTENT VOLUME CLAIMS:" -ForegroundColor Blue
& kubectl get pvc -n $Namespace

# Check non-running pods
$podsRaw = kubectl get pods -n $Namespace --no-headers 2>&1
$abnormal = @()
foreach ($line in $podsRaw) {
    if ($line -notmatch "Running|Completed") {
        $abnormal += $line
    }
}

if ($abnormal.Count -gt 0) {
    Write-Host "`n[WARNING] The following pods are in abnormal/transitional states:" -ForegroundColor Yellow
    foreach ($a in $abnormal) {
        Write-Host "  $a" -ForegroundColor Yellow
    }
    Write-Host "`nTo inspect logs for a problematic pod, run:" -ForegroundColor Cyan
    Write-Host "  kubectl logs <pod-name> -n $Namespace" -ForegroundColor Cyan
    Write-Host "To inspect pod events/failures, run:" -ForegroundColor Cyan
    Write-Host "  kubectl describe pod <pod-name> -n $Namespace" -ForegroundColor Cyan
} else {
    Write-Host "`n✓ ALL PODS IN '$Namespace' ARE RUNNING OR COMPLETED HEALTHILY!" -ForegroundColor Green
}
