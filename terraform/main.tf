# ========================================
# TERRAFORM - Infraestrutura AWS
# Sistema de Feedback Serverless
# ========================================

terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# ========================================
# VARIÁVEIS
# ========================================

variable "aws_region" {
  description = "Região AWS"
  default     = "us-east-1"
}

variable "project_name" {
  description = "Nome do projeto"
  default     = "feedback-system"
}

variable "admin_emails" {
  description = "E-mails dos administradores (separados por vírgula)"
  type        = string
}

variable "from_email" {
  description = "E-mail remetente verificado no SES"
  type        = string
}

# ========================================
# VPC E NETWORKING
# ========================================

resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "${var.project_name}-vpc"
  }
}

resource "aws_subnet" "private_a" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.1.0/24"
  availability_zone = "${var.aws_region}a"

  tags = {
    Name = "${var.project_name}-private-a"
  }
}

resource "aws_subnet" "private_b" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.2.0/24"
  availability_zone = "${var.aws_region}b"

  tags = {
    Name = "${var.project_name}-private-b"
  }
}

# ========================================
# RDS POSTGRESQL (Banco de Dados)
# ========================================

resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-db-subnet"
  subnet_ids = [aws_subnet.private_a.id, aws_subnet.private_b.id]

  tags = {
    Name = "${var.project_name}-db-subnet"
  }
}

resource "aws_security_group" "rds" {
  name        = "${var.project_name}-rds-sg"
  description = "Security group for RDS PostgreSQL"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-rds-sg"
  }
}

resource "aws_db_instance" "postgres" {
  identifier             = "${var.project_name}-db"
  engine                 = "postgres"
  engine_version         = "15.4"
  instance_class         = "db.t3.micro"
  allocated_storage      = 20
  storage_type           = "gp3"
  storage_encrypted      = true

  db_name  = "feedback_db"
  username = "admin"
  password = random_password.db_password.result

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "Mon:04:00-Mon:05:00"

  skip_final_snapshot = true
  deletion_protection = false

  tags = {
    Name = "${var.project_name}-postgres"
  }
}

resource "random_password" "db_password" {
  length  = 16
  special = true
}

# ========================================
# DYNAMODB (Armazenamento NoSQL)
# ========================================

resource "aws_dynamodb_table" "avaliacoes" {
  name           = "avaliacoes"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "id"
  range_key      = "dataEnvio"

  attribute {
    name = "id"
    type = "S"
  }

  attribute {
    name = "dataEnvio"
    type = "S"
  }

  attribute {
    name = "urgencia"
    type = "S"
  }

  global_secondary_index {
    name            = "UrgenciaIndex"
    hash_key        = "urgencia"
    range_key       = "dataEnvio"
    projection_type = "ALL"
  }

  point_in_time_recovery {
    enabled = true
  }

  server_side_encryption {
    enabled = true
  }

  tags = {
    Name = "${var.project_name}-avaliacoes"
  }
}

# ========================================
# SQS - FILAS
# ========================================

# Fila principal para notificações de urgência
resource "aws_sqs_queue" "notificacao_urgencia" {
  name                      = "notificacao-urgencia-queue"
  delay_seconds             = 0
  max_message_size          = 262144
  message_retention_seconds = 345600 # 4 dias
  receive_wait_time_seconds = 10

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.notificacao_dlq.arn
    maxReceiveCount     = 3
  })

  tags = {
    Name = "${var.project_name}-notificacao-queue"
  }
}

# Dead Letter Queue (DLQ)
resource "aws_sqs_queue" "notificacao_dlq" {
  name                      = "notificacao-urgencia-dlq"
  message_retention_seconds = 1209600 # 14 dias

  tags = {
    Name = "${var.project_name}-notificacao-dlq"
  }
}

# ========================================
# SNS - TÓPICOS
# ========================================

resource "aws_sns_topic" "urgencia" {
  name = "urgencia-topic"

  tags = {
    Name = "${var.project_name}-urgencia-topic"
  }
}

# Subscrição de e-mail para administradores
resource "aws_sns_topic_subscription" "admin_email" {
  for_each = toset(split(",", var.admin_emails))

  topic_arn = aws_sns_topic.urgencia.arn
  protocol  = "email"
  endpoint  = trimspace(each.value)
}

# ========================================
# IAM ROLES E POLÍTICAS
# ========================================

# Role para Lambda - Receber Feedback
resource "aws_iam_role" "lambda_receber_feedback" {
  name = "${var.project_name}-lambda-receber-feedback"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy" "lambda_receber_feedback_policy" {
  name = "${var.project_name}-lambda-receber-feedback-policy"
  role = aws_iam_role.lambda_receber_feedback.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "arn:aws:logs:*:*:*"
      },
      {
        Effect = "Allow"
        Action = [
          "dynamodb:PutItem",
          "dynamodb:GetItem",
          "dynamodb:UpdateItem"
        ]
        Resource = aws_dynamodb_table.avaliacoes.arn
      },
      {
        Effect = "Allow"
        Action = [
          "sqs:SendMessage"
        ]
        Resource = aws_sqs_queue.notificacao_urgencia.arn
      },
      {
        Effect = "Allow"
        Action = [
          "cloudwatch:PutMetricData"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "ec2:CreateNetworkInterface",
          "ec2:DescribeNetworkInterfaces",
          "ec2:DeleteNetworkInterface"
        ]
        Resource = "*"
      }
    ]
  })
}

