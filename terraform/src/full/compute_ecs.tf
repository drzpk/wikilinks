locals {
  ecs_application_container_name = "application"
  ecs_application_container_port = 8080
  key_name                       = "${var.prefix}ecs-node-key"
}

resource "aws_ecs_service" "application" {
  name            = "${var.prefix}application"
  cluster         = aws_ecs_cluster.cluster.id
  task_definition = aws_ecs_task_definition.application.arn
  desired_count   = 1

  // Kill old task before starting a new one, otherwise deployments will fail
  // if there's no enough CPU and memory for two instances of the application.
  deployment_minimum_healthy_percent = 0

  service_registries {
    registry_arn   = aws_service_discovery_service.application.arn
    container_name = local.ecs_application_container_name
    container_port = local.ecs_application_container_port
  }

  capacity_provider_strategy {
    capacity_provider = aws_ecs_capacity_provider.provider.name
    base              = 1
    weight            = 100
  }
}

resource "aws_ecs_cluster" "cluster" {
  name = "${var.prefix}Application"

  setting {
    name  = "containerInsights"
    value = "disabled"
  }
}

resource "aws_ecs_cluster_capacity_providers" "providers" {
  cluster_name       = aws_ecs_cluster.cluster.name
  capacity_providers = [aws_ecs_capacity_provider.provider.name]
}

resource "aws_ecs_capacity_provider" "provider" {
  name = "${var.prefix}app-provider"

  auto_scaling_group_provider {
    auto_scaling_group_arn         = aws_autoscaling_group.application.arn
    managed_termination_protection = "DISABLED"

    managed_scaling {
      status          = "ENABLED"
      target_capacity = 1
    }
  }
}

resource "aws_autoscaling_group" "application" {
  name             = "${var.prefix}application"
  min_size         = 0
  max_size         = 1
  desired_capacity = 0

  launch_template {
    id      = aws_launch_template.ecs_node.id
    version = "$Latest"
  }

  tag {
    key                 = "AmazonECSManaged"
    value               = true
    propagate_at_launch = true
  }

  tag {
    key                 = "Project"
    value               = var.project_name
    propagate_at_launch = true
  }

  lifecycle {
    ignore_changes = [
      desired_capacity
    ]
  }
}

resource "aws_launch_template" "ecs_node" {
  name                                 = "${var.prefix}ecs-node"
  instance_type                        = "t3.micro"
  image_id                             = data.aws_ssm_parameter.ecs_optimized_ami.value
  key_name                             = local.key_name
  instance_initiated_shutdown_behavior = "terminate"

  user_data = base64encode(<<-EOT
    #!/bin/bash
    echo ECS_CLUSTER=${aws_ecs_cluster.cluster.name} >> /etc/ecs/ecs.config
  EOT
  )

  iam_instance_profile {
    arn = aws_iam_instance_profile.ecs_node.arn
  }

  network_interfaces {
    subnet_id                   = var.network.subnet_id
    associate_public_ip_address = true
    security_groups             = [aws_security_group.ecs_node.id]
  }

  tag_specifications {
    resource_type = "instance"
    tags          = {
      Owner = var.owner
      Name  = "${var.prefix}ecs-node"
    }
  }
}

resource "aws_ecs_task_definition" "application" {
  family             = "${var.prefix}Application"
  network_mode       = "bridge"
  execution_role_arn = aws_iam_role.ecs_execution.arn
  task_role_arn      = aws_iam_role.application.arn

  container_definitions = jsonencode([
    {
      name        = local.ecs_application_container_name
      image       = "ghcr.io/drzpk/wikilinks/application-jvm:${var.versions.application}",
      memory      = 768
      cpu         = 2048
      essential   = true
      environment = [
        {
          name  = "DATABASES_DIRECTORY"
          value = "/data/databases"
        }
      ]
      portMappings = [
        {
          containerPort = local.ecs_application_container_port
          hostPort      = 0
        }
      ]
      mountPoints = [
        {
          sourceVolume  = "data"
          containerPath = "/data"
          readOnly      = false
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options   = {
          awslogs-group  = aws_cloudwatch_log_group.application.name
          awslogs-region = data.aws_region.region.name
        }
      }
    }
  ])

  volume {
    name = "data"

    efs_volume_configuration {
      file_system_id     = aws_efs_file_system.fs.id
      transit_encryption = "ENABLED"
      authorization_config {
        access_point_id = aws_efs_access_point.fs_root.id
        iam             = "ENABLED"
      }
    }
  }
}

resource "aws_service_discovery_service" "application" {
  name = "${var.prefix}application"

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.namespace.id

    dns_records {
      ttl  = 10
      type = "SRV"
    }

    routing_policy = "MULTIVALUE"
  }
}

resource "aws_service_discovery_private_dns_namespace" "namespace" {
  name = "${var.prefix}application.local"
  vpc  = var.network.vpc_id
}

resource "aws_iam_instance_profile" "ecs_node" {
  name = "${var.prefix}ECSNode"
  role = aws_iam_role.ecs_ec2_node.name
}

resource "tls_private_key" "private_key" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "aws_key_pair" "key_pair" {
  key_name   = local.key_name
  public_key = tls_private_key.private_key.public_key_openssh
}

resource "local_file" "key_file" {
  filename = "${path.root}/${local.key_name}.pem"
  content  = tls_private_key.private_key.private_key_pem
}

data "aws_ssm_parameter" "ecs_optimized_ami" {
  name = "/aws/service/ecs/optimized-ami/amazon-linux-2/recommended/image_id"
}

data "aws_region" "region" {}
