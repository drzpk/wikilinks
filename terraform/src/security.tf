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

resource "aws_security_group" "generator" {
  name   = "${var.prefix}generator"
  vpc_id = aws_vpc.vpc.id

  ingress {
    from_port   = 22
    protocol    = "tcp"
    to_port     = 22
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    protocol    = "-1"
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_security_group" "efs" {
  name = "${var.prefix}efs"
  vpc_id = aws_vpc.vpc.id

  ingress {
    from_port   = 2049
    protocol    = "tcp"
    to_port     = 2049
    security_groups = [aws_security_group.generator.id]
  }
}