# Role para Lambda - Enviar Notificação
resource "aws_iam_role" "lambda_enviar_notificacao" {
  name = "${var.project_name}-lambda-enviar-notificacao"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy" "lambda_enviar_notificacao_policy" {
  name = "${var.project_name}-lambda-enviar-notificacao-policy"
  role = aws_iam_role.lambda_enviar_notificacao.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "arn:aws:logs:*:*:*"
      },
      {
        Effect = "Allow"
        Action = [
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes"
        ]
        Resource = aws_sqs_queue.notificacao_urgencia.arn
      },
      {
        Effect = "Allow"
        Action = [
          "sns:Publish"
        ]
        Resource = aws_sns_topic.urgencia.arn
      },
      {
        Effect = "Allow"
        Action = [
          "ses:SendEmail",
          "ses:SendRawEmail"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "dynamodb:UpdateItem"
        ]
        Resource = aws_dynamodb_table.avaliacoes.arn
      },
      {
        Effect = "Allow"
        Action = [
          "cloudwatch:PutMetricData"
        ]
        Resource = "*"
      }
    ]
  })
}

# Role para Lambda - Gerar Relatório
resource "aws_iam_role" "lambda_gerar_relatorio" {
  name = "${var.project_name}-lambda-gerar-relatorio"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy" "lambda_gerar_relatorio_policy" {
  name = "${var.project_name}-lambda-gerar-relatorio-policy"
  role = aws_iam_role.lambda_gerar_relatorio.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "arn:aws:logs:*:*:*"
      },
      {
        Effect = "Allow"
        Action = [
          "dynamodb:Query",
          "dynamodb:Scan"
        ]
        Resource = [
          aws_dynamodb_table.avaliacoes.arn,
          "${aws_dynamodb_table.avaliacoes.arn}/index/*"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "ses:SendEmail",
          "ses:SendRawEmail"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "cloudwatch:PutMetricData"
        ]
        Resource = "*"
      }
    ]
  })
}

# ========================================
# LAMBDA FUNCTIONS
# ========================================

# Lambda 1: Receber Feedback
resource "aws_lambda_function" "receber_feedback" {
  filename      = "target/fase4-0.0.1-SNAPSHOT.jar"
  function_name = "${var.project_name}-receber-feedback"
  role          = aws_iam_role.lambda_receber_feedback.arn
  handler       = "lambda.fase4.lambda.ReceberFeedbackHandler::handleRequest"
  runtime       = "java21"
  timeout       = 30
  memory_size   = 512

  environment {
    variables = {
      AWS_REGION            = var.aws_region
      SQS_NOTIFICACAO_URL   = aws_sqs_queue.notificacao_urgencia.url
      DYNAMODB_TABLE        = aws_dynamodb_table.avaliacoes.name
      DB_HOST               = aws_db_instance.postgres.address
      DB_NAME               = aws_db_instance.postgres.db_name
      DB_USERNAME           = aws_db_instance.postgres.username
      DB_PASSWORD           = aws_db_instance.postgres.password
    }
  }

  tags = {
    Name = "${var.project_name}-receber-feedback"
  }
}

# Lambda 2: Enviar Notificação
resource "aws_lambda_function" "enviar_notificacao" {
  filename      = "target/fase4-0.0.1-SNAPSHOT.jar"
  function_name = "${var.project_name}-enviar-notificacao"
  role          = aws_iam_role.lambda_enviar_notificacao.arn
  handler       = "lambda.fase4.lambda.EnviarNotificacaoHandler::handleRequest"
  runtime       = "java21"
  timeout       = 300
  memory_size   = 512

  environment {
    variables = {
      AWS_REGION        = var.aws_region
      SNS_URGENCIA_ARN  = aws_sns_topic.urgencia.arn
      SES_FROM_EMAIL    = var.from_email
      SES_ADMIN_EMAILS  = var.admin_emails
      DYNAMODB_TABLE    = aws_dynamodb_table.avaliacoes.name
    }
  }

  tags = {
    Name = "${var.project_name}-enviar-notificacao"
  }
}

# Event Source Mapping: SQS -> Lambda
resource "aws_lambda_event_source_mapping" "notificacao_queue" {
  event_source_arn = aws_sqs_queue.notificacao_urgencia.arn
  function_name    = aws_lambda_function.enviar_notificacao.arn
  batch_size       = 10
  enabled          = true
}

