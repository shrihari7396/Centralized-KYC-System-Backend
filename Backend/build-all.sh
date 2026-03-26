#!/bin/bash

# Base directory
BASE_DIR=$(pwd)

echo "----------------------------------------"
echo "Running mvn clean package for all services..."
echo "----------------------------------------"

build_service() {
    SERVICE_NAME=$1
    SERVICE_PATH=$BASE_DIR/$SERVICE_NAME

    if [ -d "$SERVICE_PATH" ]; then
        echo "Building $SERVICE_NAME..."
        cd "$SERVICE_PATH" || exit
        mvn clean package -DskipTests
        echo "$SERVICE_NAME built successfully."
        cd "$BASE_DIR" || exit
        echo "----------------------------------------"
    else
        echo "Warning: Service directory not found at $SERVICE_PATH"
    fi
}

# List of services in order
SERVICES=(
    "DiscoveryService"
    "LogMonitoringSystem"
    "ApiGateway"
    "BankService"
    "ConsentService"
    "DigilockerService"
    "EmailService"
    "KycManagementService"
    "UserService"
    "AiService"
    "NotificationSystem"
)

# Iterate and build
for SERVICE in "${SERVICES[@]}"; do
    build_service "$SERVICE"
done

echo "All services packaged successfully!"
