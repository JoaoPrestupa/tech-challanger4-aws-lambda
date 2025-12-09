# 🚀 GUIA RÁPIDO - Configuração Multi-Região

## 📍 Regiões Utilizadas

| Serviço | Região | Motivo |
|---------|--------|--------|
| VPC, RDS, Lambda, SQS, SNS, API Gateway | **us-east-2** | Região principal escolhida |
| SES (E-mail) | **us-east-1** | SES configurado lá |

---

## ⚡ Variáveis de Ambiente (COPIAR E COLAR)

### 🔵 Lambda 1: feedback-system-receber-feedback

```
AWS_REGION=us-east-2
AWS_SES_REGION=us-east-1
SQS_NOTIFICACAO_URL=COLE-SUA-URL-DO-SQS-AQUI
DB_HOST=COLE-SEU-ENDPOINT-RDS-AQUI
DB_PORT=5432
DB_NAME=feedback_db
DB_USERNAME=postgres
DB_PASSWORD=COLE-SUA-SENHA-AQUI
```

### 🟡 Lambda 2: feedback-system-enviar-notificacao

```
AWS_REGION=us-east-2
AWS_SES_REGION=us-east-1
SNS_URGENCIA_ARN=COLE-SEU-ARN-SNS-AQUI
SES_FROM_EMAIL=COLE-SEU-EMAIL-VERIFICADO-AQUI
SES_ADMIN_EMAILS=COLE-EMAILS-SEPARADOS-POR-VIRGULA-AQUI
DB_HOST=COLE-SEU-ENDPOINT-RDS-AQUI
DB_PORT=5432
DB_NAME=feedback_db
DB_USERNAME=postgres
DB_PASSWORD=COLE-SUA-SENHA-AQUI
```

### 🟢 Lambda 3: feedback-system-gerar-relatorio

```
AWS_REGION=us-east-2
AWS_SES_REGION=us-east-1
SES_FROM_EMAIL=COLE-SEU-EMAIL-VERIFICADO-AQUI
SES_ADMIN_EMAILS=COLE-EMAILS-SEPARADOS-POR-VIRGULA-AQUI
DB_HOST=COLE-SEU-ENDPOINT-RDS-AQUI
DB_PORT=5432
DB_NAME=feedback_db
DB_USERNAME=postgres
DB_PASSWORD=COLE-SUA-SENHA-AQUI
```

---

## 📋 Como Pegar Cada Valor

### 1. **SQS_NOTIFICACAO_URL**
```
Console AWS → SQS → notificacao-urgencia-queue → Copiar URL
Formato: https://sqs.us-east-2.amazonaws.com/123456789012/notificacao-urgencia-queue
```

### 2. **SNS_URGENCIA_ARN**
```
Console AWS → SNS → Topics → urgencia-topic → Copiar ARN
Formato: arn:aws:sns:us-east-2:123456789012:urgencia-topic
```

### 3. **DB_HOST**
```
Console AWS → RDS → Databases → feedback-system-db → Endpoint
Formato: feedback-system-db.c9xxxxx.us-east-2.rds.amazonaws.com
```

### 4. **DB_PASSWORD**
```
A senha que você definiu ao criar o RDS
Se usou auto-generate, está nas credenciais salvas
```

### 5. **SES_FROM_EMAIL**
```
Console AWS → SES (em us-east-1) → Verified identities
Use o e-mail que tem status "Verified"
Exemplo: noreply@seu-dominio.com OU seu-email@gmail.com
```

### 6. **SES_ADMIN_EMAILS**
```
Mesma coisa, mas pode ser vários separados por vírgula
Exemplo: admin@exemplo.com,gerente@exemplo.com
TODOS devem estar verificados em us-east-1 (se ainda em sandbox)
```

---

## 🔐 Políticas IAM (JSON)

### Lambda 1: feedback-lambda-receber-role

**Managed Policies**:
- `AWSLambdaBasicExecutionRole`
- `AWSLambdaVPCAccessExecutionRole`

**Inline Policy** (nome: `FeedbackLambdaReceberPolicy`):
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["sqs:SendMessage"],
      "Resource": "arn:aws:sqs:us-east-2:*:notificacao-urgencia-queue"
    },
    {
      "Effect": "Allow",
      "Action": ["cloudwatch:PutMetricData"],
      "Resource": "*"
    }
  ]
}
```

### Lambda 2: feedback-lambda-notificacao-role

**Managed Policies**:
- `AWSLambdaBasicExecutionRole`
- `AWSLambdaVPCAccessExecutionRole`

**Inline Policy** (nome: `FeedbackLambdaNotificacaoPolicy`):
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "sqs:ReceiveMessage",
        "sqs:DeleteMessage",
        "sqs:GetQueueAttributes"
      ],
      "Resource": "arn:aws:sqs:us-east-2:*:notificacao-urgencia-queue"
    },
    {
      "Effect": "Allow",
      "Action": ["sns:Publish"],
      "Resource": "arn:aws:sns:us-east-2:*:urgencia-topic"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ses:SendEmail",
        "ses:SendRawEmail"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": ["cloudwatch:PutMetricData"],
      "Resource": "*"
    }
  ]
}
```

### Lambda 3: feedback-lambda-relatorio-role

**Managed Policies**:
- `AWSLambdaBasicExecutionRole`
- `AWSLambdaVPCAccessExecutionRole`

