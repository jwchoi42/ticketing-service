variable "project_name" {
  type        = string
  description = "Project name for naming the security group"
}

variable "vpc_id" {
  type        = string
  description = "The ID of the VPC where the security group will be created"
}
