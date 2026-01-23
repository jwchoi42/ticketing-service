#!/bin/bash

# Exit on error
set -e

# Navigate to the deployment directory
cd /home/ubuntu/ticketing-service

echo "Starting deployment script via CodeDeploy..."

# 1. Fetch secrets from AWS SSM Parameter Store
echo "Fetching secrets from AWS SSM..."
<<<<<<< HEAD
export DB_PASSWORD=$(aws ssm get-parameter --name "/ticketing/prod/DB_PASSWORD" --with-decryption --query "Parameter.Value" --output text --region ap-northeast-2)
export DOCKERHUB_USERNAME=$(aws ssm get-parameter --name "/ticketing/prod/DOCKERHUB_USERNAME" --query "Parameter.Value" --output text --region ap-northeast-2)
export DOCKERHUB_TOKEN=$(aws ssm get-parameter --name "/ticketing/prod/DOCKERHUB_TOKEN" --with-decryption --query "Parameter.Value" --output text --region ap-northeast-2)
=======
export DB_PASSWORD=$(aws ssm get-parameter --name "/ticketing/prod/DB_PASSWORD" --with-decryption --query "Parameter.Value" --output text)
export DOCKERHUB_USERNAME=$(aws ssm get-parameter --name "/ticketing/prod/DOCKERHUB_USERNAME" --query "Parameter.Value" --output text)
export DOCKERHUB_TOKEN=$(aws ssm get-parameter --name "/ticketing/prod/DOCKERHUB_TOKEN" --with-decryption --query "Parameter.Value" --output text)
>>>>>>> 4de69bb87f56222f8cb46101dc4c63cf4d840f79

# 2. Login to Docker Hub
echo "Logging in to Docker Hub..."
echo "$DOCKERHUB_TOKEN" | sudo docker login -u "$DOCKERHUB_USERNAME" --password-stdin

# 3. Pull latest images and restart containers
echo "Updating Docker containers..."
# Use sudo -E to preserve the exported environment variables (especially DB_PASSWORD)
sudo -E docker compose -f docker-compose.prod.yml pull
sudo -E docker compose -f docker-compose.prod.yml up -d

# 4. Cleanup
echo "Pruning old Docker images..."
sudo docker image prune -f

echo "Deployment successful!"
