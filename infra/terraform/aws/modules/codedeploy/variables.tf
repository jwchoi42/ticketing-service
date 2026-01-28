variable "project_name" {
  type        = string
  description = "Project name for naming resources"
}

variable "service_role_arn" {
  type        = string
  description = "The ARN of the CodeDeploy service role"
}

variable "ec2_tag_value" {
  type        = string
  description = "The tag value to identify the target EC2 instances"
}
