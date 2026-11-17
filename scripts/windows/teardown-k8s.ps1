<#
==============================================================================
Centralized KYC System - Windows Kubernetes Teardown
Deletes deployments, services, configs, and namespace from Kubernetes
==============================================================================
#>

[CmdletBinding()]
param(
    [switch]$Force,
    [switch]$KeepNamespace,
    [string]$Namespace = "kyc-system"
)

$ErrorActionPreference = "Continue"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = (Resolve-Path "$ScriptDir\..\..").Path
$DeployDir = "$RootDir\deploy.k8s"

Write-Host "====================================================================" -ForegroundColor Red
Write-Host "     Centralized KYC System - Kubernetes Teardown (Windows)         " -ForegroundColor Red
Write-Host "====================================================================" -ForegroundColor Red
Write-Host "Namespace: $Namespace"

if (-not $Force) {
    $confirm = Read-Host "Are you sure you want to delete all resources in '$Namespace'? [y/N]"
    if ($confirm -notmatch "^[yY](es)?$") {
        Write-Host "Teardown aborted." -ForegroundColor Yellow
        exit 0
    }
}

try {
    $null = kubectl version --client 2>&1
} catch {
    Write-Host "[ERROR] 'kubectl' not found." -ForegroundColor Red
    exit 1
}

if (-not $KeepNamespace) {
    Write-Host "Deleting entire namespace '$Namespace' and all contained resources..." -ForegroundColor Blue
    & kubectl delete namespace $Namespace --timeout=120s
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Namespace '$Namespace' successfully deleted." -ForegroundColor Green
    }
} else {
    Write-Host "Deleting resources using Kustomize manifests..." -ForegroundColor Blue
    & kubectl delete -k $DeployDir --ignore-not-found=true
    Write-Host "✓ Resources deleted." -ForegroundColor Green
}

Write-Host "`n✓ TEARDOWN COMPLETE!" -ForegroundColor Green
