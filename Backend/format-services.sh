#!/bin/bash

# Base directory
BASE_DIR=~/Documents/Centralized-KYC-System/Backend

echo "Formatting all services with Spotless..."

format_service() {
    SERVICE_NAME=$1
    SERVICE_PATH=$BASE_DIR/$SERVICE_NAME

    echo "----------------------------------------"
    echo "Formatting $SERVICE_NAME..."
    echo "----------------------------------------"
    cd $SERVICE_PATH || exit
    mvn spotless:apply
}

# Format all services
format_service DiscoveryService
format_service LogMonitoringSystem
format_service ApiGateway
format_service BankService
format_service ConsentService
format_service DigilockerService
format_service EmailService
format_service KycManagementService
format_service UserService
format_service NotificationSystem

echo "----------------------------------------"
echo "All services formatted!"
echo "----------------------------------------"
