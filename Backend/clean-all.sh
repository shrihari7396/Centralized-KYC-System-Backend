#!/bin/bash

# Base directory
BASE_DIR=~/Documents/Centralized-KYC-System/Backend

# Logs directory
LOG_DIR=$BASE_DIR/logs

echo "Clearing logs..."
if [ -d "$LOG_DIR" ]; then
    # Delete all .log files or contents
    rm -f "$LOG_DIR"/*.log
    echo "Logs cleared in $LOG_DIR"
else
    echo "Logs directory $LOG_DIR does not exist."
fi

echo "----------------------------------------"
echo "Running mvn clean for all services..."
echo "----------------------------------------"

clean_service() {
    SERVICE_NAME=$1
    SERVICE_PATH=$BASE_DIR/$SERVICE_NAME

    if [ -d "$SERVICE_PATH" ]; then
        echo "Cleaning $SERVICE_NAME..."
        cd "$SERVICE_PATH" || exit
        mvn clean
        echo "$SERVICE_NAME cleaned."
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

# Iterate and clean
for SERVICE in "${SERVICES[@]}"; do
    clean_service "$SERVICE"
done

echo "All services cleaned successfully!"
