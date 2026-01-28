variable "project_name" {
  type        = string
  description = "Project name for resource tagging"
}

variable "instance_type" {
  type        = string
  description = "EC2 instance type"
}

variable "iam_instance_profile_name" {
  type        = string
  description = "The name of the IAM instance profile for SSM"
}

variable "subnet_id" {
  type        = string
  description = "The subnet ID to launch the instance in"
}

variable "security_group_id" {
  type        = string
  description = "The ID of the security group to attach to the instance"
}
