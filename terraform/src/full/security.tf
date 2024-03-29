resource "aws_iam_role" "ecs_execution" {
  name_prefix = "${var.prefix}ApplicationExec-"

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

resource "aws_iam_role" "ecs_ec2_node" {
  name_prefix = "${var.prefix}ECSNode-"

  managed_policy_arns = [
    "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role"
  ]

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

resource "aws_iam_role" "application" {
  name_prefix = "${var.prefix}Application-"

  managed_policy_arns = [
    "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy",
    var.efs.policy_arn
  ]

  assume_role_policy = jsonencode({
    Version   = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Action    = "sts:AssumeRole"
        Principal = {
          Service = [
            "ecs-tasks.amazonaws.com"
          ]
        }
      }
    ]
  })
}

resource "aws_security_group" "ecs_node" {
  name   = "${var.prefix}ECS-node"
  vpc_id = var.network.vpc_id

  ingress {
    from_port       = 22
    protocol        = "tcp"
    to_port         = 22
    security_groups = [var.bastion_security_group_id]
  }

  ingress {
    from_port       = 0
    protocol        = "-1"
    to_port         = 0
    security_groups = [aws_security_group.api_link.id]
  }

  egress {
    from_port   = 0
    protocol    = "-1"
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "api_link" {
  name   = "${var.prefix}api-link"
  vpc_id = var.network.vpc_id
}

resource "aws_security_group_rule" "api_link_egress_rule" {
  security_group_id        = aws_security_group.api_link.id
  type                     = "egress"
  from_port                = 0
  protocol                 = "-1"
  to_port                  = 0
  source_security_group_id = aws_security_group.ecs_node.id
}
