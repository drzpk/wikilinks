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
  efs = {
    filesystem_id   = aws_efs_file_system.fs.id,
    access_point_id = aws_efs_access_point.fs_root.id,
    policy_arn      = aws_iam_policy.efs_access.arn
  }
  languages                 = var.languages
  versions                  = var.versions
  dev_tools                 = var.dev_tools
  project_name              = local.project_name
  owner                     = var.owner
  bastion_security_group_id = aws_security_group.bastion.id
}

module "batch" {
  source = "./shared/generator-batch-job"

  prefix  = local.prefix
  network = {
    vpc_id    = aws_vpc.vpc.id
    subnet_id = aws_subnet.public.id
  }
  efs = {
    filesystem_id   = aws_efs_file_system.fs.id,
    access_point_id = aws_efs_access_point.fs_root.id,
  }
  generator_options = {
    version                  = var.versions.generator,
    languages                = var.languages,
    output_location          = length(module.full) == 1 ? "file:////data/databases" : "s3://${aws_s3_bucket.links.bucket}/indexes?compress=true&include-version-in-path=true"
    current_version_location = length(module.full) == 1 ? "file:////data/databases" : ""
  }
}

output "bastion_instance_ip" {
  value = length(aws_instance.bastion) == 1 ? aws_instance.bastion[0].public_ip : "<not available>"
}
