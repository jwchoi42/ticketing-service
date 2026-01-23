#!/bin/bash
set -e

echo "Starting EC2 initialization script..."

# ==============================================================================
# 1. Docker Installation (Official Docker Documentation)
# https://docs.docker.com/engine/install/ubuntu/
# ==============================================================================

# 1-1. Uninstall all conflicting packages
sudo apt-get remove -y $(dpkg --get-selections docker.io docker-compose docker-compose-v2 docker-doc podman-docker containerd runc | cut -f1) || true

# 1-2. Add Docker's official GPG key
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

# 1-3. Add the repository to Apt sources
sudo tee /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Signed-By: /etc/apt/keyrings/docker.asc
EOF

sudo apt-get update

# 1-4. Install the Docker packages
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 1-5. Docker post-install setup
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ubuntu

echo "Docker installation completed!"

# ==============================================================================
# 2. AWS CLI Installation (Required for SSM secrets)
# ==============================================================================
sudo apt-get install -y awscli

echo "AWS CLI installation completed!"

# ==============================================================================
# 3. CodeDeploy Agent Installation
# ==============================================================================

sudo apt-get install -y ruby-full wget
cd /home/ubuntu
wget https://aws-codedeploy-ap-northeast-2.s3.ap-northeast-2.amazonaws.com/latest/install
chmod +x ./install
sudo ./install auto
sudo systemctl start codedeploy-agent
sudo systemctl enable codedeploy-agent

echo "CodeDeploy Agent installation completed!"
echo "EC2 initialization script finished successfully!"
