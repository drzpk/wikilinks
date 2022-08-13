locals {
  project_name = "WikiLinks"
}

provider "aws" {
  region  = var.aws_region
  profile = var.aws_profile

  default_tags {
    tags = {
      Owner   = var.owner
      Project = local.project_name
    }
  }
}

module "full" {
  count  = var.variant == "full" ? 1 : 0
  source = "./full"

  prefix  = local.prefix
  network = {
    vpc_id    = aws_vpc.vpc.id
    subnet_id = aws_subnet.public.id
  }
  languages                 = var.languages
  versions                  = var.versions
  dev_tools                 = var.dev_tools
  project_name              = local.project_name
  owner                     = var.owner
  bastion_security_group_id = module.bastion.security_group_id
}

module "generator_only" {
  count  = var.variant == "generator-only" ? 1 : 0
  source = "./generator-only"

  prefix  = local.prefix
  network = {
    vpc_id    = aws_vpc.vpc.id
    subnet_id = aws_subnet.public.id
  }
  languages         = var.languages
  generator_version = var.versions.generator
}

module "bastion" {
  source = "./shared/bastion"

  prefix  = local.prefix
  network = {
    vpc_id    = aws_vpc.vpc.id
    subnet_id = aws_subnet.public.id
  }
  efs_arn         = length(module.full) == 1 ? module.full[0].efs_arn : ""
  s3bucket_arn    = length(module.generator_only) == 1 ? module.generator_only[0].s3bucket_arn : ""
  create_instance = var.dev_tools
}

output "bastion_instance_ip" {
  value = module.bastion.instance_ip
}
