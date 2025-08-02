# Sistema de Gerenciamento de Projetos

Um sistema completo de gerenciamento de projetos desenvolvido em Java com Spring Boot, incluindo autenticação de usuários, gestão de projetos, kanban de tarefas, upload de atas de reunião e controle de acesso.

## Funcionalidades

### 🔐 Autenticação e Autorização
- Sistema de login e registro de usuários
- Autenticação JWT
- Controle de acesso baseado em roles (Owner, Admin, Member, Viewer)

### 📊 Dashboard
- Visão geral dos projetos e tarefas
- Estatísticas em tempo real
- Acesso rápido às principais funcionalidades

### 🗂️ Gerenciamento de Projetos
- Criação, edição e exclusão de projetos
- Listagem com filtros e busca
- Detalhes completos do projeto
- Controle de status (Ativo, Concluído, Pausado, Cancelado)

### 📋 Sistema Kanban
- Visualização de tarefas em formato Kanban
- Colunas: Backlog, Pronto para Desenvolver, Em Progresso, Em Revisão, Concluído
- Drag & drop para mover tarefas (via API)
- Filtros por projeto ou visualização geral

### ✅ Gestão de Tarefas
- Criação e edição de tarefas
- Atribuição de usuários
- Definição de prioridades (Baixa, Média, Alta, Urgente)
- Controle de prazos
- Histórico de alterações

### 📄 Atas de Reunião
- Upload de arquivos de atas
- Organização por projeto
- Download de documentos
- Controle de versões

### 👥 Controle de Acesso
- Gerenciamento de permissões por projeto
- Diferentes níveis de acesso
- Adição/remoção de usuários em projetos

## Tecnologias Utilizadas

- **Backend**: Java 11, Spring Boot 2.7.14
- **Segurança**: Spring Security, JWT
- **Banco de Dados**: H2 Database (em memória)
- **ORM**: JPA/Hibernate
- **Frontend**: Thymeleaf, Bootstrap 5, JavaScript
- **Build**: Maven
- **Testes**: JUnit 4

## Estrutura do Projeto

```
src/
├── main/
│   ├── java/com/projectmanagement/
│   │   ├── controller/          # Controladores REST e Web
│   │   ├── dto/                 # Data Transfer Objects
│   │   ├── model/               # Entidades JPA
│   │   ├── repository/          # Repositórios JPA
│   │   ├── security/            # Configurações de segurança
│   │   ├── service/             # Lógica de negócio
│   │   └── ProjectManagementApplication.java
│   └── resources/
│       ├── templates/           # Templates Thymeleaf
│       ├── application.properties
│       └── data.sql            # Dados iniciais
└── test/                       # Testes unitários
```

## Como Executar

### Pré-requisitos
- Java 11 ou superior
- Maven 3.6 ou superior

### Passos para execução

1. **Clone o repositório**
```bash
git clone <repository-url>
cd vscode-remote-try-java
```

2. **Compile o projeto**
```bash
mvn clean compile
```

3. **Execute os testes**
```bash
mvn test
```

4. **Execute a aplicação**
```bash
mvn spring-boot:run
```

5. **Acesse a aplicação**
- Interface Web: http://localhost:8080
- Console H2: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:projectmanagement`
  - Username: `sa`
  - Password: `password`

## Usuários de Teste

O sistema vem com usuários pré-cadastrados para teste:

| Usuário | Senha | Nome | Role |
|---------|-------|------|------|
| admin | password123 | Administrador | Owner |
| joao | password123 | João Silva | Admin/Member |
| maria | password123 | Maria Santos | Member |
| pedro | password123 | Pedro Costa | Member |

## API Endpoints

### Autenticação
- `POST /api/auth/signin` - Login
- `POST /api/auth/signup` - Registro

### Projetos
- `GET /api/projects` - Listar projetos do usuário
- `GET /api/projects/{id}` - Detalhes do projeto
- `POST /api/projects` - Criar projeto
- `PUT /api/projects/{id}` - Atualizar projeto
- `DELETE /api/projects/{id}` - Excluir projeto

### Tarefas
- `GET /api/tasks` - Listar tarefas do usuário
- `GET /api/tasks/{id}` - Detalhes da tarefa
- `GET /api/tasks/project/{projectId}` - Tarefas do projeto
- `POST /api/tasks` - Criar tarefa
- `PUT /api/tasks/{id}` - Atualizar tarefa
- `PUT /api/tasks/{id}/status` - Atualizar status da tarefa
- `DELETE /api/tasks/{id}` - Excluir tarefa

## Páginas Web

- `/` - Redirecionamento para dashboard
- `/login` - Página de login
- `/register` - Página de registro
- `/dashboard` - Dashboard principal
- `/projects` - Listagem de projetos
- `/projects/{id}` - Detalhes do projeto
- `/projects/{id}/kanban` - Kanban do projeto
- `/kanban` - Kanban geral

## Configuração

As principais configurações estão no arquivo `application.properties`:

```properties
# Banco de dados H2
spring.datasource.url=jdbc:h2:mem:projectmanagement
spring.h2.console.enabled=true

# JWT
app.jwtSecret=projectManagementSecretKey
app.jwtExpirationMs=86400000

# Upload de arquivos
spring.servlet.multipart.max-file-size=10MB
app.upload.dir=uploads
```

## Arquitetura

O sistema segue uma arquitetura em camadas:

1. **Controller Layer**: Controladores REST e Web
2. **Service Layer**: Lógica de negócio
3. **Repository Layer**: Acesso a dados
4. **Model Layer**: Entidades JPA
5. **Security Layer**: Autenticação e autorização

## Funcionalidades Implementadas

✅ **Autenticação de usuários**
✅ **Gestão de projetos**
✅ **Sistema de tarefas com Kanban**
✅ **Upload de atas de reunião**
✅ **Controle de acesso por projeto**
✅ **Dashboard com estatísticas**
✅ **API REST completa**
✅ **Interface web responsiva**
✅ **Testes unitários**

## Próximos Passos

- [ ] Implementar notificações em tempo real
- [ ] Adicionar relatórios e gráficos
- [ ] Melhorar a interface do Kanban com drag & drop
- [ ] Implementar comentários nas tarefas
- [ ] Adicionar histórico de atividades
- [ ] Implementar backup automático
- [ ] Adicionar integração com calendário

## Contribuição

Para contribuir com o projeto:

1. Faça um fork do repositório
2. Crie uma branch para sua feature
3. Implemente as mudanças
4. Execute os testes
5. Faça um pull request

## Licença

Este projeto está sob a licença MIT. Veja o arquivo LICENSE para mais detalhes.
