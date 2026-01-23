/**
 * # EC2 Module
 * 
 * This module provisions an EC2 instance with SSM access and assigns an EIP.
 */

data "aws_ami" "ubuntu" {
  most_recent = true

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  owners = ["099720109477"] # Canonical
}

resource "aws_instance" "web" {
  ami                  = data.aws_ami.ubuntu.id
  instance_type        = var.instance_type
  iam_instance_profile = var.iam_instance_profile_name

  subnet_id                   = var.subnet_id
  vpc_security_group_ids      = [var.security_group_id]
  associate_public_ip_address = true

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
  }

  user_data = <<-EOF
              #!/bin/bash
              set -e

              # 1. Uninstall all conflicting packages
              sudo apt-get remove -y $$(dpkg --get-selections docker.io docker-compose docker-compose-v2 docker-doc podman-docker containerd runc | cut -f1) || true

              # 2. Add Docker's official GPG key
              sudo apt-get update
              sudo apt-get install -y ca-certificates curl
              sudo install -m 0755 -d /etc/apt/keyrings
              sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
              sudo chmod a+r /etc/apt/keyrings/docker.asc

              # 3. Add the repository to Apt sources
              sudo tee /etc/apt/sources.list.d/docker.sources <<EOT
              Types: deb
              URIs: https://download.docker.com/linux/ubuntu
              Suites: $$(. /etc/os-release && echo "$${UBUNTU_CODENAME:-$$VERSION_CODENAME}")
              Components: stable
              Signed-By: /etc/apt/keyrings/docker.asc
              EOT

              sudo apt-get update

              # 4. Install the Docker packages
              sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

              # 5. Docker post-install setup
              sudo systemctl start docker
              sudo systemctl enable docker
              sudo usermod -aG docker ubuntu

              # 6. Install CodeDeploy dependencies and Agent
              sudo apt-get install -y ruby-full wget
              cd /home/ubuntu
              wget https://aws-codedeploy-ap-northeast-2.s3.ap-northeast-2.amazonaws.com/latest/install
              chmod +x ./install
              sudo ./install auto
              sudo systemctl start codedeploy-agent
              sudo systemctl enable codedeploy-agent
              EOF

  tags = {
    Name      = "${var.project_name}-ec2"
    ManagedBy = "Terraform"
  }
}

resource "aws_eip" "web" {
  instance = aws_instance.web.id
  domain   = "vpc"

  tags = {
    Name      = "${var.project_name}-eip"
    ManagedBy = "Terraform"
  }
}
