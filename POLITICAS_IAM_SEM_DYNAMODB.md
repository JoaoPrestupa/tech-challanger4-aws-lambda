# 🔐 Políticas IAM Corretas - Sem DynamoDB

## 📋 Resumo das 3 Lambdas e suas Permissões

---

## 🟢 Lambda 1: Receber Feedback

**Nome**: `feedback-system-receber-feedback`  
**Handler**: `lambda.fase4.lambda.ReceberFeedbackHandler::handleRequest`  
**Trigger**: API Gateway (POST /avaliacao)  
**Memória**: 1024 MB  
**Timeout**: 30 segundos  
**VPC**: ✅ Sim (precisa acessar RDS)

### Permissões Necessárias:

#### Managed Policies (anexar na criação do Role):
- `AWSLambdaBasicExecutionRole` - Para logs no CloudWatch
- `AWSLambdaVPCAccessExecutionRole` - Para acessar RDS na VPC

#### Inline Policy:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "EnviarParaFilaSQS",
      "Effect": "Allow",
      "Action": [
        "sqs:SendMessage"
      ],
      "Resource": "arn:aws:sqs:us-east-1:*:notificacao-urgencia-queue"
    },
    {
      "Sid": "EnviarMetricasCloudWatch",
      "Effect": "Allow",
      "Action": [
        "cloudwatch:PutMetricData"
      ],
      "Resource": "*"
    }
  ]
}
```

### Variáveis de Ambiente:
```
AWS_REGION=us-east-1
SQS_NOTIFICACAO_URL=https://sqs.us-east-1.amazonaws.com/SEU-ACCOUNT-ID/notificacao-urgencia-queue
DB_HOST=feedback-system-db.c9xxx.us-east-1.rds.amazonaws.com
DB_PORT=5432
DB_NAME=feedback_db
DB_USERNAME=postgres
DB_PASSWORD=SUA-SENHA-AQUI
```

---

## 🟡 Lambda 2: Enviar Notificação

**Nome**: `feedback-system-enviar-notificacao`  
**Handler**: `lambda.fase4.lambda.EnviarNotificacaoHandler::handleRequest`  
**Trigger**: SQS (notificacao-urgencia-queue)  
**Memória**: 512 MB  
**Timeout**: 5 minutos (300 segundos)  
**VPC**: ✅ Sim (precisa acessar RDS para marcar como notificado)

### Permissões Necessárias:

#### Managed Policies:
- `AWSLambdaBasicExecutionRole`
- `AWSLambdaVPCAccessExecutionRole`

#### Inline Policy:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ConsumirFilaSQS",
      "Effect": "Allow",
      "Action": [
        "sqs:ReceiveMessage",
        "sqs:DeleteMessage",
        "sqs:GetQueueAttributes"
      ],
      "Resource": "arn:aws:sqs:us-east-1:*:notificacao-urgencia-queue"
    },
    {
      "Sid": "PublicarTopicoSNS",
      "Effect": "Allow",
      "Action": [
        "sns:Publish"
      ],
      "Resource": "arn:aws:sns:us-east-1:*:urgencia-topic"
    },
    {
      "Sid": "EnviarEmailSES",
      "Effect": "Allow",
      "Action": [
        "ses:SendEmail",
        "ses:SendRawEmail"
      ],
      "Resource": "*"
    },
    {
      "Sid": "EnviarMetricasCloudWatch",
      "Effect": "Allow",
      "Action": [
        "cloudwatch:PutMetricData"
      ],
      "Resource": "*"
    }
  ]
}
```

### Variáveis de Ambiente:
```
AWS_REGION=us-east-1
SNS_URGENCIA_ARN=arn:aws:sns:us-east-1:SEU-ACCOUNT-ID:urgencia-topic
SES_FROM_EMAIL=noreply@seu-dominio.com
SES_ADMIN_EMAILS=admin1@exemplo.com,admin2@exemplo.com
DB_HOST=feedback-system-db.c9xxx.us-east-1.rds.amazonaws.com
DB_PORT=5432
DB_NAME=feedback_db
DB_USERNAME=postgres
DB_PASSWORD=SUA-SENHA-AQUI
```

