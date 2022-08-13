resource "aws_s3_bucket" "links" {
  bucket = "${var.prefix}index-storage"
}