# Lambda 3: Gerar Relatório
resource "aws_lambda_function" "gerar_relatorio" {
  filename      = "target/fase4-0.0.1-SNAPSHOT.jar"
  function_name = "${var.project_name}-gerar-relatorio"
  role          = aws_iam_role.lambda_gerar_relatorio.arn
  handler       = "lambda.fase4.lambda.GerarRelatorioHandler::handleRequest"
  runtime       = "java21"
  timeout       = 300
  memory_size   = 512

  environment {
    variables = {
      AWS_REGION       = var.aws_region
      SES_FROM_EMAIL   = var.from_email
      SES_ADMIN_EMAILS = var.admin_emails
      DYNAMODB_TABLE   = aws_dynamodb_table.avaliacoes.name
      DB_HOST          = aws_db_instance.postgres.address
      DB_NAME          = aws_db_instance.postgres.db_name
      DB_USERNAME      = aws_db_instance.postgres.username
      DB_PASSWORD      = aws_db_instance.postgres.password
    }
  }

  tags = {
    Name = "${var.project_name}-gerar-relatorio"
  }
}

# EventBridge Rule: Execução semanal (toda segunda-feira às 9h UTC)
resource "aws_cloudwatch_event_rule" "relatorio_semanal" {
  name                = "${var.project_name}-relatorio-semanal"
  description         = "Trigger para geração de relatório semanal"
  schedule_expression = "cron(0 9 ? * MON *)"
}

resource "aws_cloudwatch_event_target" "relatorio_semanal" {
  rule      = aws_cloudwatch_event_rule.relatorio_semanal.name
  target_id = "Lambda"
  arn       = aws_lambda_function.gerar_relatorio.arn
}

resource "aws_lambda_permission" "allow_eventbridge" {
  statement_id  = "AllowExecutionFromEventBridge"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.gerar_relatorio.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.relatorio_semanal.arn
}

# ========================================
# API GATEWAY
# ========================================

resource "aws_apigatewayv2_api" "main" {
  name          = "${var.project_name}-api"
  protocol_type = "HTTP"

  cors_configuration {
    allow_origins = ["*"]
    allow_methods = ["POST", "GET", "OPTIONS"]
    allow_headers = ["Content-Type"]
  }
}

resource "aws_apigatewayv2_integration" "receber_feedback" {
  api_id           = aws_apigatewayv2_api.main.id
  integration_type = "AWS_PROXY"
  integration_uri  = aws_lambda_function.receber_feedback.invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "post_avaliacao" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "POST /avaliacao"
  target    = "integrations/${aws_apigatewayv2_integration.receber_feedback.id}"
}

resource "aws_apigatewayv2_stage" "prod" {
  api_id      = aws_apigatewayv2_api.main.id
  name        = "prod"
  auto_deploy = true

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.api_gateway.arn
    format = jsonencode({
      requestId      = "$context.requestId"
      ip             = "$context.identity.sourceIp"
      requestTime    = "$context.requestTime"
      httpMethod     = "$context.httpMethod"
      routeKey       = "$context.routeKey"
      status         = "$context.status"
      protocol       = "$context.protocol"
      responseLength = "$context.responseLength"
    })
  }
}

resource "aws_cloudwatch_log_group" "api_gateway" {
  name              = "/aws/apigateway/${var.project_name}"
  retention_in_days = 7
}

resource "aws_lambda_permission" "api_gateway" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.receber_feedback.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.main.execution_arn}/*/*"
}

# ========================================
# CLOUDWATCH ALARMS
# ========================================

resource "aws_cloudwatch_metric_alarm" "lambda_errors" {
  alarm_name          = "${var.project_name}-lambda-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "Errors"
  namespace           = "AWS/Lambda"
  period              = "300"
  statistic           = "Sum"
  threshold           = "5"
  alarm_description   = "Alerta quando há muitos erros nas Lambdas"
  alarm_actions       = [aws_sns_topic.urgencia.arn]
}

resource "aws_cloudwatch_metric_alarm" "sqs_dlq_messages" {
  alarm_name          = "${var.project_name}-dlq-messages"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "ApproximateNumberOfMessagesVisible"
  namespace           = "AWS/SQS"
  period              = "300"
  statistic           = "Sum"
  threshold           = "0"
  alarm_description   = "Alerta quando há mensagens na DLQ"
  alarm_actions       = [aws_sns_topic.urgencia.arn]

  dimensions = {
    QueueName = aws_sqs_queue.notificacao_dlq.name
  }
}

# ========================================
# OUTPUTS
# ========================================

output "api_endpoint" {
  description = "Endpoint da API Gateway"
  value       = "${aws_apigatewayv2_api.main.api_endpoint}/prod/avaliacao"
}

output "db_endpoint" {
  description = "Endpoint do banco RDS"
  value       = aws_db_instance.postgres.endpoint
}

output "db_password" {
  description = "Senha do banco de dados"
  value       = random_password.db_password.result
  sensitive   = true
}

output "sqs_queue_url" {
  description = "URL da fila SQS"
  value       = aws_sqs_queue.notificacao_urgencia.url
}

output "sns_topic_arn" {
  description = "ARN do tópico SNS"
  value       = aws_sns_topic.urgencia.arn
}

