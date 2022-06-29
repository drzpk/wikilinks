resource "aws_iam_role" "bucket_client" {
  name_prefix = "${var.prefix}BucketClient-"

  inline_policy {
    name   = "GrantS3ReadOnlyAccess"
    policy = jsonencode({
      Version   = "2012-10-17"
      Statement = [
        {
          Effect = "Allow"
          Action = [
            "s3:Get*",
            "s3:List*"
          ]
          Resource = "${aws_s3_bucket.bucket.arn}/*"
        }
      ]
    })
  }

  assume_role_policy = jsonencode({
    Version   = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Action    = "sts:AssumeRole"
        Principal = {
          Service = "apigateway.amazonaws.com"
        }
      }
    ]
  })
}
