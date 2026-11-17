<#
==============================================================================
Centralized KYC System - Windows Kubernetes Deployer
Deploys namespace, configs, secrets, infrastructure, and microservices to K8s
==============================================================================
#>

[CmdletBinding()]
param(
    [switch]$NoWait,
    [string]$Namespace = "kyc-system"
)

$ErrorActionPreference = "Continue"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = (Resolve-Path "$ScriptDir\..\..").Path
$DeployDir = "$RootDir\deploy.k8s"

Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "     Centralized KYC System - Kubernetes Deployment (Windows)       " -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "Project Root : $RootDir"
Write-Host "Manifest Dir : $DeployDir"
Write-Host "Namespace    : $Namespace`n"

# Verify kubectl
try {
    $null = kubectl version --client 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] 'kubectl' client is not working properly." -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "[ERROR] 'kubectl' is not installed or not in system PATH." -ForegroundColor Red
    Write-Host "Please install kubectl and configure your kubeconfig."
    exit 1
}

# Verify cluster connectivity
Write-Host "Checking Kubernetes cluster connectivity..." -ForegroundColor Blue
$clusterInfo = kubectl cluster-info 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Cannot connect to Kubernetes cluster." -ForegroundColor Red
    Write-Host "Current context error: $clusterInfo"
    exit 1
}
$currentCtx = (kubectl config current-context).Trim()
Write-Host "✓ Connected to cluster: $currentCtx`n" -ForegroundColor Green

# Deploy manifests
Write-Host "Applying manifests to Kubernetes..." -ForegroundColor Cyan
& kubectl apply -k $DeployDir
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Successfully applied manifests via Kustomize.`n" -ForegroundColor Green
} else {
    Write-Host "Kustomize apply encountered an issue. Falling back to ordered file apply..." -ForegroundColor Yellow
    
    $Files = @(
        "00-namespace.yaml",
        "01-configmap.yaml",
        "02-secrets.yaml",
        "03-infrastructure.yaml",
        "04-discovery-service.yaml",
        "05-api-gateway.yaml",
        "06-user-service.yaml",
        "07-kyc-management-service.yaml",
        "08-consent-service.yaml",
        "09-bank-service.yaml",
        "10-digilocker-service.yaml",
        "11-email-service.yaml",
        "12-notification-system.yaml",
        "13-log-monitoring-system.yaml",
        "14-ai-service.yaml"
    )

    foreach ($f in $Files) {
        $p = "$DeployDir\$f"
        if (Test-Path $p) {
            Write-Host "Applying $f..." -ForegroundColor Blue
            & kubectl apply -f $p
        }
    }
    Write-Host "✓ Finished sequential apply.`n" -ForegroundColor Green
}

# Wait for rollouts
if (-not $NoWait) {
    Write-Host "Waiting for core infrastructure to become ready..." -ForegroundColor Cyan
    $CoreDeploys = @("mysql", "mongodb", "redis", "zookeeper", "kafka")
    foreach ($dep in $CoreDeploys) {
        Write-Host "Waiting for deployment/$dep..." -ForegroundColor Blue
        & kubectl rollout status "deployment/$dep" -n $Namespace --timeout=180s
    }

    Write-Host "`nWaiting for service discovery & gateway..." -ForegroundColor Cyan
    & kubectl rollout status "deployment/discovery-service" -n $Namespace --timeout=180s
    & kubectl rollout status "deployment/api-gateway" -n $Namespace --timeout=180s

    Write-Host "`nWaiting for microservices..." -ForegroundColor Cyan
    $Microservices = @(
        "user-service",
        "kyc-management-service",
        "consent-service",
        "bank-service",
        "digilocker-service",
        "email-service",
        "notification-system",
        "log-monitoring-system",
        "ai-service"
    )
    foreach ($ms in $Microservices) {
        & kubectl rollout status "deployment/$ms" -n $Namespace --timeout=180s
    }
}

# Summary
Write-Host "`n====================================================================" -ForegroundColor Cyan
Write-Host "                    DEPLOYMENT COMPLETED                            " -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "Current Pod Status:" -ForegroundColor Green
& kubectl get pods -n $Namespace

Write-Host "`nExposed Services:" -ForegroundColor Green
& kubectl get svc -n $Namespace

Write-Host "`nAccess Points:" -ForegroundColor White
Write-Host "  - API Gateway: Port 8083 (LoadBalancer / port-forward)"
Write-Host "    Run: kubectl port-forward svc/api-gateway 8083:8083 -n $Namespace" -ForegroundColor Cyan
Write-Host "  - Eureka Dashboard: Port 8761"
Write-Host "    Run: kubectl port-forward svc/discovery-service 8761:8761 -n $Namespace" -ForegroundColor Cyan
Write-Host "    URL: http://localhost:8761" -ForegroundColor Cyan
