# Infrastructure (Terraform)

This repository contains Terraform configurations to manage cloud infrastructure in a modular and reusable way.

## Directory Structure

- `aws/modules/`: Reusable infrastructure components (VPC, Security Group, EC2, IAM, CodeDeploy).
- `aws/environments/`: Environment-specific configurations (dev, stage, prod).

## Naming Conventions & Tagging

Resources follow a standardized naming convention:
- **Pattern**: `tf-[project]-[env]-[resource]` (e.g., `tf-ticketing-prod-vpc`).
- **Shortcuts**: `sg` (Security Group), `dg` (Deployment Group), `rt` (Route Table), `sub` (Subnet).
- **Tagging**: All resources are tagged with `ManagedBy = "Terraform"` for easy filtering and management.

## Prerequisites

1.  **Cloud Provider CLI**: Configured with appropriate credentials (e.g., AWS CLI).
2.  **Terraform**: Installed (v1.0+ recommended).
3.  **Permissions**: Sufficient IAM permissions to create and manage the defined resources (IAM, EC2, VPC, CodeDeploy).

## Usage

Navigate to the desired environment directory (e.g., `aws/environments/prod`):

```bash
cd aws/environments/prod
terraform init
terraform plan
terraform apply
```

## Standards & Best Practices

- **Modularity**: Resources are grouped into logical modules.
- **Security**: Adheres to security best practices (e.g., AWS SSM for access, no SSH required, least privilege IAM).
- **Deployment**: Integrated with AWS CodeDeploy for automated, reliable application updates.
- **Environment Isolation**: Separate state and configurations for different environments.
- **Documentation**: All modules include a standardized `README.md`.
