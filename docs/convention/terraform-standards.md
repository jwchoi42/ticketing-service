# Terraform Development Standards

This document serves as the **Source of Truth** for all Terraform infrastructure code within the project. All AI agents and developers must adhere to these guidelines.

## 1. Core Principles
 We strictly follow the infrastructure-as-code principles defined in the [HashiCorp Official Documentation](https://developer.hashicorp.com/terraform/docs).
- **Declarative Code**: Define the desired state, not the steps to reach it.
- **Idempotency**: Applying the same code multiple times should produce the same result.
- **Immutability**: Prefer replacing infrastructure over modifying it in place.

## 2. Security Rules (Checkov)
Security is paramount. All Terraform code must be scanned with **Checkov** to identify and resolve vulnerabilities.
- **Mandatory Scanning**: Code must pass `checkov` scans before merging or applying.
- **Secure by Default**:
    - Avoid open security groups (e.g., `0.0.0.0/0` on SSH/RDP).
    - Encrypt data at rest (S3, RDS, EBS) and in transit.
    - Principle of Least Privilege for IAM roles.
- **Access Management**:
    - Prefer **AWS SSM Session Manager** over SSH keys for terminal access.
    - If SSM is used, port 22 (SSH) should remain closed in Security Groups.

## 3. Community Best Practices
We adopt the standards from [Terraform Best Practices](https://www.terraform-best-practices.com/).

### Directory Structure
- Follow the `provider/services/envs` pattern (or `modules/environments` where appropriate).
- Isolate environments (dev, stage, prod) into separate directories to limit blast radius.

### Naming Conventions
- **snake_case** for all internal resource IDs in Terraform (e.g., `resource "aws_vpc" "main_vpc"`).
- **Kebab-case** for physical resource names on AWS (e.g., `tf-ticketing-prod-vpc`).
- **Standard Pattern**: `tf-[project]-[env]-[resource]`.
- **Shortcuts**: Use standard abbreviations for resource types (e.g., `sg` for security group, `dg` for deployment group, `sub` for subnet).
- **Do not repeat** the resource type in the internal ID (e.g., `resource "aws_route_table" "public"` instead of `public_route_table`).

### Tagging
- All resources must include a `ManagedBy = "Terraform"` tag.
- Use a `Name` tag following the standard naming pattern.

## 4. Documentation Standards
All modules must be documented using **[terraform-docs](https://github.com/terraform-docs/terraform-docs)** style comments.
- **Header**: Description of what the module does.
- **Variables**: Description, type, and default value (if any).
- **Outputs**: Description of what is returned.

**Example:**
```hcl
variable "vpc_cidr" {
  type        = string
  description = "The CIDR block for the VPC"
  default     = "10.0.0.0/16"
}
```

## 5. Ambiguity Policy
If a requirement is unclear, or if there are multiple valid architectural approaches (e.g., using a single NAT Gateway vs. one per AZ), **you must ask the user** for clarification before proceeding. Do not make assumptions about cost or architecture trade-offs.
