output "app_name" {
  description = "The name of the CodeDeploy application"
  value       = aws_codedeploy_app.main.name
}

output "deployment_group_name" {
  description = "The name of the CodeDeploy deployment group"
  value       = aws_codedeploy_deployment_group.main.deployment_group_name
}
