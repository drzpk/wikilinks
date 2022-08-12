resource "aws_iam_role" "generator" {
  name_prefix = "${var.prefix}Generator-"

  managed_policy_arns = [
    "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
  ]

  dynamic "inline_policy" {
    for_each = local.efs_defined ? toset([1]) : toset([])
    content {
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
            Resource = var.efs.filesystem_arn
          }
        ]
      })
    }
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

resource "aws_iam_role" "ecs_execution" {
  name_prefix = "${var.prefix}GeneratorBatchExec-"

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

resource "aws_iam_role" "event_bridge_generator_invoker" {
  name_prefix = "${var.prefix}EvtBridgeGenInvoker-"

  inline_policy {
    name   = "AllowInvokingGeneratorBatchJob"
    policy = jsonencode({
      Version   = "2012-10-17"
      Statement = [
        {
          Effect   = "Allow"
          Action   = "batch:SubmitJob"
          Resource = [
            aws_batch_job_definition.generator.arn,
            aws_batch_job_queue.queue.arn
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
            "events.amazonaws.com"
          ]
        }
      }
    ]
  })
}

resource "aws_security_group" "generator" {
  name   = "${var.prefix}generator"
  vpc_id = var.network.vpc_id

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
