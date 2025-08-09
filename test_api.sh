#!/bin/bash

# Teste automatizado para verificar a criação de tarefas via API e Chat IA

BASE_URL="https://codespaces-7ad082-8080.app.github.dev"

echo "🚀 Iniciando testes automatizados..."
echo

# 1. Login
echo "🔐 Testando login..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/signin" \
  -H "Content-Type: application/json" \
  -d '{"username": "admlucas", "password": "123456"}')

echo "Resposta do login: $LOGIN_RESPONSE"

# Extrair token (assumindo formato JSON)
TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "❌ Erro no login. Não foi possível obter token."
    exit 1
fi

echo "✅ Login bem-sucedido! Token: ${TOKEN:0:20}..."
echo

# 2. Listar projetos
echo "📋 Testando listagem de projetos..."
PROJECTS_RESPONSE=$(curl -s -X GET "$BASE_URL/api/projects" \
  -H "Authorization: Bearer $TOKEN")

echo "Projetos encontrados: $PROJECTS_RESPONSE"

# Extrair ID do projeto "teste" (assumindo que existe)
PROJECT_ID=$(echo "$PROJECTS_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

if [ -z "$PROJECT_ID" ]; then
    echo "❌ Nenhum projeto encontrado."
    exit 1
fi

echo "✅ Usando projeto ID: $PROJECT_ID"
echo

# 3. Listar tarefas ANTES
echo "📝 Listando tarefas ANTES dos testes..."
TASKS_BEFORE=$(curl -s -X GET "$BASE_URL/api/tasks/project/$PROJECT_ID" \
  -H "Authorization: Bearer $TOKEN")

echo "Tarefas antes: $TASKS_BEFORE"

# Contar tarefas antes
TASKS_COUNT_BEFORE=$(echo "$TASKS_BEFORE" | grep -o '"id":[0-9]*' | wc -l)
echo "Número de tarefas antes: $TASKS_COUNT_BEFORE"
echo

# 4. Teste criação direta de tarefa
echo "➕ Testando criação direta de tarefa..."
DIRECT_TASK_RESPONSE=$(curl -s -X POST "$BASE_URL/api/tasks" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\": \"Teste Automatizado - Tarefa Criada Diretamente\",
    \"description\": \"Esta tarefa foi criada por um teste automatizado\",
    \"status\": \"BACKLOG\",
    \"priority\": \"MEDIUM\",
    \"project\": {\"id\": $PROJECT_ID}
  }")

echo "Resposta criação direta: $DIRECT_TASK_RESPONSE"
echo

# 5. Teste chat IA - nova abordagem com detecção direta
echo "🤖 Testando chat IA - nova abordagem com detecção direta..."
AI_RESPONSE_1=$(curl -s -X POST "$BASE_URL/api/ai/chat" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message": "Criar tarefas no projeto teste: - Avaliar Desempenho de Fornecedores - Atualizar Documentação Técnica de Projetos - Planejar Ações de Endomarketing"}')

echo "Resposta IA (1ª mensagem): $AI_RESPONSE_1"
echo

# 6. Verificar se a resposta contém sucesso ou se ainda pede confirmação
if echo "$AI_RESPONSE_1" | grep -q "TAREFAS CRIADAS COM SUCESSO"; then
    echo "✅ Nova abordagem funcionou! Tarefas criadas diretamente."
elif echo "$AI_RESPONSE_1" | grep -q "CONFIRMAR_AÇÃO"; then
    echo "🔄 Ainda está pedindo confirmação, enviando confirmação..."

    AI_RESPONSE_2=$(curl -s -X POST "$BASE_URL/api/ai/chat" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{"message": "sim"}')

    echo "Resposta IA (confirmação): $AI_RESPONSE_2"
    echo
else
    echo "⚠️ Resposta inesperada da IA"
fi

# 7. Aguardar um pouco
echo "⏳ Aguardando 5 segundos..."
sleep 5

# 8. Listar tarefas DEPOIS
echo "📝 Listando tarefas DEPOIS dos testes..."
TASKS_AFTER=$(curl -s -X GET "$BASE_URL/api/tasks/project/$PROJECT_ID" \
  -H "Authorization: Bearer $TOKEN")

echo "Tarefas depois: $TASKS_AFTER"

# Contar tarefas depois
TASKS_COUNT_AFTER=$(echo "$TASKS_AFTER" | grep -o '"id":[0-9]*' | wc -l)
echo "Número de tarefas depois: $TASKS_COUNT_AFTER"
echo

# 9. Análise dos resultados
echo "📈 ANÁLISE DOS RESULTADOS:"
echo "Tarefas antes: $TASKS_COUNT_BEFORE"
echo "Tarefas depois: $TASKS_COUNT_AFTER"
DIFFERENCE=$((TASKS_COUNT_AFTER - TASKS_COUNT_BEFORE))
echo "Diferença: $DIFFERENCE"

if [ $DIFFERENCE -gt 0 ]; then
    echo "✅ SUCESSO: $DIFFERENCE nova(s) tarefa(s) foi(ram) criada(s)!"
else
    echo "❌ FALHA: Nenhuma tarefa nova foi criada."
    
    # Verificar se a criação direta funcionou
    if echo "$DIRECT_TASK_RESPONSE" | grep -q '"id":[0-9]*'; then
        echo "⚠️  A criação direta funcionou, mas o chat IA não."
    else
        echo "⚠️  Nem a criação direta nem o chat IA funcionaram."
    fi
fi

echo
echo "🏁 Teste concluído!"
