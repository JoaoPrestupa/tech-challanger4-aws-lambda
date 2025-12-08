#!/bin/bash
# ========================================
# Script de Deploy da Aplicação
# Sistema de Feedback AWS Lambda
# ========================================

set -e

echo "========================================"
echo " DEPLOY - SISTEMA DE FEEDBACK"
echo "========================================"
echo ""

echo "[1/5] Compilando aplicação com Maven..."
mvn clean package -DskipTests
echo "Compilação concluída com sucesso!"
echo ""

echo "[2/5] Inicializando Terraform..."
cd terraform
terraform init
echo ""

echo "[3/5] Validando configuração Terraform..."
terraform validate
echo ""

echo "[4/5] Planejando infraestrutura..."
terraform plan -out=tfplan
echo ""

echo "[5/5] Aplicando infraestrutura..."
read -p "Deseja aplicar as mudanças? (sim/nao): " CONFIRM
if [ "$CONFIRM" = "sim" ]; then
    terraform apply tfplan
    echo ""
    echo "========================================"
    echo " DEPLOY CONCLUÍDO COM SUCESSO!"
    echo "========================================"
    echo ""
    terraform output
else
    echo "Deploy cancelado pelo usuário"
fi

cd ..
# Valores para as variáveis do Terraform
# Copie este arquivo para terraform.tfvars e preencha com seus valores

aws_region = "us-east-1"
project_name = "feedback-system"

# E-mails dos administradores (separados por vírgula)
admin_emails = "admin@example.com,gerente@example.com"

# E-mail remetente (deve ser verificado no AWS SES)
from_email = "noreply@feedback-system.com"

