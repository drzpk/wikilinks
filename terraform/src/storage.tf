resource "aws_efs_file_system" "fs" {
  creation_token = "wikilinks-efs"
  performance_mode = "generalPurpose"

  lifecycle_policy {
    transition_to_ia = "AFTER_14_DAYS"
  }

  tags = {
    Name = "${var.prefix}WikiLinks"
  }
}

resource "aws_s3_bucket" "bucket" {
  bucket_prefix = "${var.prefix}wikilinks-"
}

output "bucket_name" {
  value = aws_s3_bucket.bucket.bucket
}