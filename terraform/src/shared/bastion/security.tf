resource "aws_iam_role" "bastion" {
  name_prefix = "${var.prefix}Bastion-"

  managed_policy_arns = [
    "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
  ]

  dynamic "inline_policy" {
    for_each = length(var.efs_arn) > 0 ? toset([1]) : toset([])
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
            Resource = var.efs_arn
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
          Service = [
            "ec2.amazonaws.com"
          ]
        }
      }
    ]
  })
}

resource "aws_security_group" "bastion" {
  name   = "${var.prefix}Bastion"
  vpc_id = var.network.vpc_id

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
