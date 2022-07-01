locals {
  key_name       = "${var.prefix}generator-key"
  efs_mount_path = "/mnt/data"
}

resource "aws_instance" "dev" {
  count = var.dev_tools ? 1 : 0

  instance_type = "t3.micro"
  launch_template {
    id      = aws_launch_template.generator.id
    version = "$Latest"
  }
  tags = {
    Name = "${var.prefix}Dev"
  }
}

resource "aws_launch_template" "generator" {
  name                                 = "${var.prefix}Generator"
  instance_type                        = "t3.medium"
  image_id                             = data.aws_ami.ubuntu.image_id
  key_name                             = local.key_name
  instance_initiated_shutdown_behavior = "terminate"
  network_interfaces {
    subnet_id                   = aws_subnet.public.id
    associate_public_ip_address = true
    security_groups             = [aws_security_group.generator.id]
  }
  update_default_version = true
}

resource "aws_lambda_function" "updater" {
  function_name = "${var.prefix}updater"
  role          = aws_iam_role.updater.arn
  runtime       = "java11"
  memory_size   = 256
  handler       = "dev.drzepka.wikilinks.updater.LambdaHandler"
  filename      = "${path.root}/../../updater/build/libs/updater.jar"
  timeout       = 600

  environment {
    variables = {
      DATABASES_DIRECTORY = "${local.efs_mount_path}/databases"
      LAUNCH_TEMPLATE_ID  = aws_launch_template.generator.id
    }
  }

  vpc_config {
    security_group_ids = [aws_security_group.updater.id]
    subnet_ids         = [aws_subnet.public.id]
  }

  file_system_config {
    arn              = aws_efs_access_point.fs_root.arn
    local_mount_path = local.efs_mount_path
  }
}

data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
  }
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
  filename = "${path.module}/${local.key_name}.pem"
  content  = tls_private_key.private_key.private_key_pem
}

output "dev_instance_ip" {
  value = length(aws_instance.dev) > 0 ? aws_instance.dev[0].public_ip : "<not available>"
}
