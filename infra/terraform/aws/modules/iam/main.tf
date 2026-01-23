/**
 * # IAM Module
 * 
 * This module defines the IAM Roles and Permissions for both EC2 Instances and the CodeDeploy Service.
 */

# ==============================================================================
# 1. EC2 INSTANCE PERMISSIONS
# ==============================================================================

# [1-1] EC2 Role Definition: The primary identity for the server
resource "aws_iam_role" "ec2_role" {
  name = "${var.project_name}-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ec2.amazonaws.com"
      }
    }]
  })

  tags = {
    Name      = "${var.project_name}-ec2-role"
    ManagedBy = "Terraform"
  }
}

# [1-2] SSM Permission: Grants terminal access via AWS Systems Manager
resource "aws_iam_role_policy_attachment" "ec2_ssm_attach" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# [1-3] CodeDeploy Permission: Grants ability to download deployment artifacts
resource "aws_iam_role_policy_attachment" "ec2_codedeploy_attach" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2RoleforAWSCodeDeployLimited"
}

# [1-4] EC2 Profile Definition: Wraps the role so it can be attached to the instance
resource "aws_iam_instance_profile" "ec2_profile" {
  name = "${var.project_name}-ec2-profile"
  role = aws_iam_role.ec2_role.name

  tags = {
    ManagedBy = "Terraform"
  }
}


# ==============================================================================
# 2. CODEDEPLOY SERVICE PERMISSIONS
# ==============================================================================

# [2-1] CodeDeploy Role Definition: The identity for the CodeDeploy service itself
resource "aws_iam_role" "codedeploy_role" {
  name = "${var.project_name}-cd-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "codedeploy.amazonaws.com"
      }
    }]
  })

  tags = {
    Name      = "${var.project_name}-cd-role"
    ManagedBy = "Terraform"
  }
}

# [2-2] Management Permission: Grants CodeDeploy authority to manage EC2 resources
resource "aws_iam_role_policy_attachment" "codedeploy_service_attach" {
  role       = aws_iam_role.codedeploy_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSCodeDeployRole"
}
