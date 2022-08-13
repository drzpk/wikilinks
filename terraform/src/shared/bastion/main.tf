variable "prefix" {
  type = string
}

variable "network" {
  type = object({
    vpc_id    = string
    subnet_id = string
  })
}

variable "create_instance" {
  type = bool
}

variable "efs_arn" {
  type    = string
  default = ""
}

variable "s3bucket_arn" {
  type = string
  default = ""
}

output "security_group_id" {
  value = aws_security_group.bastion.id
}