**Inline Policy** (nome: `FeedbackLambdaRelatorioPolicy`):
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ses:SendEmail",
        "ses:SendRawEmail"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": ["cloudwatch:PutMetricData"],
      "Resource": "*"
    }
  ]
}
```

---

## ⚙️ Configuração das Lambdas

### Configurações Comuns (todas as 3):

| Configuração | Valor |
|--------------|-------|
| **Runtime** | Java 21 |
| **Architecture** | x86_64 |
| **Memory** | 1024 MB (Lambda 1 e 3), 512 MB (Lambda 2) |
| **Timeout** | 30s (Lambda 1), 300s (Lambda 2 e 3) |
| **VPC** | feedback-system-vpc |
| **Subnets** | As 2 subnets privadas |
| **Security Groups** | feedback-lambda-sg |

### Handlers:

| Lambda | Handler |
|--------|---------|
| Lambda 1 | `lambda.fase4.lambda.ReceberFeedbackHandler::handleRequest` |
| Lambda 2 | `lambda.fase4.lambda.EnviarNotificacaoHandler::handleRequest` |
| Lambda 3 | `lambda.fase4.lambda.GerarRelatorioHandler::handleRequest` |

---

## 🧪 Comandos de Teste

### Teste 1: Avaliação Normal (nota 9)
```powershell
Invoke-RestMethod -Method Post -Uri "https://SUA-URL-API-GATEWAY/prod/avaliacao" `
  -Headers @{"Content-Type"="application/json"} `
  -Body '{"descricao":"Ótimo curso!","nota":9}'
```

### Teste 2: Avaliação Crítica (nota 2 - dispara e-mail)
```powershell
Invoke-RestMethod -Method Post -Uri "https://SUA-URL-API-GATEWAY/prod/avaliacao" `
  -Headers @{"Content-Type"="application/json"} `
  -Body '{"descricao":"Problema grave no sistema!","nota":2}'
```

### Teste 3: Gerar Relatório Manual
```cmd
aws lambda invoke --function-name feedback-system-gerar-relatorio --payload "{}" response.json
```

---

## 🔍 Verificação Rápida

### ✅ Checklist Pré-Deploy

- [ ] JAR compilado (`mvn clean package -DskipTests`)
- [ ] JAR está em `target\fase4-0.0.1-SNAPSHOT.jar`
- [ ] E-mails verificados em **us-east-1** (não us-east-2!)
- [ ] RDS criado e status "Available"
- [ ] SQS fila criada
- [ ] SNS tópico criado e e-mails subscritos
- [ ] VPC com 2 subnets privadas
- [ ] Security Groups configurados
- [ ] IAM Roles criadas para as 3 Lambdas

### ✅ Checklist Pós-Deploy

- [ ] Lambda 1 criada e na VPC
- [ ] Lambda 2 criada, na VPC e conectada ao SQS
- [ ] Lambda 3 criada e na VPC
- [ ] API Gateway criado e URL anotada
- [ ] EventBridge agendamento semanal criado
- [ ] Teste 1 funciona (avaliação normal)
- [ ] Teste 2 funciona (e-mail chega)
- [ ] Logs aparecem no CloudWatch

---

## 🚨 Troubleshooting Rápido

| Problema | Solução Rápida |
|----------|----------------|
| **E-mail não chega** | 1. E-mail verificado em **us-east-1**?<br>2. Ainda em sandbox? Verificar destinatários<br>3. Checar spam |
| **Lambda timeout** | 1. Aumentar timeout para 60s<br>2. Verificar se está na VPC<br>3. Cold start demora (normal) |
| **Cannot connect to RDS** | 1. Lambda na mesma VPC?<br>2. Security Group RDS permite SG Lambda?<br>3. Endpoint correto? |
| **Access Denied SES** | 1. Política IAM permite `ses:SendEmail`?<br>2. E-mail remetente verificado? |
| **SQS não dispara Lambda** | 1. Event source mapping habilitado?<br>2. Lambda tem permissão SQS? |

---

## 📊 Onde Ver os Logs

### Logs das Lambdas (us-east-2):
```
CloudWatch → Log groups → /aws/lambda/feedback-system-XXX
```

### Métricas do SES (us-east-1):
```
CloudWatch (mude para us-east-1) → Metrics → AWS/SES
```

---

## 💰 Custos Estimados

| Item | Custo/Mês |
|------|-----------|
| RDS db.t3.micro | $15-20 |
| Lambda (3 funções) | $0-5 (free tier) |
| SQS, SNS, SES | $0 (free tier) |
| Transferência cross-region | $0.02 |
| **TOTAL** | **~$15-25/mês** |

---

## 📞 Suporte Rápido

### E-mails não verificados?
```
Console → SES (us-east-1) → Verified identities → Create identity
```

### Ver ARN do SNS?
```
Console → SNS → Topics → urgencia-topic → Copiar ARN
```

### Ver URL do SQS?
```
Console → SQS → notificacao-urgencia-queue → Detalhes → URL
```

### Ver Endpoint do RDS?
```
Console → RDS → Databases → feedback-system-db → Connectivity & security → Endpoint
```

---

## 🎯 IMPORTANTE: Regiões!

⚠️ **SEMPRE verifique a região no console AWS (canto superior direito)**

- Para SQS, SNS, RDS, Lambda, API Gateway: **us-east-2**
- Para SES: **us-east-1**

---

**✅ Guia rápido completo! Use como referência durante a configuração! 🚀**

