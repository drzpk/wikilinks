resource "aws_cloudwatch_log_group" "api" {
  name              = "/${var.prefix}wikilinks/api"
  retention_in_days = 7
}

resource "aws_cloudwatch_log_group" "application" {
  name              = "/${var.prefix}wikilinks/application"
  retention_in_days = 7
}
