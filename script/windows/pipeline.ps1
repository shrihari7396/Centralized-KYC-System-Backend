<#
==============================================================================
Centralized KYC System - Windows Master End-to-End Pipeline
Runs tests -> Builds Docker images -> Deploys to Kubernetes -> Checks Status
==============================================================================
#>

[CmdletBinding()]
param(
    [switch]$SkipTests,
    [switch]$SkipBuild,
    [switch]$NoWait
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "   Centralized KYC System - Master CI/CD Pipeline (Windows)         " -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan

# Stage 1: Tests
if (-not $SkipTests) {
    Write-Host "`n[STAGE 1/3] Running Microservice Test Suites..." -ForegroundColor Blue
    & "$ScriptDir\run-tests.ps1"
} else {
    Write-Host "`n[STAGE 1/3] Skipped Test Execution." -ForegroundColor Yellow
}

# Stage 2: Build Images
if (-not $SkipBuild) {
    Write-Host "`n[STAGE 2/3] Building JARs and Docker Container Images..." -ForegroundColor Blue
    & "$ScriptDir\build-images.ps1"
} else {
    Write-Host "`n[STAGE 2/3] Skipped Container Image Building." -ForegroundColor Yellow
}

# Stage 3: Deploy
Write-Host "`n[STAGE 3/3] Deploying Manifests to Kubernetes..." -ForegroundColor Blue
$deployParams = @{}
if ($NoWait) {
    $deployParams["NoWait"] = $true
}
& "$ScriptDir\deploy-k8s.ps1" @deployParams

# Status Check
Write-Host "`n[POST-DEPLOY] Checking Cluster Runtime Health..." -ForegroundColor Blue
& "$ScriptDir\status-k8s.ps1"

Write-Host "`n====================================================================" -ForegroundColor Green
Write-Host "   ✓ COMPLETE PIPELINE EXECUTED SUCCESSFULLY!                      " -ForegroundColor Green
Write-Host "====================================================================" -ForegroundColor Green
