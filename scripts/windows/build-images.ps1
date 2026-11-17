<#
==============================================================================
Centralized KYC System - Windows Image Builder
Packages JARs and builds Docker images for all 11 microservices
==============================================================================
#>

[CmdletBinding()]
param(
    [switch]$SkipPackage,
    [string]$Prefix = "kyc-system",
    [string]$Tag = "latest",
    [string]$Service = ""
)

$ErrorActionPreference = "Continue"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = (Resolve-Path "$ScriptDir\..\..").Path
$BackendDir = "$RootDir\Backend"

Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "     Centralized KYC System - Docker Image Builder (Windows)        " -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "Project Root   : $RootDir"
Write-Host "Image Tag      : $Prefix/<service-name>:$Tag"
Write-Host "Skip Packaging : $SkipPackage`n"

# Verify Docker
try {
    $dockerCheck = docker info 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Docker daemon is not running. Please start Docker Desktop." -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "[ERROR] Docker CLI not found or daemon not reachable." -ForegroundColor Red
    exit 1
}

$ServiceMap = [ordered]@{
    "DiscoveryService"     = "discovery-service"
    "ApiGateway"           = "api-gateway"
    "UserService"          = "user-service"
    "KycManagementService" = "kyc-management-service"
    "ConsentService"       = "consent-service"
    "BankService"          = "bank-service"
    "DigilockerService"    = "digilocker-service"
    "EmailService"         = "email-service"
    "NotificationSystem"   = "notification-system"
    "LogMonitoringSystem"  = "log-monitoring-system"
    "AiService"            = "ai-service"
}

if ($Service -ne "") {
    if (-not $ServiceMap.Contains($Service)) {
        Write-Host "[ERROR] Unknown service name: '$Service'" -ForegroundColor Red
        Write-Host "Available services: $($ServiceMap.Keys -join ', ')"
        exit 1
    }
    $SelectedServices = @($Service)
} else {
    $SelectedServices = $ServiceMap.Keys
}

$BuiltImages = [System.Collections.Generic.List[string]]::new()
$FailedBuilds = [System.Collections.Generic.List[string]]::new()
$StopWatch = [System.Diagnostics.StopWatch]::StartNew()

foreach ($s in $SelectedServices) {
    $ServiceDir = "$BackendDir\$s"
    $ImageName = $ServiceMap[$s]
    $FullImage = "$Prefix/${ImageName}:$Tag"

    Write-Host "====================================================================" -ForegroundColor Blue
    Write-Host ">>> Building: $s -> $FullImage" -ForegroundColor Blue
    Write-Host "====================================================================" -ForegroundColor Blue

    if (-not (Test-Path $ServiceDir)) {
        Write-Host "[ERROR] Directory not found: $ServiceDir" -ForegroundColor Red
        $FailedBuilds.Add($s)
        continue
    }

    Push-Location $ServiceDir

    # Step 1: Package JAR
    if (-not $SkipPackage) {
        Write-Host "Step 1/2: Compiling & packaging fat JAR..." -ForegroundColor Cyan
        $MvnCmd = if (Test-Path ".\mvnw.cmd") { ".\mvnw.cmd" } else { "mvn" }
        & $MvnCmd clean package -DskipTests
        if ($LASTEXITCODE -ne 0) {
            Write-Host "✗ Maven build failed for $s." -ForegroundColor Red
            $FailedBuilds.Add($s)
            Pop-Location
            continue
        }
        Write-Host "✓ JAR package successful." -ForegroundColor Green
    } else {
        Write-Host "Step 1/2: Skipped packaging (using existing target/*.jar)." -ForegroundColor Yellow
    }

    # Step 2: Build Docker Image
    Write-Host "Step 2/2: Building Docker image $FullImage..." -ForegroundColor Cyan
    & docker build -t $FullImage .
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Successfully built Docker image: $FullImage" -ForegroundColor Green
        $BuiltImages.Add($FullImage)
    } else {
        Write-Host "✗ Docker build failed for $FullImage." -ForegroundColor Red
        $FailedBuilds.Add($s)
    }

    Pop-Location
}

$StopWatch.Stop()

Write-Host "`n====================================================================" -ForegroundColor Cyan
Write-Host "                      BUILD SUMMARY                                 " -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "Total Time Elapsed : $([math]::Round($StopWatch.Elapsed.TotalSeconds, 2)) seconds"
Write-Host "Images Built       : $($BuiltImages.Count)" -ForegroundColor Green
Write-Host "Build Failures     : $($FailedBuilds.Count)" -ForegroundColor Red

if ($BuiltImages.Count -gt 0) {
    Write-Host "`nBuilt Images List:" -ForegroundColor Green
    foreach ($img in $BuiltImages) {
        Write-Host "  - $img"
    }
}

if ($FailedBuilds.Count -gt 0) {
    Write-Host "`nFailed Services:" -ForegroundColor Red
    foreach ($f in $FailedBuilds) {
        Write-Host "  - $f" -ForegroundColor Red
    }
    exit 1
}

Write-Host "`n✓ ALL REQUESTED IMAGES BUILT SUCCESSFULLY!" -ForegroundColor Green
