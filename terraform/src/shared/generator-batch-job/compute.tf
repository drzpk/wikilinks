locals {
  generator_job_cpu_count = 2
}

resource "aws_batch_compute_environment" "generator" {
  compute_environment_name = "${var.prefix}GeneratorEnv"

  compute_resources {
    type = "FARGATE_SPOT"

    security_group_ids = [aws_security_group.generator.id]
    subnets            = [var.network.subnet_id]

    max_vcpus = 2
  }

  service_role = aws_iam_role.batch_environment.arn
  type         = "MANAGED"
}

resource "aws_batch_job_queue" "queue" {
  name                 = "${var.prefix}Generator"
  state                = "ENABLED"
  priority             = 1
  compute_environments = [
    aws_batch_compute_environment.generator.arn
  ]
}

resource "aws_batch_job_definition" "generator" {
  name                  = "${var.prefix}Generator"
  type                  = "container"
  platform_capabilities = ["FARGATE"]

  container_properties = jsonencode({
    command     = ["language=${var.generator_options.languages}"]
    image       = "ghcr.io/drzpk/wikilinks/generator:${var.generator_options.version}"
    environment = [
      {
        name  = "WORKING_DIRECTORY"
        value = "/data/dumps"
      },
      {
        name  = format("OUTPUT_LOCATION%s", length(var.generator_options.output_location) == 0 ? "_DISABLED" : "")
        value = var.generator_options.output_location
      },
      {
        name  = format("CURRENT_VERSION_LOCATION%s", length(var.generator_options.current_version_location) == 0 ? "_DISABLED" : "")
        value = var.generator_options.current_version_location
      },
      {
        name  = "BATCH_MODE"
        value = "true"
      },
      {
        name  = "JAVA_TOOL_OPTIONS"
        value = "-XX:+UseContainerSupport -XX:MaxRAMPercentage=80.0 -XX:ActiveProcessorCount=${local.generator_job_cpu_count}"
      },
      {
        name = format("AWS_ACCESS_KEY_ID%s", length(var.authentication_override.access_key_id) == 0 ? "_DISABLED" : "")
        value = var.authentication_override.access_key_id
      },
      {
        name = format("AWS_SECRET_ACCESS_KEY%s", length(var.authentication_override.secret_access_key) == 0 ? "_DISABLED" : "")
        value = var.authentication_override.secret_access_key
      }
    ]
    volumes = [
      {
        name                   = "data"
        efsVolumeConfiguration = {
          fileSystemId        = var.efs.filesystem_id
          authorizationConfig = {
            accessPointId = var.efs.access_point_id
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
    attempt_duration_seconds = 150 * 60 * length(split(",", var.generator_options.languages))
  }
}
