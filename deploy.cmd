@echo off
REM ========================================
REM Script de Deploy da Aplicação
REM Sistema de Feedback AWS Lambda
REM ========================================

echo ========================================
echo  DEPLOY - SISTEMA DE FEEDBACK
echo ========================================
echo.

echo [1/5] Compilando aplicação com Maven...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo ERRO: Falha na compilação Maven
    exit /b 1
)
echo Compilação concluída com sucesso!
echo.

echo [2/5] Inicializando Terraform...
cd terraform
terraform init
if %errorlevel% neq 0 (
    echo ERRO: Falha na inicialização do Terraform
    exit /b 1
)
echo.

echo [3/5] Validando configuração Terraform...
terraform validate
if %errorlevel% neq 0 (
    echo ERRO: Configuração Terraform inválida
    exit /b 1
)
echo.

echo [4/5] Planejando infraestrutura...
terraform plan -out=tfplan
if %errorlevel% neq 0 (
    echo ERRO: Falha no planejamento Terraform
    exit /b 1
)
echo.

echo [5/5] Aplicando infraestrutura...
set /p CONFIRM="Deseja aplicar as mudanças? (sim/nao): "
if /i "%CONFIRM%"=="sim" (
    terraform apply tfplan
    if %errorlevel% neq 0 (
        echo ERRO: Falha na aplicação do Terraform
        exit /b 1
    )
    echo.
    echo ========================================
    echo  DEPLOY CONCLUÍDO COM SUCESSO!
    echo ========================================
    echo.
    terraform output
) else (
    echo Deploy cancelado pelo usuário
)

cd ..

