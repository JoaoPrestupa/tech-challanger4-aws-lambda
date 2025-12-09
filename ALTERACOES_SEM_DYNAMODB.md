# 📝 Alterações Realizadas - Remoção do DynamoDB

## ✅ O que foi feito

Você está correto! **Não é necessário usar DynamoDB** se o RDS PostgreSQL já atende suas necessidades. O sistema foi ajustado para usar **apenas RDS PostgreSQL** como banco de dados.

---

## 🔧 Alterações no Código

### 1. **pom.xml** - Removida dependência do DynamoDB
```xml
<!-- REMOVIDO: -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>dynamodb-enhanced</artifactId>
    <version>${aws.sdk.version}</version>
</dependency>
```

### 2. **Avaliacao.java** - Removidas anotações do DynamoDB
```java
// REMOVIDO:
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
@DynamoDbPartitionKey
@DynamoDbSortKey
public String getDataEnvioString() { ... }
```

**Agora a classe usa apenas JPA/Hibernate** para persistência no PostgreSQL.

### 3. **application.properties** - Removida configuração do DynamoDB
```ini
# REMOVIDO:
aws.dynamodb.table.avaliacoes=${DYNAMODB_TABLE:avaliacoes}
```

---

## 📚 Novo Guia Criado

✅ **Arquivo criado**: `CONFIGURACAO_AWS_SEM_DYNAMODB.md`

Este novo guia contém:
- ❌ Sem seção "DynamoDB - Banco NoSQL"
- ✅ Foco total em RDS PostgreSQL
- ✅ Políticas IAM atualizadas (sem permissões DynamoDB)
- ✅ Variáveis de ambiente corretas para as Lambdas
- ✅ Todos os passos de configuração ajustados

---

## 🚀 Próximos Passos

### 1. **Recompilar o Projeto**
```cmd
cd C:\Users\joao.pedro\Downloads\fase4\fase4
mvn clean package -DskipTests
```

Isso vai gerar um novo JAR **sem as dependências do DynamoDB**, reduzindo o tamanho do arquivo.

### 2. **Continuar a Configuração AWS**

Você parou no **item 9.1** (criar IAM Role para Lambda). Agora siga o novo guia:

📖 **Use o arquivo**: `CONFIGURACAO_AWS_SEM_DYNAMODB.md`

**Diferenças principais no item 9.1:**

#### ❌ Política ANTIGA (com DynamoDB):
```json
{
  "Effect": "Allow",
  "Action": [
    "dynamodb:PutItem",
    "dynamodb:GetItem",
    "dynamodb:UpdateItem"
  ],
  "Resource": "arn:aws:dynamodb:us-east-1:*:table/avaliacoes"
}
```

#### ✅ Política NOVA (sem DynamoDB):
```json
{
  "Effect": "Allow",
  "Action": [
    "sqs:SendMessage"
  ],
  "Resource": "arn:aws:sqs:us-east-1:*:notificacao-urgencia-queue"
}
```

### 3. **Variáveis de Ambiente das Lambdas**

Ao configurar as 3 Lambdas, **não adicione** estas variáveis:
- ❌ `DYNAMODB_TABLE`

**Adicione apenas**:
- ✅ `AWS_REGION`
- ✅ `DB_HOST`
- ✅ `DB_PORT`
- ✅ `DB_NAME`
- ✅ `DB_USERNAME`
- ✅ `DB_PASSWORD`
- ✅ `SQS_NOTIFICACAO_URL` (Lambda 1)
- ✅ `SNS_URGENCIA_ARN` (Lambda 2)
- ✅ `SES_FROM_EMAIL` (Lambdas 2 e 3)
- ✅ `SES_ADMIN_EMAILS` (Lambdas 2 e 3)

---

## 🎯 Por que RDS PostgreSQL é Suficiente?

### ✅ Vantagens do RDS para este projeto:

1. **Transações ACID**: Garante consistência nos dados
2. **Queries SQL complexas**: Relatórios e agregações são mais fáceis
3. **Menor custo**: Um serviço em vez de dois
4. **Mais simples**: Menos serviços para gerenciar
5. **Backup automático**: RDS já faz backups diários
6. **Relações**: Pode adicionar tabelas relacionadas no futuro (cursos, alunos, etc.)

### 📊 Quando usar DynamoDB?

