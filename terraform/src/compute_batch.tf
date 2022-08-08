locals {
  generator_job_cpu_count = 2
}

resource "aws_batch_compute_environment" "generator" {
  compute_environment_name = "${local.prefix}GeneratorEnv"

  compute_resources {
    type = "FARGATE_SPOT"

    security_group_ids = [aws_security_group.generator.id]
    subnets            = [aws_subnet.public.id]

    max_vcpus = 2
  }

  service_role = aws_iam_role.batch_environment.arn
  type         = "MANAGED"
}

resource "aws_batch_job_queue" "queue" {
  name                 = "${local.prefix}Generator"
  state                = "ENABLED"
  priority             = 1
  compute_environments = [
    aws_batch_compute_environment.generator.arn
  ]
}

resource "aws_batch_job_definition" "generator" {
  name                  = "${local.prefix}Generator"
  type                  = "container"
  platform_capabilities = ["FARGATE"]

  container_properties = jsonencode({
    command     = ["language=${var.languages}"]
    image       = "ghcr.io/drzpk/wikilinks/generator:${var.versions.generator}"
    environment = [
      {
        name  = "DATABASES_DIRECTORY"
        value = "/data/databases"
      },
      {
        name  = "WORKING_DIRECTORY"
        value = "/data/dumps"
      },
      {
        name  = "BATCH_MODE"
        value = "true"
      },
      {
        name  = "JAVA_TOOL_OPTIONS"
        value = "-XX:+UseContainerSupport -XX:MaxRAMPercentage=80.0 -XX:ActiveProcessorCount=${local.generator_job_cpu_count}"
      }
    ]
    volumes = [
      {
        name                   = "data"
        efsVolumeConfiguration = {
          fileSystemId        = aws_efs_file_system.fs.id
          authorizationConfig = {
            accessPointId = aws_efs_access_point.fs_root.id
            iam           = "ENABLED"
          }
          rootDirectory     = "/"
          transitEncryption = "ENABLED"
        }
      }
    ]
    mountPoints = [
      {
        sourceVolume  = "data"
        containerPath = "/data"
      }
    ]
    networkConfiguration = {
      assignPublicIp = "ENABLED"
    }
    resourceRequirements = [
      {
        type  = "VCPU"
        value = tostring(local.generator_job_cpu_count)
      },
      {
        type  = "MEMORY"
        value = tostring(1024 * 6)
      }
    ]
    fargatePlatformConfiguration = {
      platformVersion = "1.4.0"
    }
    executionRoleArn = aws_iam_role.ecs_execution.arn
    jobRoleArn       = aws_iam_role.generator.arn
  })

  timeout {
    // 2.5h for each language
    attempt_duration_seconds = 150 * 60 * length(split(",", var.languages))
  }
}
