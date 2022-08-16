resource "aws_iam_policy" "efs_access" {
  name   = "${local.prefix}EFSAccess"
  policy = jsonencode({
    Version   = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "elasticfilesystem:ClientMount",
          "elasticfilesystem:ClientRootAccess",
          "elasticfilesystem:ClientWrite",
          "elasticfilesystem:DescribeMountTargets"
        ]
        Resource = aws_efs_file_system.fs.arn
      }
    ]
  })
}

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

resource "aws_security_group" "efs" {
  name   = "${local.prefix}efs"
  vpc_id = aws_vpc.vpc.id

  ingress {
    from_port       = 2049
    protocol        = "tcp"
    to_port         = 2049
    security_groups = compact([
      module.batch.generator_security_group_id,
      length(module.full) > 0 ? module.full[0].ecs_node_security_group_id : "",
      aws_security_group.bastion.id
    ])
  }
}

resource "aws_iam_role" "bastion" {
  name_prefix = "${local.prefix}Bastion-"

  managed_policy_arns = [
    "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy",
    aws_iam_policy.efs_access.arn
  ]

  inline_policy {
    name   = "S3Access"
    policy = jsonencode({
      Version   = "2012-10-17"
      Statement = [
        {
          Effect   = "Allow"
          Action   = "s3:*"
          Resource = [
            aws_s3_bucket.links.arn,
            "${aws_s3_bucket.links.arn}/*"
          ]
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
          Service = [
            "ec2.amazonaws.com"
          ]
        }
      }
    ]
  })
}

resource "aws_security_group" "bastion" {
  name   = "${local.prefix}Bastion"
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

resource "aws_s3_bucket_acl" "links_bucket_acl" {
  bucket = aws_s3_bucket.links.id
  acl    = "private"
}
