resource "aws_cloudwatch_event_rule" "generator" {
  name                = "${var.prefix}run-generator"
  description         = "Runs WikiLinks generator"
  schedule_expression = "cron(0 0 3 * ? *)"
}

resource "aws_cloudwatch_event_target" "generator" {
  target_id = "${var.prefix}generator"
  rule      = aws_cloudwatch_event_rule.generator.name
  arn       = aws_batch_job_queue.queue.arn
  role_arn  = aws_iam_role.event_bridge_generator_invoker.arn

  batch_target {
    job_definition = aws_batch_job_definition.generator.arn
    job_name       = "${var.prefix}Generator-scheduled"
  }
}
