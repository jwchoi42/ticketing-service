#!/bin/bash

# Exit on error
set -e

# Navigate to the deployment directory
cd /home/ubuntu/ticketing-service

echo "Starting deployment script via CodeDeploy..."

# Note: In a production environment, you should use AWS SSM Parameter Store to get these.
# For now, we assume the environment is set or we skip login if already logged in.
# Alternatively, use $(aws ecr get-login-password) if using ECR.

# 1. Pull latest images
echo "Updating Docker containers..."
sudo docker compose -f docker-compose.prod.yml pull
sudo docker compose -f docker-compose.prod.yml up -d

# 2. Cleanup
echo "Pruning old Docker images..."
sudo docker image prune -f

echo "Deployment successful!"
