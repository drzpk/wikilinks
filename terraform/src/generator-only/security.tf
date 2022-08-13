resource "aws_iam_role_policy" "generator_s3_access" {
  name   = "S3Access"
  role   = module.batch.generator_role_id
  policy = jsonencode({
    Version   = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = "s3:ListBucket"
        Resource = aws_s3_bucket.links.arn
      },
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject"
        ],
        Resource = "${aws_s3_bucket.links.arn}/*"
      }
    ]
  })
}

resource "aws_s3_bucket_acl" "links_bucket_acl" {
  bucket = aws_s3_bucket.links.id
  acl    = "private"
}
