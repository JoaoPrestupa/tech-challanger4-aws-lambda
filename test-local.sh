#!/bin/bash
# ========================================
# Script de Teste Local
# Sistema de Feedback
# ========================================

set -e

echo "========================================"
echo " TESTES LOCAIS - SISTEMA DE FEEDBACK"
echo "========================================"
echo ""

echo "[1/3] Compilando aplicação..."
mvn clean package -DskipTests
echo "Compilação OK!"
echo ""

echo "[2/3] Iniciando aplicação Spring Boot..."
mvn spring-boot:run &
SPRING_PID=$!
echo "Aguardando aplicação iniciar (30 segundos)..."
sleep 30
echo ""

echo "[3/3] Executando testes..."
echo ""

echo "Teste 1: Health Check"
curl -X GET http://localhost:8080/api/avaliacoes/health
echo ""
echo ""

echo "Teste 2: Avaliação Positiva (nota 9)"
curl -X POST http://localhost:8080/api/avaliacoes \
  -H "Content-Type: application/json" \
  -d '{"descricao":"Curso excelente!","nota":9}'
echo ""
echo ""

echo "Teste 3: Avaliação Média (nota 5)"
curl -X POST http://localhost:8080/api/avaliacoes \
  -H "Content-Type: application/json" \
  -d '{"descricao":"Curso ok, mas pode melhorar.","nota":5}'
echo ""
echo ""

echo "Teste 4: Avaliação Crítica (nota 2) - Dispara notificação"
curl -X POST http://localhost:8080/api/avaliacoes \
  -H "Content-Type: application/json" \
  -d '{"descricao":"Muito insatisfeito com o curso.","nota":2}'
echo ""
echo ""

echo "========================================"
echo " TESTES CONCLUÍDOS"
echo "========================================"
echo ""
echo "Parando aplicação..."
kill $SPRING_PID

echo "Aplicação parada."
@echo off
REM ========================================
REM Script de Teste Local
REM Sistema de Feedback
REM ========================================

echo ========================================
echo  TESTES LOCAIS - SISTEMA DE FEEDBACK
echo ========================================
echo.

echo [1/3] Compilando aplicação...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo ERRO: Falha na compilação
    exit /b 1
)
echo Compilação OK!
echo.

echo [2/3] Iniciando aplicação Spring Boot...
start "Feedback System" mvn spring-boot:run
echo Aguardando aplicação iniciar (30 segundos)...
timeout /t 30 /nobreak > nul
echo.

echo [3/3] Executando testes...
echo.

echo Teste 1: Health Check
curl -X GET http://localhost:8080/api/avaliacoes/health
echo.
echo.

echo Teste 2: Avaliação Positiva (nota 9)
curl -X POST http://localhost:8080/api/avaliacoes ^
  -H "Content-Type: application/json" ^
  -d "{\"descricao\":\"Curso excelente!\",\"nota\":9}"
echo.
echo.

echo Teste 3: Avaliação Média (nota 5)
curl -X POST http://localhost:8080/api/avaliacoes ^
  -H "Content-Type: application/json" ^
  -d "{\"descricao\":\"Curso ok, mas pode melhorar.\",\"nota\":5}"
echo.
echo.

echo Teste 4: Avaliação Crítica (nota 2) - Dispara notificação
curl -X POST http://localhost:8080/api/avaliacoes ^
  -H "Content-Type: application/json" ^
  -d "{\"descricao\":\"Muito insatisfeito com o curso.\",\"nota\":2}"
echo.
echo.

echo ========================================
echo  TESTES CONCLUÍDOS
echo ========================================
echo.
echo Para parar a aplicação, feche a janela "Feedback System"
echo ou pressione Ctrl+C nela.
echo.

pause

