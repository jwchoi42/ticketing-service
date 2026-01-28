# EC2 Module

Generic EC2 instance module optimized for web servers and application hosts.

## Features
- Automatic AMI discovery for Ubuntu Server.
- IAM Instance Profile integration for secure access.
- User Data support for initial provisioning (Docker & CodeDeploy Agent).
- Elastic IP (EIP) assignment for persistent public address.

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| iam_instance_profile_name | The name of the IAM instance profile for secure access (e.g., SSM) | `string` | n/a | yes |
| instance_type | EC2 instance specification (e.g., t3.micro, t3.small) | `string` | n/a | yes |
| project_name | Namespace for resource tagging and identification | `string` | n/a | yes |
| security_group_id | The ID of the security group to attach to the instance | `string` | n/a | yes |
| subnet_id | The subnet ID to launch the instance in | `string` | n/a | yes |

## Outputs

| Name | Description |
|------|-------------|
| instance_id | The ID of the EC2 instance |
| public_ip | The public Elastic IP of the instance |
