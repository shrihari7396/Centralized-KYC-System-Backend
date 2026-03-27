#!/bin/bash

ACTION=$1   # 1 = start, 0 = stop

MYSQL="mysql-kyc"
MONGO="mongo-kyc"
REDIS="redis-kyc"

KAFKA_DIR="$HOME/Documents/Centralized-KYC-System/Backend/Kafka"

echo "Processing containers..."

if [ "$ACTION" == "1" ]; then
    echo "Starting MySQL, MongoDB, Redis..."
    docker start $MYSQL $MONGO $REDIS 2>/dev/null

    echo "Starting Kafka (docker-compose)..."
    cd $KAFKA_DIR || { echo "Kafka directory not found ❌"; exit 1; }
    docker-compose up -d

    echo "All services started ✅"

elif [ "$ACTION" == "0" ]; then
    echo "Stopping MySQL, MongoDB, Redis..."
    docker stop $MYSQL $MONGO $REDIS 2>/dev/null

    echo "Stopping Kafka (docker-compose)..."
    cd $KAFKA_DIR || { echo "Kafka directory not found ❌"; exit 1; }
    docker-compose down

    echo "All services stopped 🛑"

else
    echo "Invalid parameter ❌"
    echo "Usage: ./manage-containers.sh [1=start | 0=stop]"
fi