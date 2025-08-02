# 🚀 Sistema de Gerenciamento de Projetos

<div align="center">

![Java](https://img.shields.io/badge/Java-11-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.14-brightgreen?style=for-the-badge&logo=spring)
![Angular](https://img.shields.io/badge/Angular-16-red?style=for-the-badge&logo=angular)
![H2 Database](https://img.shields.io/badge/H2-Database-blue?style=for-the-badge&logo=h2)
![JWT](https://img.shields.io/badge/JWT-Authentication-black?style=for-the-badge&logo=jsonwebtokens)

**Sistema completo de gerenciamento de projetos e tarefas com interface moderna e funcionalidades avançadas**

[🎯 Funcionalidades](#-funcionalidades) • [🛠️ Tecnologias](#️-tecnologias) • [🚀 Como Executar](#-como-executar) • [📱 Screenshots](#-screenshots) • [🔧 API](#-api-endpoints)

</div>

---

## 📋 Sobre o Projeto

Este é um sistema completo de **gerenciamento de projetos e tarefas** desenvolvido com **Spring Boot** no backend e **Angular** no frontend. O sistema oferece uma interface moderna e intuitiva para gerenciar projetos, tarefas, equipes e acompanhar o progresso através de dashboards e quadros Kanban.

### ✨ Principais Características

- 🔐 **Autenticação JWT** segura e robusta
- 📊 **Dashboard interativo** com métricas em tempo real  
- 📋 **Quadro Kanban** para gestão visual de tarefas
- 👥 **Gestão de equipes** e controle de acesso
- 💬 **Sistema de comentários** em tarefas
- ✅ **Checklists** para tarefas complexas
- 📄 **Detalhes completos** de projetos com abas organizadas
- 📱 **Interface responsiva** para todos os dispositivos
- 🎨 **Design moderno** com UX otimizada

---

## 🎯 Funcionalidades

### 🏠 Dashboard
- **Visão geral** de todos os projetos e tarefas
- **Métricas visuais** com gráficos e estatísticas
- **Atividades recentes** e notificações
- **Acesso rápido** às funcionalidades principais

### 📋 Gestão de Projetos
- ✅ **Criar, editar e excluir** projetos
- 📊 **Acompanhar progresso** com barras visuais
- 👥 **Gerenciar equipe** e permissões de acesso
- 📅 **Controlar prazos** e marcos importantes
- 📄 **Página de detalhes** completa com abas:
  - **Tarefas**: Lista visual de todas as tarefas
  - **Acessos**: Usuários com permissão no projeto  
  - **Atas de Reunião**: Documentação e decisões

### 📝 Gestão de Tarefas
- 🎯 **Quadro Kanban** interativo com drag & drop
- 🏷️ **Prioridades** e status personalizáveis
- 👤 **Atribuição** de responsáveis
- 💬 **Sistema de comentários** para colaboração
- ✅ **Checklists** para quebrar tarefas complexas
- 📊 **Acompanhamento** de progresso individual

### 🔐 Segurança e Autenticação
- 🛡️ **JWT Authentication** com interceptors automáticos
- 🔒 **Controle de acesso** baseado em papéis
- 🚪 **Logout automático** em caso de token expirado
- 🔄 **Renovação** transparente de sessões

---

## 🛠️ Tecnologias

### Backend
- **Java 11** - Linguagem principal
- **Spring Boot 2.7.14** - Framework principal
- **Spring Security** - Autenticação e autorização
- **Spring Data JPA** - Persistência de dados
- **H2 Database** - Banco de dados persistente
- **JWT** - Tokens de autenticação
- **Maven** - Gerenciamento de dependências

### Frontend  
- **Angular 16** - Framework frontend
- **TypeScript** - Linguagem tipada
- **RxJS** - Programação reativa
- **CSS3** - Estilização moderna
- **Font Awesome** - Ícones
- **Responsive Design** - Layout adaptativo

### Ferramentas de Desenvolvimento
- **VS Code** - Editor principal
- **Dev Containers** - Ambiente de desenvolvimento
- **Git** - Controle de versão
- **Docker** - Containerização

---

## 🚀 Como Executar

### 📋 Pré-requisitos
- **Java 11** ou superior
- **Node.js 16** ou superior  
- **Maven 3.6** ou superior
- **Git** para clonar o repositório

### 🔧 Instalação

1. **Clone o repositório**
```bash
git clone <repository-url>
cd vscode-remote-try-java
```

2. **Execute o Backend**
```bash
# Compile e execute o Spring Boot
mvn clean compile
mvn spring-boot:run
```

3. **Execute o Frontend**
```bash
# Navegue para o diretório frontend
cd frontend

# Instale as dependências
npm install

# Execute o servidor de desenvolvimento
npm start
```

4. **Acesse a aplicação**
- **Frontend**: http://localhost:4200 (ou porta alternativa)
- **Backend API**: http://localhost:8080
- **Console H2**: http://localhost:8080/h2-console

### 🐳 Usando Docker (Opcional)
```bash
# Build da imagem
docker build -t project-management .

# Execute o container
docker run -p 8080:8080 project-management
```

---

## 👤 Primeiros Passos

### 1. **Criar Usuário**
- Acesse a aplicação frontend
- Clique em "Registrar" se não houver usuários
- Ou use as credenciais padrão se houver dados iniciais

### 2. **Criar Primeiro Projeto**
- No dashboard, clique em "Novo Projeto"
- Preencha nome, descrição e datas
- Defina o status inicial

### 3. **Adicionar Tarefas**
- Entre no projeto criado
- Na aba "Tarefas", clique em "Nova Tarefa"
- Ou use o Kanban para gestão visual

### 4. **Gerenciar Equipe**
- Na aba "Acessos" do projeto
- Adicione usuários com diferentes permissões
- Controle quem pode ver/editar o projeto

---

## ✅ Status de Implementação

### 🎯 **Funcionalidades Completas**
- ✅ **Autenticação JWT** com interceptors automáticos
- ✅ **Dashboard** com métricas e estatísticas
- ✅ **CRUD de Projetos** completo
- ✅ **CRUD de Tarefas** completo
- ✅ **Quadro Kanban** interativo
- ✅ **Sistema de Comentários** em tarefas
- ✅ **Checklists** para tarefas
- ✅ **Página de Detalhes** do projeto com abas
- ✅ **Controle de Acesso** por projeto
- ✅ **Interface Responsiva** para mobile/desktop

### 🚧 **Funcionalidades Futuras**
- 🔄 **Notificações** em tempo real
- 🔄 **Upload de Arquivos** em tarefas
- 🔄 **Relatórios** e exportação
- 🔄 **Integração** com calendário
- 🔄 **API de Webhooks** para integrações

---

## 📱 Capturas de Tela

### 🏠 Dashboard Principal
- **Visão geral** de todos os projetos e tarefas
- **Métricas visuais** com estatísticas em tempo real
- **Navegação intuitiva** entre módulos

### 📋 Quadro Kanban
- **Gestão visual** de tarefas com drag & drop
- **Colunas personalizáveis** por status
- **Filtros** por projeto, responsável e prioridade

### 📄 Detalhes do Projeto
- **Página completa** com informações organizadas
- **Abas funcionais**: Tarefas, Acessos, Atas de Reunião
- **Progress bar** visual do progresso

### 💬 Sistema de Comentários
- **Colaboração** em tempo real nas tarefas
- **Histórico** completo de interações
- **Notificações** de novas mensagens

---

## 🔧 API Endpoints

### 🔐 Autenticação
```http
POST /api/auth/signin     # Login do usuário
POST /api/auth/signup     # Registro de novo usuário
```

### 📋 Projetos
```http
GET    /api/projects           # Listar projetos do usuário
GET    /api/projects/{id}      # Detalhes do projeto
POST   /api/projects           # Criar novo projeto
PUT    /api/projects/{id}      # Atualizar projeto
DELETE /api/projects/{id}      # Excluir projeto
```

### 📝 Tarefas
```http
GET    /api/tasks                    # Listar tarefas do usuário
GET    /api/tasks/{id}               # Detalhes da tarefa
GET    /api/projects/{id}/tasks      # Tarefas do projeto
POST   /api/tasks                   # Criar nova tarefa
PUT    /api/tasks/{id}              # Atualizar tarefa
PUT    /api/tasks/{id}/status       # Atualizar status
PUT    /api/tasks/{id}/assign       # Atribuir responsável
DELETE /api/tasks/{id}              # Excluir tarefa
```

### 💬 Comentários
```http
GET  /api/tasks/{id}/comments       # Listar comentários
POST /api/tasks/{id}/comments       # Adicionar comentário
```

### ✅ Checklists
```http
GET    /api/tasks/{id}/checklist           # Listar itens
POST   /api/tasks/{id}/checklist           # Adicionar item
PUT    /api/tasks/{taskId}/checklist/{id}/toggle  # Marcar/desmarcar
DELETE /api/tasks/{taskId}/checklist/{id}  # Excluir item
```

### 👥 Usuários
```http
GET /api/users        # Listar usuários
GET /api/users/me     # Usuário atual
```

---

## 📁 Estrutura do Projeto

```
flow-tasks/
├── 📁 src/main/java/com/projectmanagement/
│   ├── 📁 controller/          # Controladores REST API
│   ├── 📁 service/            # Lógica de negócio
│   ├── 📁 repository/         # Acesso a dados (JPA)
│   ├── 📁 model/             # Entidades do banco
│   ├── 📁 security/          # Configurações JWT
│   ├── 📁 dto/              # Data Transfer Objects
│   └── 📁 config/           # Configurações Spring
├── 📁 frontend/
│   ├── 📁 src/app/
│   │   ├── 📁 components/    # Componentes Angular
│   │   ├── 📁 services/     # Serviços e APIs
│   │   ├── 📁 models/       # Interfaces TypeScript
│   │   ├── 📁 guards/       # Guards de autenticação
│   │   ├── 📁 interceptors/ # Interceptors HTTP
│   │   └── 📁 styles/       # Estilos globais
│   ├── 📄 package.json      # Dependências Node.js
│   └── 📄 angular.json      # Configuração Angular
├── 📄 pom.xml               # Dependências Maven
├── 📄 README.md             # Este arquivo
└── 📄 .gitignore           # Arquivos ignorados
```

---

## 🏗️ Arquitetura

### Backend (Spring Boot)
```
src/main/java/com/projectmanagement/
├── controller/          # Controladores REST
├── service/            # Lógica de negócio
├── repository/         # Acesso a dados
├── model/             # Entidades JPA
├── security/          # Configurações de segurança
├── dto/              # Data Transfer Objects
└── config/           # Configurações gerais
```

### Frontend (Angular)
```
frontend/src/app/
├── components/        # Componentes da UI
├── services/         # Serviços e APIs
├── models/          # Interfaces TypeScript
├── guards/          # Guards de rota
├── interceptors/    # Interceptors HTTP
└── styles/         # Estilos globais
```

---

## 🔒 Configuração de Segurança

### JWT Configuration
```properties
# JWT Secret (deve ser alterado em produção)
app.jwtSecret=projectManagementSecretKey
app.jwtExpirationMs=86400000  # 24 horas
```

### Database Configuration
```properties
# H2 Database (persistente)
spring.datasource.url=jdbc:h2:file:./data/projectmanagement
spring.h2.console.enabled=true
```

---

## 🧪 Testes

### Executar Testes Backend
```bash
mvn test
```

### Executar Testes Frontend
```bash
cd frontend
npm test
```

### Cobertura de Testes
```bash
# Backend
mvn jacoco:report

# Frontend  
ng test --code-coverage
```

---

## 🚀 Deploy

### Produção
1. **Build do Frontend**
```bash
cd frontend
ng build --prod
```

2. **Package do Backend**
```bash
mvn clean package
```

3. **Execute o JAR**
```bash
java -jar target/project-management-system-1.0.0.jar
```

### Docker
```dockerfile
# Dockerfile já configurado
docker build -t project-management .
docker run -p 8080:8080 project-management
```

---

## 🤝 Contribuição

1. **Fork** o projeto
2. **Crie** uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. **Commit** suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. **Push** para a branch (`git push origin feature/AmazingFeature`)
5. **Abra** um Pull Request

---

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

---

## 👨‍💻 Autor

**Desenvolvido com ❤️ por [Seu Nome]**

- 📧 Email: seu.email@exemplo.com
- 💼 LinkedIn: [Seu LinkedIn](https://linkedin.com/in/seu-perfil)
- 🐙 GitHub: [Seu GitHub](https://github.com/seu-usuario)

---

<div align="center">

**⭐ Se este projeto te ajudou, considere dar uma estrela!**

</div>
