#!/bin/bash

echo "Stopping all services..."

pkill -f 'spring-boot:run'
# OR if using jar:
# pkill -f '.jar'

echo "All services stopped!"