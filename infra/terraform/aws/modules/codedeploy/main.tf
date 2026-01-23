/**
 * # CodeDeploy Module
 * 
 * This module manages the AWS CodeDeploy Application and Deployment Group.
 */

resource "aws_codedeploy_app" "main" {

  compute_platform = "Server"
  name             = "${var.project_name}-app"

  tags = {
    ManagedBy = "Terraform"
  }
}

resource "aws_codedeploy_deployment_group" "main" {

  app_name              = aws_codedeploy_app.main.name
  deployment_group_name = "${var.project_name}-dg"
  service_role_arn      = var.service_role_arn

  deployment_config_name = "CodeDeployDefault.AllAtOnce"

  ec2_tag_set {
    ec2_tag_filter {
      key   = "Name"
      type  = "KEY_AND_VALUE"
      value = var.ec2_tag_value
    }
  }

  auto_rollback_configuration {
    enabled = true
    events  = ["DEPLOYMENT_FAILURE"]
  }

  tags = {
    ManagedBy = "Terraform"
  }
}
