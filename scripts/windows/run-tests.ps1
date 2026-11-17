<#
==============================================================================
Centralized KYC System - Windows Test Runner
Runs unit/integration tests across all 11 Spring Boot microservices
==============================================================================
#>

[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [string]$Service = ""
)

$ErrorActionPreference = "Continue"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = (Resolve-Path "$ScriptDir\..\..").Path
$BackendDir = "$RootDir\Backend"

Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "     Centralized KYC System - Microservice Test Suite (Windows)     " -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "Project Root: $RootDir`n"

$AllServices = @(
    "AiService",
    "ApiGateway",
    "BankService",
    "ConsentService",
    "DigilockerService",
    "DiscoveryService",
    "EmailService",
    "KycManagementService",
    "LogMonitoringSystem",
    "NotificationSystem",
    "UserService"
)

if ($Service -ne "") {
    $TargetServices = @($Service)
    Write-Host "Targeting specific service: $Service`n" -ForegroundColor Yellow
} else {
    $TargetServices = $AllServices
    Write-Host "Running test suites for all $($AllServices.Count) microservices...`n" -ForegroundColor Blue
}

$PassedServices = [System.Collections.Generic.List[string]]::new()
$FailedServices = [System.Collections.Generic.List[string]]::new()
$SkippedServices = [System.Collections.Generic.List[string]]::new()

$StopWatch = [System.Diagnostics.Stopwatch]::StartNew()

foreach ($s in $TargetServices) {
    $ServicePath = "$BackendDir\$s"

    if (-not (Test-Path $ServicePath)) {
        Write-Host "[ERROR] Service directory not found: $ServicePath" -ForegroundColor Red
        $SkippedServices.Add($s)
        continue
    }

    Write-Host ">>> [TESTING] $s..." -ForegroundColor Cyan
    Push-Location $ServicePath

    $MvnCmd = if (Test-Path ".\mvnw.cmd") { ".\mvnw.cmd" } else { "mvn" }

    & $MvnCmd test
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ [PASSED] $s`n" -ForegroundColor Green
        $PassedServices.Add($s)
    } else {
        Write-Host "✗ [FAILED] $s`n" -ForegroundColor Red
        $FailedServices.Add($s)
    }

    Pop-Location
}

$StopWatch.Stop()

Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "                         TEST SUMMARY                               " -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "Total Time Elapsed : $([math]::Round($StopWatch.Elapsed.TotalSeconds, 2)) seconds"
Write-Host "Total Executed     : $($TargetServices.Count)"
Write-Host "Passed             : $($PassedServices.Count)" -ForegroundColor Green
Write-Host "Failed             : $($FailedServices.Count)" -ForegroundColor Red

if ($SkippedServices.Count -gt 0) {
    Write-Host "Skipped/Not Found  : $($SkippedServices.Count)" -ForegroundColor Yellow
}

Write-Host "--------------------------------------------------------------------"
if ($FailedServices.Count -eq 0) {
    Write-Host "✓ ALL TEST SUITES PASSED SUCCESSFULLY!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "✗ FAILURES DETECTED IN:" -ForegroundColor Red
    foreach ($f in $FailedServices) {
        Write-Host "  - $f" -ForegroundColor Red
    }
    exit 1
}
