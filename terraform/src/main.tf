locals {
  project_name                = "WikiLinks"
  s3_common_options           = "compress=true&include-version-in-path=true"
  internal_s3_output_location = local.use_external_s3 ? "" : "s3://${aws_s3_bucket.links[0].bucket}/indexes?${local.s3_common_options}"
  external_s3_output_location = local.use_external_s3 ? "s3://${var.external_s3.bucket_name}/indexes?endpoint-host=${var.external_s3.endpoint_host}&${local.s3_common_options}" : ""
  s3_output_location          = local.use_external_s3 ? local.external_s3_output_location : local.internal_s3_output_location
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
    output_location          = length(module.full) == 1 ? "file:////data/databases" : local.s3_output_location
    current_version_location = length(module.full) == 1 ? "file:////data/databases" : ""
  }
  authentication_override = {
    access_key_id     = var.external_s3.access_key_id
    secret_access_key = var.external_s3.secret_access_key
  }
}

output "bastion_instance_ip" {
  value = length(aws_instance.bastion) == 1 ? aws_instance.bastion[0].public_ip : "<not available>"
}
