variable "aws_region" {
  type    = string
  default = "eu-west-1"
}

variable "aws_profile" {
  type    = string
  default = "default"
}

variable "prefix" {
  type        = string
  description = "Prefix prepend to resource names"
  default     = ""
}

variable "owner" {
  type    = string
  default = ""
}

variable "dev_tools" {
  type        = bool
  description = "Whether provision additional resources or enable some settings for development purposes"
  default     = false
}

variable "versions" {
  type = object({
    application = string
    generator   = string
  })
  description = "Image versions of WikiLinks modules to use"
  default     = {
    application = "1.0-snapshot"
    generator   = "1.0-snapshot"
  }
}
