resource "aws_apigatewayv2_api" "api" {
  name          = "${var.prefix}api"
  protocol_type = "HTTP"

}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.api.id
  name        = "$default"
  auto_deploy = true

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.api.arn
    format          = "$context.identity.sourceIp - - [$context.requestTime] \"$context.httpMethod $context.routeKey $context.protocol\" $context.status $context.responseLength $context.requestId $context.integrationErrorMessage"
  }
}

resource "aws_apigatewayv2_integration" "proxy" {
  api_id               = aws_apigatewayv2_api.api.id
  integration_type     = "HTTP_PROXY"
  integration_method   = "GET"
  integration_uri      = aws_service_discovery_service.application.arn
  passthrough_behavior = "WHEN_NO_MATCH"

  connection_type = "VPC_LINK"
  connection_id   = aws_apigatewayv2_vpc_link.link.id
}

resource "aws_apigatewayv2_route" "root" {
  api_id    = aws_apigatewayv2_api.api.id
  route_key = "$default"
  target    = "integrations/${aws_apigatewayv2_integration.proxy.id}"
}

resource "aws_apigatewayv2_vpc_link" "link" {
  name               = "${var.prefix}link"
  security_group_ids = [aws_security_group.api_link.id]
  subnet_ids         = [aws_subnet.public.id]
}