DynamoDB seria útil se você precisasse de:
- Escala massiva (milhões de requisições/segundo)
- Latência ultra-baixa (milissegundos)
- Modelo de dados NoSQL (documentos, chave-valor)
- Custo variável (pay-per-request)

Para um **sistema de feedback de cursos**, RDS é perfeito!

---

## 📋 Checklist de Validação

Antes de fazer o upload do JAR para Lambda:

- [x] ✅ Dependência do DynamoDB removida do `pom.xml`
- [x] ✅ Anotações do DynamoDB removidas de `Avaliacao.java`
- [x] ✅ Variável `DYNAMODB_TABLE` removida do `application.properties`
- [ ] 🔄 Projeto recompilado com `mvn clean package`
- [ ] 🔄 JAR gerado em `target\fase4-0.0.1-SNAPSHOT.jar`
- [ ] 🔄 Políticas IAM criadas **sem permissões DynamoDB**
- [ ] 🔄 Variáveis de ambiente das Lambdas configuradas **sem DYNAMODB_TABLE**

---

## 🔍 Como Validar que Funciona?

Após criar tudo e fazer os testes:

### Teste 1: Criar Avaliação
```powershell
Invoke-RestMethod -Method Post -Uri "https://SUA-URL/prod/avaliacao" `
  -Headers @{"Content-Type"="application/json"} `
  -Body '{"descricao":"Teste sem DynamoDB","nota":8}'
```

**Verificar no RDS:**
```sql
SELECT * FROM avaliacoes ORDER BY data_envio DESC LIMIT 5;
```

### Teste 2: Notificação Crítica
```powershell
Invoke-RestMethod -Method Post -Uri "https://SUA-URL/prod/avaliacao" `
  -Headers @{"Content-Type"="application/json"} `
  -Body '{"descricao":"Problema crítico!","nota":2}'
```

**Deve**:
1. Salvar no RDS
2. Enviar para fila SQS
3. Lambda 2 processar
4. Enviar e-mail e notificação SNS
5. Atualizar `notificacao_enviada = true` no RDS

### Teste 3: Relatório
```cmd
aws lambda invoke --function-name feedback-system-gerar-relatorio --payload "{}" response.json
```

**Deve**:
1. Buscar dados do RDS (últimos 7 dias)
2. Calcular estatísticas
3. Enviar e-mail com relatório

---

## 💡 Dicas Importantes

### 1. **Lambda precisa de VPC para acessar RDS**
- ✅ Configure Lambda na mesma VPC do RDS
- ✅ Use subnets privadas
- ✅ Configure Security Groups corretamente

### 2. **Lambda em VPC precisa de internet para SQS/SNS/SES**
Duas opções:
- **NAT Gateway** ($30-45/mês) - mais simples
- **VPC Endpoints** (gratuito) - mais econômico

### 3. **Spring Boot em Lambda é pesado**
- Use **1024 MB de memória** (mínimo)
- Configure **timeout de 30 segundos**
- Primeira execução demora (cold start)

### 4. **Monitore os custos**
- RDS db.t3.micro: ~$15-20/mês
- Lambda: quase gratuito (free tier)
- NAT Gateway: ~$30-45/mês (se usar)
- Total: ~$15-65/mês

---

## 📞 Se Tiver Problemas

### Erro: "Could not resolve placeholder 'aws.dynamodb.table.avaliacoes'"
**Solução**: Certifique-se de ter removido a linha do `application.properties` e recompilado.

### Erro: "ClassNotFoundException: DynamoDbBean"
**Solução**: Execute `mvn clean package` para recompilar sem a dependência.

### Lambda não conecta no RDS
**Solução**: 
1. Lambda está na VPC?
2. Security Group do RDS permite entrada do SG da Lambda?
3. Endpoint e credenciais estão corretos?

### E-mails não chegam
**Solução**:
1. E-mails verificados no SES?
2. Ainda está em sandbox? (precisa verificar destinatários)
3. Verificar logs da Lambda

---

## ✅ Resumo

- ✅ **Código ajustado** para usar apenas RDS PostgreSQL
- ✅ **Novo guia criado** sem DynamoDB
- ✅ **Políticas IAM atualizadas** com permissões corretas
- ✅ **Pronto para continuar** a partir do item 9.1

**Próximo passo**: Recompile o projeto e continue seguindo o guia `CONFIGURACAO_AWS_SEM_DYNAMODB.md` 🚀

