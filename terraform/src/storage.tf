resource "aws_efs_file_system" "fs" {
  creation_token   = "wikilinks-efs"
  performance_mode = "generalPurpose"

  lifecycle_policy {
    transition_to_ia = "AFTER_14_DAYS"
  }

  tags = {
    Name = "${var.prefix}WikiLinks"
  }
}

resource "aws_efs_access_point" "fs_root" {
  file_system_id = aws_efs_file_system.fs.id
  posix_user {
    gid = 0
    uid = 0
  }

  root_directory {
    path = "/"
    creation_info {
      owner_gid   = 0
      owner_uid   = 0
      permissions = "777"
    }
  }

  tags = {
    Name = "${var.prefix}-root"
  }
}

resource "aws_efs_mount_target" "public" {
  file_system_id  = aws_efs_file_system.fs.id
  subnet_id       = aws_subnet.public.id
  security_groups = [aws_security_group.efs.id]
}

resource "aws_s3_bucket" "bucket" {
  bucket_prefix = "${var.prefix}wikilinks-"
}

# todo: lifecycle policy
resource "aws_ecr_repository" "generator" {
  name                 = "${var.prefix}generator"
  image_tag_mutability = "MUTABLE"
  image_scanning_configuration {
    scan_on_push = true
  }
}

output "bucket_name" {
  value = aws_s3_bucket.bucket.bucket
}
