module "vpc" {
  source       = "../../modules/vpc"
  project_name = var.project_name
  aws_region   = var.aws_region
}

module "iam" {
  source       = "../../modules/iam"
  project_name = var.project_name
}

module "security_group" {
  source       = "../../modules/security_group"
  project_name = var.project_name
  vpc_id       = module.vpc.vpc_id
}

module "ec2" {
  source                    = "../../modules/ec2"
  project_name              = var.project_name
  instance_type             = var.instance_type
  iam_instance_profile_name = module.iam.instance_profile_name
  subnet_id                 = module.vpc.public_subnet_id
  security_group_id         = module.security_group.security_group_id
}

module "codedeploy" {
  source           = "../../modules/codedeploy"
  project_name     = var.project_name
  service_role_arn = module.iam.codedeploy_role_arn
  ec2_tag_value    = "${var.project_name}-ec2"
}
