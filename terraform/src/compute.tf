locals {
  key_name = "${var.prefix}ec2-key"
}

resource "aws_instance" "bastion" {
  count = var.dev_tools ? 1 : 0

  instance_type               = "t3.micro"
  ami                         = data.aws_ami.amazon_linux.id
  subnet_id                   = aws_subnet.public.id
  vpc_security_group_ids      = [aws_security_group.bastion.id]
  iam_instance_profile        = aws_iam_instance_profile.bastion.name
  associate_public_ip_address = true
  key_name                    = local.key_name

  user_data = base64encode(templatefile("${path.cwd}/scripts/application.sh.tftpl", {
    bucket   = aws_s3_bucket.bucket.bucket,
    fs_id    = aws_efs_file_system.fs.id,
    fs_ap_id = aws_efs_access_point.fs_root.id
  }))

  tags = {
    Name = "${var.prefix}Bastion"
  }
}

resource "aws_iam_instance_profile" "bastion" {
  name = "${var.prefix}Bastion"
  // Use the same role as the application
  role = aws_iam_role.application.name
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

output "bastion_instance_ip" {
  value = length(aws_instance.bastion) > 0 ? aws_instance.bastion[0].public_ip : "<not available>"
}
