#!/usr/bin/env python3
"""
Teste automatizado para verificar a criação de tarefas via API e Chat IA
"""

import requests
import json
import time

BASE_URL = "https://codespaces-7ad082-8080.app.github.dev"

def test_login():
    """Testa o login e retorna o token"""
    print("🔐 Testando login...")
    
    login_data = {
        "username": "admlucas",
        "password": "123456"
    }
    
    response = requests.post(f"{BASE_URL}/api/auth/signin", json=login_data)
    print(f"Status login: {response.status_code}")
    
    if response.status_code == 200:
        data = response.json()
        token = data.get('accessToken')
        print(f"✅ Login bem-sucedido! Token: {token[:20]}...")
        return token
    else:
        print(f"❌ Erro no login: {response.text}")
        return None

def test_list_projects(token):
    """Testa listagem de projetos"""
    print("\n📋 Testando listagem de projetos...")
    
    headers = {"Authorization": f"Bearer {token}"}
    response = requests.get(f"{BASE_URL}/api/projects", headers=headers)
    
    print(f"Status projetos: {response.status_code}")
    if response.status_code == 200:
        projects = response.json()
        print(f"✅ Projetos encontrados: {len(projects)}")
        for project in projects:
            print(f"  - ID: {project['id']}, Nome: {project['name']}")
        return projects
    else:
        print(f"❌ Erro ao listar projetos: {response.text}")
        return []

def test_list_tasks(token, project_id=None):
    """Testa listagem de tarefas"""
    print(f"\n📝 Testando listagem de tarefas{f' do projeto {project_id}' if project_id else ''}...")
    
    headers = {"Authorization": f"Bearer {token}"}
    url = f"{BASE_URL}/api/tasks"
    if project_id:
        url = f"{BASE_URL}/api/tasks/project/{project_id}"
    
    response = requests.get(url, headers=headers)
    
    print(f"Status tarefas: {response.status_code}")
    if response.status_code == 200:
        tasks = response.json()
        print(f"✅ Tarefas encontradas: {len(tasks)}")
        for task in tasks:
            print(f"  - ID: {task['id']}, Título: {task['title']}, Status: {task['status']}")
        return tasks
    else:
        print(f"❌ Erro ao listar tarefas: {response.text}")
        return []

def test_create_task_direct(token, project_id):
    """Testa criação direta de tarefa via API"""
    print(f"\n➕ Testando criação direta de tarefa no projeto {project_id}...")
    
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    
    task_data = {
        "title": "Teste Automatizado - Tarefa Criada Diretamente",
        "description": "Esta tarefa foi criada por um teste automatizado",
        "status": "BACKLOG",
        "priority": "MEDIUM",
        "project": {"id": project_id}
    }
    
    response = requests.post(f"{BASE_URL}/api/tasks", json=task_data, headers=headers)
    
    print(f"Status criação: {response.status_code}")
    if response.status_code == 200 or response.status_code == 201:
        task = response.json()
        print(f"✅ Tarefa criada com sucesso! ID: {task['id']}")
        return task
    else:
        print(f"❌ Erro ao criar tarefa: {response.text}")
        return None

def test_ai_chat(token):
    """Testa o chat IA para criação de tarefas"""
    print("\n🤖 Testando chat IA...")
    
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    
    # Primeira mensagem - solicitar criação de tarefas
    chat_data = {
        "message": "Criar as seguintes tarefas no projeto teste: Avaliar Desempenho de Fornecedores, Atualizar Documentação Técnica de Projetos, Planejar Ações de Endomarketing"
    }
    
    print("Enviando solicitação para IA...")
    response = requests.post(f"{BASE_URL}/api/ai/chat", json=chat_data, headers=headers)
    
    print(f"Status chat IA: {response.status_code}")
    if response.status_code == 200:
        ai_response = response.json()
        print(f"✅ Resposta da IA: {ai_response.get('message', '')[:100]}...")
        
        # Se a IA pedir confirmação, confirmar
        if ai_response.get('requiresConfirmation'):
            print("🔄 IA pediu confirmação, enviando confirmação...")
            
            confirm_data = {"message": "Sim, confirmo. Execute a ação."}
            confirm_response = requests.post(f"{BASE_URL}/api/ai/chat", json=confirm_data, headers=headers)
            
            print(f"Status confirmação: {confirm_response.status_code}")
            if confirm_response.status_code == 200:
                confirm_result = confirm_response.json()
                print(f"✅ Resultado da confirmação: {confirm_result.get('message', '')[:100]}...")
                return confirm_result
            else:
                print(f"❌ Erro na confirmação: {confirm_response.text}")
        
        return ai_response
    else:
        print(f"❌ Erro no chat IA: {response.text}")
        return None

def main():
    """Executa todos os testes"""
    print("🚀 Iniciando testes automatizados...\n")
    
    # 1. Login
    token = test_login()
    if not token:
        print("❌ Não foi possível fazer login. Parando testes.")
        return
    
    # 2. Listar projetos
    projects = test_list_projects(token)
    if not projects:
        print("❌ Nenhum projeto encontrado. Parando testes.")
        return
    
    # Encontrar projeto "teste"
    test_project = None
    for project in projects:
        if project['name'].lower() == 'teste':
            test_project = project
            break
    
    if not test_project:
        print("❌ Projeto 'teste' não encontrado. Parando testes.")
        return
    
    project_id = test_project['id']
    print(f"🎯 Usando projeto 'teste' (ID: {project_id})")
    
    # 3. Listar tarefas antes
    print("\n📊 Estado ANTES dos testes:")
    tasks_before = test_list_tasks(token, project_id)
    
    # 4. Teste criação direta
    direct_task = test_create_task_direct(token, project_id)
    
    # 5. Teste chat IA
    ai_result = test_ai_chat(token)
    
    # 6. Aguardar um pouco e verificar resultado
    print("\n⏳ Aguardando 3 segundos...")
    time.sleep(3)
    
    # 7. Listar tarefas depois
    print("\n📊 Estado DEPOIS dos testes:")
    tasks_after = test_list_tasks(token, project_id)
    
    # 8. Análise dos resultados
    print("\n📈 ANÁLISE DOS RESULTADOS:")
    print(f"Tarefas antes: {len(tasks_before)}")
    print(f"Tarefas depois: {len(tasks_after)}")
    print(f"Diferença: {len(tasks_after) - len(tasks_before)}")
    
    if len(tasks_after) > len(tasks_before):
        print("✅ SUCESSO: Novas tarefas foram criadas!")
        
        # Mostrar tarefas novas
        new_tasks = [task for task in tasks_after if task not in tasks_before]
        print(f"Novas tarefas criadas ({len(new_tasks)}):")
        for task in new_tasks:
            print(f"  - {task['title']}")
    else:
        print("❌ FALHA: Nenhuma tarefa nova foi criada.")
        
        if direct_task:
            print("⚠️  A criação direta funcionou, mas o chat IA não.")
        else:
            print("⚠️  Nem a criação direta nem o chat IA funcionaram.")

if __name__ == "__main__":
    main()
