# VPC Module

Standard VPC module for creating a baseline network environment.

## Features
- Custom VPC with DNS support.
- Public subnet with auto-assign public IP on launch.
- Internet Gateway for external connectivity.
- Route Table and Association for public routing.

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| aws_region | The AWS region to deploy resources into | `string` | n/a | yes |
| project_name | Namespace to be used for resource naming and tagging | `string` | n/a | yes |
| public_subnet_cidr | The CIDR block for the public subnet | `string` | `"10.0.1.0/24"` | no |
| vpc_cidr | The CIDR block for the VPC | `string` | `"10.0.0.0/16"` | no |

## Outputs

| Name | Description |
|------|-------------|
| public_subnet_id | The ID of the created public subnet |
| vpc_id | The ID of the created VPC |
