# CodeDeploy Module

This module manages the software deployment automation using AWS CodeDeploy.

## Features
- CodeDeploy Application.
- Deployment Group targeting EC2 instances by tags.
- Automatic rollback on failure.

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| project_name | Project name for naming resources | `string` | n/a | yes |
| service_role_arn | The ARN of the CodeDeploy service role | `string` | n/a | yes |
| ec2_tag_value | The tag value to identify the target EC2 instances | `string` | n/a | yes |

## Outputs

| Name | Description |
|------|-------------|
| app_name | The name of the CodeDeploy application |
| deployment_group_name | The name of the CodeDeploy deployment group |
