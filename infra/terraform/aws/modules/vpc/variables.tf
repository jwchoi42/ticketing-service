variable "project_name" {
  type        = string
  description = "Project name to be used for tagging and resource naming"
}

variable "vpc_cidr" {
  type        = string
  description = "The CIDR block for the VPC"
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidr" {
  type        = string
  description = "The CIDR block for the public subnet"
  default     = "10.0.1.0/24"
}

variable "aws_region" {
  type        = string
  description = "The AWS region to deploy resources into"
}
