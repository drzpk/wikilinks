resource "aws_api_gateway_rest_api" "api" {
  name               = "${var.prefix}WikiLinks"
  binary_media_types = [
    "image/jpeg",
    "image/png",
    "image/*"
  ]
  endpoint_configuration {
    types = ["REGIONAL"]
  }
}

resource "aws_api_gateway_resource" "app" {
  path_part   = "app"
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  rest_api_id = aws_api_gateway_rest_api.api.id
}

resource "aws_api_gateway_resource" "s3_proxy" {
  path_part   = "{object+}"
  parent_id   = aws_api_gateway_resource.app.id
  rest_api_id = aws_api_gateway_rest_api.api.id
}

resource "aws_api_gateway_method" "root_get" {
  authorization = "NONE"
  http_method   = "GET"
  resource_id   = aws_api_gateway_rest_api.api.root_resource_id
  rest_api_id   = aws_api_gateway_rest_api.api.id
}

resource "aws_api_gateway_method" "app_get" {
  authorization = "NONE"
  http_method   = "GET"
  resource_id   = aws_api_gateway_resource.app.id
  rest_api_id   = aws_api_gateway_rest_api.api.id
}

resource "aws_api_gateway_method" "frontend_get" {
  authorization      = "NONE"
  http_method        = "GET"
  resource_id        = aws_api_gateway_resource.s3_proxy.id
  rest_api_id        = aws_api_gateway_rest_api.api.id
  request_parameters = {
    "method.request.path.object" = true
  }
}

resource "aws_api_gateway_integration" "root_redirect" {
  http_method       = aws_api_gateway_method.root_get.http_method
  resource_id       = aws_api_gateway_rest_api.api.root_resource_id
  rest_api_id       = aws_api_gateway_rest_api.api.id
  type              = "MOCK"
  request_templates = {
    "application/json" = "{ \"statusCode\": 302 }"
  }
}

resource "aws_api_gateway_integration" "app_redirect" {
  http_method       = aws_api_gateway_method.app_get.http_method
  resource_id       = aws_api_gateway_resource.app.id
  rest_api_id       = aws_api_gateway_rest_api.api.id
  type              = "MOCK"
  request_templates = {
    "application/json" = "{ \"statusCode\": 302 }"
  }
}

resource "aws_api_gateway_integration" "s3_proxy" {
  http_method             = aws_api_gateway_method.frontend_get.http_method
  resource_id             = aws_api_gateway_resource.s3_proxy.id
  rest_api_id             = aws_api_gateway_rest_api.api.id
  type                    = "AWS"
  integration_http_method = "GET"
  uri                     = "arn:aws:apigateway:${var.aws_region}:s3:path/${aws_s3_bucket.bucket.bucket}/frontend/{object}"
  passthrough_behavior    = "WHEN_NO_MATCH"
  credentials             = aws_iam_role.api_gateway.arn
  request_parameters      = {
    "integration.request.path.object" : "method.request.path.object"
  }
}

resource "aws_api_gateway_method_response" "root_redirect_302" {
  http_method         = aws_api_gateway_method.root_get.http_method
  status_code         = "302"
  resource_id         = aws_api_gateway_rest_api.api.root_resource_id
  rest_api_id         = aws_api_gateway_rest_api.api.id
  response_parameters = {
    "method.response.header.Location" = true
  }
}

resource "aws_api_gateway_method_response" "app_redirect_302" {
  http_method         = aws_api_gateway_method.app_get.http_method
  status_code         = "302"
  resource_id         = aws_api_gateway_resource.app.id
  rest_api_id         = aws_api_gateway_rest_api.api.id
  response_parameters = {
    "method.response.header.Location" = true
  }
}

resource "aws_api_gateway_method_response" "frontend_get_200" {
  http_method         = aws_api_gateway_method.frontend_get.http_method
  status_code         = "200"
  resource_id         = aws_api_gateway_resource.s3_proxy.id
  rest_api_id         = aws_api_gateway_rest_api.api.id
  response_parameters = {
    "method.response.header.Content-Type" = true
  }
}

resource "aws_api_gateway_integration_response" "root_redirect_302" {
  http_method        = aws_api_gateway_method.root_get.http_method
  status_code        = aws_api_gateway_method_response.root_redirect_302.status_code
  resource_id        = aws_api_gateway_rest_api.api.root_resource_id
  rest_api_id        = aws_api_gateway_rest_api.api.id
  response_templates = {
    "application/json" = <<-EOT
      #set($redirectTarget = "app/index.html")
      #set($path = $context.path)
      #if(!$path.endsWith("/"))
        #set($path = "$${path}/")
      #end
      #set($context.responseOverride.header.Location = "$${path}$${redirectTarget}")
    EOT
  }
}

resource "aws_api_gateway_integration_response" "app_redirect_302" {
  http_method        = aws_api_gateway_method.app_get.http_method
  status_code        = aws_api_gateway_method_response.app_redirect_302.status_code
  resource_id        = aws_api_gateway_resource.app.id
  rest_api_id        = aws_api_gateway_rest_api.api.id
  response_templates = {
    "application/json" = <<-EOT
      #set($redirectTarget = "index.html")
      #set($path = $context.path)
      #if(!$path.endsWith("/"))
        #set($path = "$${path}/")
      #end
      #set($context.responseOverride.header.Location = "$${path}$${redirectTarget}")
    EOT
  }
}

resource "aws_api_gateway_integration_response" "frontend_proxy_200" {
  http_method         = aws_api_gateway_method.frontend_get.http_method
  status_code         = aws_api_gateway_method_response.frontend_get_200.status_code
  resource_id         = aws_api_gateway_resource.s3_proxy.id
  rest_api_id         = aws_api_gateway_rest_api.api.id
  response_parameters = {
    "method.response.header.Content-Type" = "integration.response.header.Content-Type"
  }
}

resource "aws_api_gateway_deployment" "deployment" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  triggers    = {
    redeployment = filesha1("${path.module}/api.tf")
  }

  lifecycle {
    create_before_destroy = true
  }

  depends_on = [
    aws_api_gateway_integration.root_redirect,
    aws_api_gateway_integration.app_redirect,
    aws_api_gateway_integration.s3_proxy
  ]
}

resource "aws_api_gateway_stage" "wikilinks" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  deployment_id = aws_api_gateway_deployment.deployment.id
  stage_name    = "wikilinks"
}
