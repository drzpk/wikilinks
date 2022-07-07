resource "aws_batch_compute_environment" "generator" {
  compute_environment_name = "${var.prefix}GeneratorEnv"

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
    command     = []
    image       = "${aws_ecr_repository.generator.repository_url}:latest"
    environment = [
      {
        name  = "SCRIPT_LOCATION"
        value = "s3://${aws_s3_bucket.bucket.bucket}/generator/generator.sh"
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
        value = "2"
      },
      {
        type  = "MEMORY"
        value = tostring(1024 * 6)
      }
    ]
    fargatePlatformConfiguration = {
      platformVersion = "1.4.0"
    }
    executionRoleArn = aws_iam_role.batch_execution.arn
    jobRoleArn       = aws_iam_role.generator.arn
  })
}

resource "local_file" "generator_script" {
  filename = "${path.cwd}/scripts/processed/generator.sh"
  content  = templatefile("${path.cwd}/scripts/generator.sh.tftpl", {
    bucket   = aws_s3_bucket.bucket.bucket,
    fs_id    = aws_efs_file_system.fs.id,
    fs_ap_id = aws_efs_access_point.fs_root.id
  })
}
