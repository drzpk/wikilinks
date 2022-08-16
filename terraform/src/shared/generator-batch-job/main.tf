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
    output_location          = string,
    current_version_location = string
  })
}

variable "efs" {
  type = object({
    filesystem_id   = string,
    access_point_id = string
  })
}

output "generator_role_id" {
  value = aws_iam_role.generator.id
}

output "generator_security_group_id" {
  value = aws_security_group.generator.id
}
