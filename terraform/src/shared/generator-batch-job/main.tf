variable "prefix" {
  type = string
}

variable "network" {
  type = object({
    vpc_id    = string,
    subnet_id = string
  })
}

variable "generator_options" {
  type = object({
    version                  = string,
    languages                = string,
    working_directory        = string,
    output_location          = string,
    current_version_location = string
  })
}

variable "efs" {
  type = object({
    filesystem_id   = string,
    access_point_id = string,
    container_path  = string
  })
  default = {
    filesystem_id   = "",
    access_point_id = "",
    container_path  = ""
  }
}

locals {
  efs_defined = length(var.efs.filesystem_id) > 0 && length(var.efs.access_point_id) > 0
}

output "generator_role_id" {
  value = aws_iam_role.generator.id
}

output "generator_security_group_id" {
  value = aws_security_group.generator.id
}