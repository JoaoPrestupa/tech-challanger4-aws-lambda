# 🚀 Sistema de Feedback Serverless - AWS Lambda

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![AWS](https://img.shields.io/badge/AWS-Lambda%20%7C%20SQS%20%7C%20SNS-yellow.svg)](https://aws.amazon.com/)
[![Terraform](https://img.shields.io/badge/Terraform-1.0+-purple.svg)](https://www.terraform.io/)

Sistema serverless completo para gerenciamento de feedbacks de cursos online, com notificações automáticas e relatórios semanais.

## 📖 Sobre o Projeto

Este projeto foi desenvolvido como parte do **Tech Challenge - Fase 4**, implementando uma arquitetura serverless na AWS para automatizar:

- ✅ Recebimento e processamento de feedbacks de alunos
- 🚨 Notificações instantâneas para feedbacks críticos
- 📊 Geração automática de relatórios semanais
- 📈 Monitoramento em tempo real com CloudWatch
- 🔒 Segurança e governança de dados na nuvem

## 🏗️ Arquitetura

### Principais Componentes

- **3 Funções Lambda** (Java 21):
  1. `ReceberFeedbackHandler` - Processa avaliações via API Gateway
  2. `EnviarNotificacaoHandler` - Envia notificações de urgência
  3. `GerarRelatorioHandler` - Gera relatórios semanais

- **Banco de Dados**:
  - RDS PostgreSQL (dados relacionais)
  - DynamoDB (alta disponibilidade)

- **Mensageria**:
  - SQS (fila de notificações)
  - SNS (notificações push)

- **Comunicação**:
  - API Gateway (HTTP API)
  - SES (e-mails)

- **Monitoramento**:
  - CloudWatch Metrics & Logs
  - Alarmes automáticos

## 🎯 Funcionalidades

### 1️⃣ Recebimento de Feedbacks

```bash
POST /avaliacao
Content-Type: application/json

{
  "descricao": "O curso superou minhas expectativas!",
  "nota": 9
}
```

**Processamento:**
- Validação de dados (nota 0-10)
- Cálculo automático de urgência
- Armazenamento em RDS e DynamoDB
- Envio para fila SQS se crítico (nota ≤ 3)
- Registro de métricas no CloudWatch

### 2️⃣ Notificações Automáticas

Para feedbacks críticos:
- 📱 **SNS Push** - Notificação instantânea
- 📧 **E-mail SES** - Detalhes completos do feedback
- ✅ **Registro** - Marca avaliação como notificada

### 3️⃣ Relatórios Semanais

Gerados automaticamente toda segunda-feira às 9h UTC:
- Total de avaliações
- Média das notas
- Distribuição por dia
- Distribuição por urgência
- Enviado por e-mail para administradores

## 🚀 Começando

### Pré-requisitos

```bash
# Java 21 JDK
java -version

# Maven 3.8+
mvn -version

# Terraform 1.0+
terraform -version

# AWS CLI
aws --version
```

### Configuração AWS

1. **Configure credenciais AWS:**
```bash
aws configure
```

2. **Verifique e-mail no SES:**
```bash
aws ses verify-email-identity --email-address seu-email@example.com
```
Confirme o e-mail recebido.

### Instalação

1. **Clone o repositório:**
```bash
git clone <repositorio>
cd fase4
```

2. **Configure variáveis:**
```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
```

Edite `terraform.tfvars` com seus valores:
```hcl
aws_region = "us-east-1"
project_name = "feedback-system"
admin_emails = "admin@example.com"
from_email = "noreply@example.com"
```

3. **Compile a aplicação:**
```bash
mvn clean package
```

4. **Execute o deploy:**

**Windows:**
```cmd
deploy.cmd
```

**Linux/Mac:**
```bash
chmod +x deploy.sh
./deploy.sh
```

## 📝 Testes

### Teste Local (Spring Boot)

```bash
# Inicie a aplicação localmente
mvn spring-boot:run

# Teste o endpoint
curl -X POST http://localhost:8080/api/avaliacoes \
  -H "Content-Type: application/json" \
  -d '{
    "descricao": "Curso excelente!",
    "nota": 9
  }'
```

### Teste na AWS

```bash
# Após o deploy, teste o endpoint Lambda
curl -X POST https://seu-api-id.execute-api.us-east-1.amazonaws.com/prod/avaliacao \
  -H "Content-Type: application/json" \
  -d '{
    "descricao": "Teste de feedback crítico",
    "nota": 2
  }'
```

## 📊 Monitoramento

### Visualizar Logs

```bash
# Logs do Lambda de recebimento
aws logs tail /aws/lambda/feedback-system-receber-feedback --follow

# Logs do Lambda de notificação
aws logs tail /aws/lambda/feedback-system-enviar-notificacao --follow

# Logs do Lambda de relatório
aws logs tail /aws/lambda/feedback-system-gerar-relatorio --follow
```

### Métricas CloudWatch

Acesse o console da AWS:
- CloudWatch > Metrics > FeedbackSystem
- Visualize métricas customizadas

### Alarmes

Configurados automaticamente:
- **Lambda Errors**: > 5 erros em 5 minutos
- **DLQ Messages**: Mensagens na Dead Letter Queue

## 🔒 Segurança

### Governança de Acesso

- ✅ **IAM Roles**: Princípio do menor privilégio
- ✅ **Security Groups**: Isolamento de rede
- ✅ **VPC**: RDS em rede privada
- ✅ **Encryption**: Dados em repouso e em trânsito

### Proteção de Dados

- ✅ **RDS**: Storage criptografado com KMS
- ✅ **DynamoDB**: Server-side encryption
- ✅ **Secrets**: Gerenciamento seguro via Terraform
- ✅ **HTTPS**: Obrigatório no API Gateway

### Auditoria

- ✅ **CloudWatch Logs**: Todos os eventos registrados
- ✅ **CloudTrail**: Auditoria de API calls (opcional)
- ✅ **Métricas**: Monitoramento contínuo

## 💰 Custos

### Estimativa Mensal

| Serviço | Custo Estimado |
|---------|----------------|
| Lambda | ~$5 |
| RDS (t3.micro) | ~$15 |
| DynamoDB | ~$2 |
| SQS + SNS + SES | ~$1 |
| API Gateway | ~$1 |
| CloudWatch | ~$5 |
| **Total** | **~$30-35/mês** |

### Otimizações

- Use Reserved Instances no RDS (até 60% economia)
- Configure retenção de logs (3-7 dias)
- Otimize memória das Lambdas

## 📚 Documentação

- [Documentação Completa](DOCUMENTACAO.md) - Arquitetura detalhada
- [Guia de Deploy](GUIA_DEPLOY.md) - Instruções passo a passo
- [API Reference](API.md) - Especificação dos endpoints

## 🏆 Atendimento aos Requisitos

### ✅ Requisitos Implementados

- [x] Ambiente cloud (AWS) configurado e funcionando
- [x] Segurança e governança de acesso (IAM Roles, Security Groups)
- [x] Componentes de suporte (RDS, DynamoDB, SQS, SNS, SES)
- [x] Deploy automatizado (Terraform + Scripts)
- [x] Aplicação monitorada (CloudWatch Metrics + Logs + Alarms)
- [x] Notificações automáticas para problemas críticos
- [x] Relatório semanal com médias de avaliações
- [x] Implementação serverless (AWS Lambda)
- [x] Separação de responsabilidades (3 Lambdas distintas)

### 🎯 Princípio da Responsabilidade Única

Cada Lambda tem uma única responsabilidade:
1. **ReceberFeedbackHandler**: Apenas recebe e processa feedbacks
2. **EnviarNotificacaoHandler**: Apenas envia notificações
3. **GerarRelatorioHandler**: Apenas gera relatórios

## 🛠️ Tecnologias

- **Backend**: Java 21, Spring Boot 4.0.0
- **Cloud**: AWS (Lambda, RDS, DynamoDB, SQS, SNS, SES, API Gateway)
- **IaC**: Terraform 1.0+
- **Build**: Maven
- **Database**: PostgreSQL 15.4
- **Monitoring**: CloudWatch

## 📦 Estrutura do Projeto

```
fase4/
├── src/
│   └── main/
│       ├── java/lambda/fase4/
│       │   ├── model/          # Entidades
│       │   ├── dto/            # DTOs
│       │   ├── repository/     # Repositories
│       │   ├── service/        # Services
│       │   ├── controller/     # Controllers (testes locais)
│       │   ├── lambda/         # Lambda Handlers
│       │   └── config/         # Configurações
│       └── resources/
│           └── application.properties
├── terraform/
│   ├── main.tf                 # Infraestrutura AWS
│   └── terraform.tfvars.example
├── deploy.cmd                  # Script deploy Windows
├── deploy.sh                   # Script deploy Linux/Mac
├── DOCUMENTACAO.md             # Documentação completa
├── README.md                   # Este arquivo
└── pom.xml                     # Dependências Maven
```

## 🤝 Contribuindo

1. Fork o projeto
2. Crie uma branch (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## 📄 Licença

Este projeto foi desenvolvido para fins educacionais - Tech Challenge Fase 4.

## 👥 Autores

**Tech Challenge - Fase 4**
- Sistema de Feedback Serverless
- Arquitetura AWS Lambda

## 🎓 Referências

- [AWS Lambda Documentation](https://docs.aws.amazon.com/lambda/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [AWS Well-Architected Framework](https://aws.amazon.com/architecture/well-architected/)

---

⭐ **Se este projeto te ajudou, deixe uma estrela!**

#   t e c h - c h a l l a n g e r 4 - a w s - l a m b d a  
 