### Event Source Mapping (SQS Trigger):
- **Batch size**: 10
- **Batch window**: 0 seconds
- **Enabled**: ✅ Yes

---

## 🔵 Lambda 3: Gerar Relatório

**Nome**: `feedback-system-gerar-relatorio`  
**Handler**: `lambda.fase4.lambda.GerarRelatorioHandler::handleRequest`  
**Trigger**: EventBridge (cron semanal)  
**Memória**: 1024 MB  
**Timeout**: 5 minutos (300 segundos)  
**VPC**: ✅ Sim (precisa consultar RDS)

### Permissões Necessárias:

#### Managed Policies:
- `AWSLambdaBasicExecutionRole`
- `AWSLambdaVPCAccessExecutionRole`

#### Inline Policy:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "EnviarEmailSES",
      "Effect": "Allow",
      "Action": [
        "ses:SendEmail",
        "ses:SendRawEmail"
      ],
      "Resource": "*"
    },
    {
      "Sid": "EnviarMetricasCloudWatch",
      "Effect": "Allow",
      "Action": [
        "cloudwatch:PutMetricData"
      ],
      "Resource": "*"
    }
  ]
}
```

### Variáveis de Ambiente:
```
AWS_REGION=us-east-1
SES_FROM_EMAIL=noreply@seu-dominio.com
SES_ADMIN_EMAILS=admin1@exemplo.com,admin2@exemplo.com
DB_HOST=feedback-system-db.c9xxx.us-east-1.rds.amazonaws.com
DB_PORT=5432
DB_NAME=feedback_db
DB_USERNAME=postgres
DB_PASSWORD=SUA-SENHA-AQUI
```

---

## 📝 Comparação: Com vs Sem DynamoDB

### ❌ Política ANTIGA (com DynamoDB):
```json
{
  "Effect": "Allow",
  "Action": [
    "dynamodb:PutItem",
    "dynamodb:GetItem",
    "dynamodb:UpdateItem",
    "dynamodb:Query",
    "dynamodb:Scan"
  ],
  "Resource": [
    "arn:aws:dynamodb:us-east-1:*:table/avaliacoes",
    "arn:aws:dynamodb:us-east-1:*:table/avaliacoes/index/*"
  ]
}
```

### ✅ Política NOVA (apenas RDS):
**Não precisa de permissões específicas!**  
RDS é acessado via VPC, não via IAM.

As permissões são:
- **VPC**: Lambda precisa estar na mesma VPC
- **Security Groups**: SG da Lambda deve poder acessar SG do RDS na porta 5432
- **Credenciais**: Username e password passados via variáveis de ambiente

---

## 🔒 Princípio do Menor Privilégio

### Lambda 1:
- ✅ Pode: Enviar mensagens SQS, registrar métricas
- ❌ Não pode: Ler SQS, publicar SNS, enviar e-mails

### Lambda 2:
- ✅ Pode: Ler e deletar SQS, publicar SNS, enviar e-mails
- ❌ Não pode: Enviar para SQS

### Lambda 3:
- ✅ Pode: Apenas enviar e-mails e registrar métricas
- ❌ Não pode: Acessar SQS ou SNS

---

## 🚨 Segurança das Credenciais do RDS

### ❌ NÃO RECOMENDADO (mas funcional):
Passar credenciais como variáveis de ambiente simples:
```
DB_PASSWORD=minhasenha123
```

### ✅ RECOMENDADO para Produção:
Usar **AWS Secrets Manager**:

1. **Criar Secret**:
```bash
aws secretsmanager create-secret \
  --name feedback-db-credentials \
  --secret-string '{"username":"postgres","password":"SUA-SENHA"}'
