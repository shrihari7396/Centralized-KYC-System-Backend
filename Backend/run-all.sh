#!/bin/bash

# Base directory (change if needed)
BASE_DIR=~/Documents/Centralized-KYC-System/Backend

# Logs directory
LOG_DIR=$BASE_DIR/logs

# Create logs folder if not exists
mkdir -p $LOG_DIR

echo "Starting all services..."

# Function to run service
run_service() {
    SERVICE_NAME=$1
    SERVICE_PATH=$BASE_DIR/$SERVICE_NAME
    LOG_FILE=$LOG_DIR/$SERVICE_NAME.log

    echo "Starting $SERVICE_NAME..."

    cd $SERVICE_PATH || exit

    # If using Maven
    nohup mvn spring-boot:run > "$LOG_FILE" 2>&1 &

    # If using JAR instead, use this instead:
    # nohup java -jar target/*.jar > "$LOG_FILE" 2>&1 &

    echo "$SERVICE_NAME started with PID $!"
}

# Start all services
run_service DiscoveryService
sleep 10
run_service LogMonitoringSystem
sleep 10
run_service ApiGateway
run_service BankService
run_service ConsentService
run_service DigilockerService
run_service EmailService
run_service KycManagementService
run_service UserService
run_service NotificationSystem
run_service AiService


echo "All services started!"