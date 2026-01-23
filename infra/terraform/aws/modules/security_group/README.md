# Security Group Module

Flexible Security Group module for managing firewall rules.

## Features
- Inbound rules for standard web traffic (HTTP/HTTPS).
- Restricted access management (all traffic typically routed through a proxy or SSM).
- Full outbound (egress) connectivity by default.

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| project_name | Namespace for resource naming | `string` | n/a | yes |
| vpc_id | The ID of the VPC where the security group will be created | `string` | n/a | yes |

## Outputs

| Name | Description |
|------|-------------|
| security_group_id | The ID of the created security group |
