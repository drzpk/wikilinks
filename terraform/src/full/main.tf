variable "prefix" {
  type = string
}

variable "versions" {
  type = object({
    application = string
    generator   = string
  })
}

variable "network" {
  type = object({
    vpc_id    = string,
    subnet_id = string
  })
}

variable "languages" {
  type = string
}

variable "dev_tools" {
  type = bool
}

variable "project_name" {
  type = string
}

variable "owner" {
  type = string
}

variable "bastion_security_group_id" {
  type = string
}

module "batch" {
  source = "../shared/generator-batch-job"

  prefix  = var.prefix
  network = var.network
  efs     = {
    filesystem_id   = aws_efs_file_system.fs.id,
    access_point_id = aws_efs_access_point.fs_root.id,
    container_path  = "/data"
  }
  generator_options = {
    version                  = var.versions.generator,
    languages                = var.languages,
    working_directory        = "/data/dumps",
    output_location          = "file:////data/databases"
    current_version_location = "file:////data/databases"
  }
}

output "efs_arn" {
  value = aws_efs_file_system.fs.arn
}
