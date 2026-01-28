output "instance_profile_name" {
  description = "The name of the IAM instance profile for EC2"
  value       = aws_iam_instance_profile.ec2_profile.name
}

output "codedeploy_role_arn" {
  description = "The ARN of the IAM role for CodeDeploy service"
  value       = aws_iam_role.codedeploy_role.arn
}
