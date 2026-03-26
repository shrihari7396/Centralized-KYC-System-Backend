#!/bin/bash

# Base directory
BASE_DIR=$(pwd)

echo "Removing src/test from all microservices..."

rm -rf "$BASE_DIR/AiService/src/test"
rm -rf "$BASE_DIR/ApiGateway/src/test"
rm -rf "$BASE_DIR/BankService/src/test"
rm -rf "$BASE_DIR/ConsentService/src/test"
rm -rf "$BASE_DIR/DigilockerService/src/test"
rm -rf "$BASE_DIR/DiscoveryService/src/test"
rm -rf "$BASE_DIR/EmailService/src/test"
rm -rf "$BASE_DIR/KycManagementService/src/test"
rm -rf "$BASE_DIR/UserService/src/test"
rm -rf "$BASE_DIR/LogMonitoringSystem/src/test"
rm -rf "$BASE_DIR/NotificationSystem/src/test"

echo "Test directories removed."
