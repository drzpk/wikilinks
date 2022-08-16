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

variable "efs" {
  type = object({
    filesystem_id = string,
    access_point_id = string,
    policy_arn = string
  })
}

output "ecs_node_security_group_id" {
  value = aws_security_group.ecs_node.id
}
