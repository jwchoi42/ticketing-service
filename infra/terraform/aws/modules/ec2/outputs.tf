output "instance_public_ip" {
  description = "The Elastic IP address of the EC2 instance"
  value       = aws_eip.web.public_ip
}

output "instance_id" {
  description = "The ID of the EC2 instance"
  value       = aws_instance.web.id
}
