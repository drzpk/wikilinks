resource "aws_s3_bucket" "links" {
  count  = local.use_external_s3 ? 0 : 1
  bucket = "${local.prefix}index-storage"
}

resource "aws_efs_file_system" "fs" {
  creation_token   = "wikilinks-efs"
  performance_mode = "generalPurpose"

  lifecycle_policy {
    transition_to_ia = "AFTER_14_DAYS"
  }

  tags = {
    Name = "${local.prefix}WikiLinks"
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
    Name = "${local.prefix}root"
  }
}

resource "aws_efs_mount_target" "public" {
  file_system_id  = aws_efs_file_system.fs.id
  subnet_id       = aws_subnet.public.id
  security_groups = [aws_security_group.efs.id]
}
