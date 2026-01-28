variable "aws_region" {
  type        = string
  description = "The AWS region to deploy resources into"
  default     = "ap-northeast-2"
}

variable "project_name" {
  type        = string
  description = "Project name prefix following the 'tf-[project]-[env]' convention"
  default     = "tf-ticketing-prod"
}

variable "instance_type" {
  type        = string
  description = "EC2 instance type"
  default     = "t3.small"
}
