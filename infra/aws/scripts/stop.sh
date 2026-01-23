#!/bin/bash
set -e

echo "Stopping existing Docker containers..."

cd /home/ubuntu/ticketing-service 2>/dev/null || exit 0

# Stop containers if they exist
if [ -f docker-compose.prod.yml ]; then
    sudo docker compose -f docker-compose.prod.yml down || true
fi

echo "Application stopped."