```

2. **Dar permissão à Lambda**:
```json
{
  "Effect": "Allow",
  "Action": [
    "secretsmanager:GetSecretValue"
  ],
  "Resource": "arn:aws:secretsmanager:us-east-1:*:secret:feedback-db-credentials-*"
}
```

3. **Código Lambda busca a senha**:
```java
// No código Java
SecretsManagerClient client = SecretsManagerClient.create();
GetSecretValueRequest request = GetSecretValueRequest.builder()
    .secretId("feedback-db-credentials")
    .build();
String secret = client.getSecretValue(request).secretString();
```

### 💰 Custo do Secrets Manager:
- $0.40/mês por secret
- $0.05 por 10.000 chamadas

**Para este projeto**: Variável de ambiente é suficiente, mas em produção use Secrets Manager.

---

## 🔗 Configuração de VPC e Security Groups

### Security Group do RDS: `feedback-rds-sg`

**Inbound Rules**:
| Type | Protocol | Port | Source | Description |
|------|----------|------|--------|-------------|
| PostgreSQL | TCP | 5432 | `feedback-lambda-sg` | Allow from Lambda |
| PostgreSQL | TCP | 5432 | `10.0.0.0/16` | Allow from VPC |

**Outbound Rules**:
| Type | Protocol | Port | Destination | Description |
|------|----------|------|-------------|-------------|
| All traffic | All | All | 0.0.0.0/0 | Default |

### Security Group da Lambda: `feedback-lambda-sg`

**Inbound Rules**:
- Nenhuma (Lambda não recebe conexões)

**Outbound Rules**:
| Type | Protocol | Port | Destination | Description |
|------|----------|------|-------------|-------------|
| All traffic | All | All | 0.0.0.0/0 | Allow Lambda to access AWS services |

---

## 🧪 Como Testar as Permissões

### Teste 1: Lambda 1 consegue enviar para SQS?
```bash
# Após enviar uma avaliação crítica via API
aws sqs get-queue-attributes \
  --queue-url https://sqs.us-east-1.amazonaws.com/SEU-ACCOUNT/notificacao-urgencia-queue \
  --attribute-names ApproximateNumberOfMessages
```
Deve mostrar mensagens na fila.

### Teste 2: Lambda 2 consegue consumir SQS e enviar e-mail?
Verificar logs:
```bash
aws logs tail /aws/lambda/feedback-system-enviar-notificacao --follow
```

### Teste 3: Lambda 3 consegue acessar RDS e gerar relatório?
```bash
aws lambda invoke \
  --function-name feedback-system-gerar-relatorio \
  --payload '{}' \
  --log-type Tail \
  response.json
```
Verificar se retorna sucesso e e-mail chega.

---

## ❗ Problemas Comuns

### Erro: "Access Denied" ao enviar para SQS
**Causa**: Política IAM não permite `sqs:SendMessage`  
**Solução**: Verificar inline policy do role da Lambda 1

### Erro: "Unable to connect to RDS"
**Causa**: Lambda não está na VPC ou Security Group bloqueando  
**Solução**: 
1. Lambda deve estar nas subnets privadas da VPC
2. Security Group do RDS deve permitir entrada do SG da Lambda
3. Verificar endpoint e credenciais

### Erro: "Email address not verified" (SES)
**Causa**: E-mail remetente ou destinatários não verificados  
**Solução**: Verificar todos os e-mails no console do SES

### Erro: Lambda não consegue acessar SQS/SNS/SES
**Causa**: Lambda em VPC privada sem acesso à internet  
**Solução**:
- **Opção 1**: Criar NAT Gateway ($30-45/mês)
- **Opção 2**: Criar VPC Endpoints (gratuito)

---

## 📚 Documentação Oficial

- [Lambda IAM Permissions](https://docs.aws.amazon.com/lambda/latest/dg/lambda-intro-execution-role.html)
- [Lambda VPC Configuration](https://docs.aws.amazon.com/lambda/latest/dg/configuration-vpc.html)
- [RDS Security Groups](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/Overview.RDSSecurityGroups.html)
- [SQS Permissions](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-authentication-and-access-control.html)

---

**✅ Permissões corrigidas e otimizadas!**  
**Sem dependências do DynamoDB.**  
**Pronto para deploy! 🚀**

