output "instance_public_ip" {
  value = module.ec2.instance_public_ip
}

output "instance_id" {
  value = module.ec2.instance_id
}

output "vpc_id" {
  value = module.vpc.vpc_id
}

output "codedeploy_app_name" {
  value = module.codedeploy.app_name
}

output "codedeploy_deployment_group_name" {
  value = module.codedeploy.deployment_group_name
}
