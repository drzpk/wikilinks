variable "aws_region" {
  type    = string
  default = "eu-west-1"
}

variable "aws_profile" {
  type    = string
  default = "default"
}

variable "variant" {
  type        = string
  description = "Infrastructure variant: full or generator-only"
  default     = "full"
  validation {
    condition     = var.variant == "full" || var.variant == "generator-only"
    error_message = "Invalid variant name."
  }
}

variable "resource_name_prefix" {
  type        = string
  description = "Prefix prepend to resource names"
  default     = ""
  validation {
    condition     = length(var.resource_name_prefix) == 0 || length(var.resource_name_prefix) > 1 && substr(var.resource_name_prefix, -1, 1) == "-"
    error_message = "Resource name prefix must be empty or end with dash."
  }
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
    application = "latest"
    generator   = "latest"
  }
}

variable "languages" {
  type        = string
  description = "Wikipedia languages to generate indexes from, comma-separated."
  default     = "pl,en"
}

variable "external_s3" {
  type = object({
    endpoint_host     = string,
    bucket_name       = string,
    access_key_id     = string,
    secret_access_key = string
  })
  description = "If defined, the specified S3-compatible storage is used and local bucket is not created."
  default     = {
    endpoint_host     = "",
    bucket_name       = "",
    access_key_id     = "",
    secret_access_key = ""
  }
}

locals {
  prefix          = "${var.resource_name_prefix}wikilinks-"
  use_external_s3 = length(var.external_s3.endpoint_host) > 0 && length(var.external_s3.bucket_name) > 0
}
