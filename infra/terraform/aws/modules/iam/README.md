# IAM Module

Standard IAM module to enable secure access and service permissions.

## Features
- IAM Role with trust policy for EC2.
- Integration with AWS SSM Session Manager for keyless console access.
- Limited S3 access for CodeDeploy artifacts (Least Privilege).
- Instance Profile generation for easy attachment to EC2.
- Dedicated Service Role for AWS CodeDeploy.

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| project_name | Namespace to be used for IAM resource naming | `string` | n/a | yes |

## Outputs

| Name | Description |
|------|-------------|
| instance_profile_name | The name of the IAM instance profile |
| codedeploy_role_arn | The ARN of the IAM role for CodeDeploy service |
