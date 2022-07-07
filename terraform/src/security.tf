resource "aws_iam_role" "api_gateway" {
  name_prefix = "${var.prefix}APIGateway-"

  inline_policy {
    name   = "S3FrontendApp"
    policy = jsonencode({
      Version   = "2012-10-17"
      Statement = [
        {
          Effect = "Allow"
          Action = [
            "s3:Get*",
            "s3:List*"
          ]
          Resource = "${aws_s3_bucket.bucket.arn}/frontend/*"
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

resource "aws_iam_role" "generator" {
  name_prefix = "${var.prefix}Generator-"

  managed_policy_arns = [
    "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
  ]

  inline_policy {
    name   = "EFSAccess"
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

  inline_policy {
    name   = "S3GeneratorApp"
    policy = jsonencode({
      Version   = "2012-10-17"
      Statement = [
        {
          Effect = "Allow"
          Action = [
            "s3:Get*",
            "s3:List*"
          ]
          Resource = [
            aws_s3_bucket.bucket.arn,
            "${aws_s3_bucket.bucket.arn}/generator",
            "${aws_s3_bucket.bucket.arn}/generator/*"
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
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role" "batch_environment" {
  name_prefix = "${var.prefix}GeneratorBatch-"

  managed_policy_arns = [
    "arn:aws:iam::aws:policy/service-role/AWSBatchServiceRole"
  ]

  assume_role_policy = jsonencode({
    Version   = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Action    = "sts:AssumeRole"
        Principal = {
          Service = "batch.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role" "batch_execution" {
  name_prefix = "${var.prefix}GeneratorBatchExecution-"

  managed_policy_arns = [
    "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
  ]

  assume_role_policy = jsonencode({
    Version   = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Action    = "sts:AssumeRole"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role" "application" {
  name_prefix = "${var.prefix}Application-"

  managed_policy_arns = [
    "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
  ]

  inline_policy {
    name   = "EFSAccess"
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

  inline_policy {
    name   = "S3Application"
    policy = jsonencode({
      Version   = "2012-10-17"
      Statement = [
        {
          Effect = "Allow"
          Action = [
            "s3:Get*",
            "s3:List*"
          ]
          Resource = [
            aws_s3_bucket.bucket.arn,
            "${aws_s3_bucket.bucket.arn}/application/",
            "${aws_s3_bucket.bucket.arn}/application/*",
            "${aws_s3_bucket.bucket.arn}/updater/",
            "${aws_s3_bucket.bucket.arn}/updater/*"
          ]
        }
      ]
    })
  }

  inline_policy {
    name   = "EC2GeneratorControl"
    policy = jsonencode({
      Version   = "2012-10-17"
      Statement = [
        {
          Effect   = "Allow"
          Action   = "ec2:DescribeInstances"
          Resource = "*"
        },
        {
          Effect = "Allow"
          Action = [
            "ec2:RunInstances",
            "ec2:CreateTags",
            "ec2:TerminateInstances"
          ]
          Resource  = "*"
          Condition = {
            StringEquals = {
              "ec2:LaunchTemplate" = aws_launch_template.generator.arn
            }
          }
        },
        {
          Effect = "Allow"
          Action = [
            "ec2:RunInstances",
            "ec2:CreateTags",
            "ec2:TerminateInstances"
          ]
          Resource  = "*"
          Condition = {
            StringLike = {
              "ec2:ResourceTag/GeneratorRunner" = "*"
            }
          }
        },
        {
          // Copied from the policy AutoScalingServiceRolePolicy,
          // required for launching EC2 instances
          "Sid" : "EC2InstanceProfileManagement",
          "Effect" : "Allow",
          "Action" : [
            "iam:PassRole"
          ],
          "Resource" : "*",
          "Condition" : {
            "StringLike" : {
              "iam:PassedToService" : "ec2.amazonaws.com*"
            }
          }
        },
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
          Service = "ec2.amazonaws.com"
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

resource "aws_security_group" "application" {
  name   = "${var.prefix}Application"
  vpc_id = aws_vpc.vpc.id

  ingress {
    # todo: this rule should probably be removed after testing
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
}

resource "aws_security_group" "efs" {
  name   = "${var.prefix}efs"
  vpc_id = aws_vpc.vpc.id

  ingress {
    from_port       = 2049
    protocol        = "tcp"
    to_port         = 2049
    security_groups = [
      aws_security_group.generator.id,
      aws_security_group.application.id
    ]
  }
}

resource "aws_ecr_repository_policy" "generator" {
  repository = aws_ecr_repository.generator.name
  policy     = jsonencode({
    Version   = "2008-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal : {
          AWS = data.aws_caller_identity.id.user_id
        }
        Action = [
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "ecr:BatchCheckLayerAvailability",
          "ecr:PutImage",
          "ecr:InitiateLayerUpload",
          "ecr:UploadLayerPart",
          "ecr:CompleteLayerUpload",
          "ecr:DescribeRepositories",
          "ecr:GetRepositoryPolicy",
          "ecr:ListImages",
          "ecr:DeleteRepository",
          "ecr:BatchDeleteImage",
          "ecr:SetRepositoryPolicy",
          "ecr:DeleteRepositoryPolicy"
        ]
      },
      {
        Sid = "AWSBatchAccess"
        Effect = "Allow"
        Principal = {
          Service = "batch.amazonaws.com"
        }
        Action = [
          "ecr:BatchGetImage",
          "ecr:GetDownloadUrlForLayer"
        ]
      }
    ]
  })
}

data "aws_caller_identity" "id" {}
