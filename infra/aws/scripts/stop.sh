#!/bin/bash
set -e

echo "Stopping existing Docker containers..."

cd /home/ubuntu/ticketing-service 2>/dev/null || exit 0

# Stop containers if they exist
if [ -f compose.prod.yaml ]; then
    sudo docker compose -f compose.prod.yaml down || true
fi

echo "Application stopped."
