variable "prefix" {
  type = string
}

variable "generator_version" {
  type = string
}

variable "languages" {
  type = string
}

variable "network" {
  type = object({
    vpc_id    = string,
    subnet_id = string
  })
}

module "batch" {
  source = "../shared/generator-batch-job"

  prefix            = var.prefix
  network           = var.network
  generator_options = {
    version                  = var.generator_version,
    languages                = var.languages,
    working_directory        = "/dumps",
    output_location          = "s3://${aws_s3_bucket.links.bucket}/indexes?compress=true&include-version-in-path=true"
    current_version_location = ""
  }
}

output "s3bucket_arn" {
  value = aws_s3_bucket.links.arn
}
