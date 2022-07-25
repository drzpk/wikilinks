resource "aws_cloudwatch_log_group" "api" {
  name              = "/${local.prefix}wikilinks/api"
  retention_in_days = 7
}

resource "aws_cloudwatch_log_group" "application" {
  name              = "/${local.prefix}wikilinks/application"
  retention_in_days = 7
}
