locals {
  key_name       = "${var.prefix}generator-key"
  efs_mount_path = "/mnt/data"
}

resource "aws_instance" "application" {
  instance_type               = "t3.micro"
  ami                         = data.aws_ami.amazon_linux.id
  subnet_id                   = aws_subnet.public.id
  vpc_security_group_ids      = [aws_security_group.application.id]
  iam_instance_profile        = aws_iam_instance_profile.application.name
  associate_public_ip_address = true
  key_name                    = local.key_name

  user_data = base64encode(templatefile("${path.cwd}/scripts/application.sh.tftpl", {
    bucket             = aws_s3_bucket.bucket.bucket,
    fs_id              = aws_efs_file_system.fs.id,
    fs_ap_id           = aws_efs_access_point.fs_root.id
    launch_template_id = aws_launch_template.generator.id
  }))

  tags = {
    Name = "${var.prefix}Application"
  }
}

resource "aws_instance" "dev" {
  count = var.dev_tools ? 1 : 0

  instance_type = "t3.micro"
  launch_template {
    id      = aws_launch_template.generator.id
    version = "$Latest"
  }
  user_data = ""
  tags      = {
    Name = "${var.prefix}Dev"
  }
}

resource "aws_launch_template" "generator" {
  name                                 = "${var.prefix}Generator"
  instance_type                        = "t3.medium"
  image_id                             = data.aws_ami.amazon_linux.image_id
  key_name                             = local.key_name
  instance_initiated_shutdown_behavior = "terminate"
  iam_instance_profile {
    arn = aws_iam_instance_profile.generator.arn
  }
  network_interfaces {
    subnet_id                   = aws_subnet.public.id
    associate_public_ip_address = true
    security_groups             = [aws_security_group.generator.id]
  }
  update_default_version = true

  user_data = base64encode(templatefile("${path.cwd}/scripts/generator.sh.tftpl", {
    bucket   = aws_s3_bucket.bucket.bucket,
    fs_id    = aws_efs_file_system.fs.id,
    fs_ap_id = aws_efs_access_point.fs_root.id
  }))
  tag_specifications {
    resource_type = "instance"
    tags          = {
      Name            = "${var.prefix}GeneratorRunner"
      Owner           = var.owner
      GeneratorRunner = ""
    }
  }
}

resource "aws_iam_instance_profile" "application" {
  name = "${var.prefix}Application"
  role = aws_iam_role.application.name
}

resource "aws_iam_instance_profile" "generator" {
  name = "${var.prefix}Generator"
  role = aws_iam_role.generator.name
}

data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["137112412989"] # Amazon

  filter {
    name   = "name"
    values = ["amzn2-ami-kernel-5.10-hvm-2.0.*"]
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
