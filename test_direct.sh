#!/bin/bash

# Teste direto para verificar a nova detecção de padrões

BASE_URL="https://codespaces-7ad082-8080.app.github.dev"

echo "🧪 Teste direto da nova detecção de padrões..."
echo

# Simular uma sessão autenticada (usando o token do browser)
echo "📝 Testando mensagem: 'Criar as seguintes tarefas no projeto teste'"

# Fazer a requisição diretamente
curl -X POST "$BASE_URL/api/ai/chat" \
  -H "Content-Type: application/json" \
  -H "Cookie: JSESSIONID=C151398473E0261E63C9B052F61160D3" \
  -d '{"message": "Criar as seguintes tarefas no projeto teste: - Avaliar Desempenho de Fornecedores - Atualizar Documentação Técnica de Projetos - Planejar Ações de Endomarketing"}' \
  -v

echo
echo "🏁 Teste concluído!"
