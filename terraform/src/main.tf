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
