package com.projectmanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectmanagement.controller.ProjectController;
import com.projectmanagement.controller.TaskController;
import com.projectmanagement.model.Project;
import com.projectmanagement.model.ProjectStatus;
import com.projectmanagement.model.Task;
import com.projectmanagement.model.TaskPriority;
import com.projectmanagement.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service MCP (Model Context Protocol) para execução de ferramentas
 * Faz interface entre o protocolo MCP e os controllers existentes
 */
@Service
public class MCPClient {

    @Autowired
    private ProjectController projectController;

    @Autowired
    private TaskController taskController;

    @Autowired
    private GitHubMCPService gitHubMCPService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Retorna lista de todas as ferramentas disponíveis com seus schemas
     */
    public List<Map<String, Object>> getAvailableTools() {
        List<Map<String, Object>> tools = new ArrayList<>();

        // create_task
        tools.add(createToolSchema(
            "create_task",
            "Cria uma nova tarefa em um projeto EXISTENTE. Use list_projects primeiro para obter o projectId correto. NUNCA invente IDs de projeto.",
            Map.of(
                "title", Map.of("type", "string", "description", "Título da tarefa"),
                "projectId", Map.of("type", "integer", "description", "ID do projeto EXISTENTE (obtenha via list_projects)"),
                "description", Map.of("type", "string", "description", "Descrição da tarefa (opcional)")
            ),
            Arrays.asList("title", "projectId")
        ));

        // create_project
        tools.add(createToolSchema(
            "create_project",
            "Cria um novo projeto",
            Map.of(
                "name", Map.of("type", "string", "description", "Nome do projeto"),
                "description", Map.of("type", "string", "description", "Descrição do projeto (opcional)")
            ),
            Arrays.asList("name")
        ));

        // list_projects
        tools.add(createToolSchema(
            "list_projects",
            "Lista todos os projetos disponíveis. SEMPRE use esta ferramenta ANTES de criar tarefas para verificar projetos existentes.",
            Map.of(),
            Arrays.asList()
        ));

        // list_tasks
        tools.add(createToolSchema(
            "list_tasks",
            "Lista tarefas de um projeto específico ou todas as tarefas",
            Map.of(
                "projectId", Map.of("type", "integer", "description", "ID do projeto (opcional)")
            ),
            Arrays.asList()
        ));

        // update_task
        tools.add(createToolSchema(
            "update_task",
            "Atualiza informações de uma tarefa ESPECÍFICA. Use list_tasks primeiro para obter o taskId correto. NUNCA invente IDs de tarefa.",
            Map.of(
                "taskId", Map.of("type", "integer", "description", "ID da tarefa EXISTENTE (obtenha via list_tasks)"),
                "title", Map.of("type", "string", "description", "Novo título (opcional)"),
                "description", Map.of("type", "string", "description", "Nova descrição (opcional)"),
                "status", Map.of("type", "string", "description", "Novo status: BACKLOG, IN_PROGRESS, DONE (opcional)")
            ),
            Arrays.asList("taskId")
        ));

        // move_task
        tools.add(createToolSchema(
            "move_task",
            "Move uma tarefa ESPECÍFICA para um novo status. Use list_tasks primeiro para obter o taskId correto. NUNCA invente IDs de tarefa.",
            Map.of(
                "taskId", Map.of("type", "integer", "description", "ID da tarefa EXISTENTE (obtenha via list_tasks)"),
                "status", Map.of("type", "string", "description", "Novo status: BACKLOG, IN_PROGRESS, DONE")
            ),
            Arrays.asList("taskId", "status")
        ));

        // === FERRAMENTAS GITHUB ===

        // github_list_repositories
        tools.add(createToolSchema(
            "github_list_repositories",
            "Lista repositórios do usuário autenticado no GitHub",
            Map.of(
                "type", Map.of("type", "string", "description", "Tipo de repositório: owner, member, public (padrão: owner)"),
                "sort", Map.of("type", "string", "description", "Ordenação: created, updated, pushed, full_name (padrão: updated)"),
                "direction", Map.of("type", "string", "description", "Direção: asc, desc (padrão: desc)"),
                "per_page", Map.of("type", "integer", "description", "Itens por página (padrão: 30)"),
                "page", Map.of("type", "integer", "description", "Número da página (padrão: 1)")
            ),
            Arrays.asList()
        ));

        // github_get_repository
        tools.add(createToolSchema(
            "github_get_repository",
            "Obtém detalhes de um repositório específico",
            Map.of(
                "owner", Map.of("type", "string", "description", "Proprietário do repositório"),
                "repo", Map.of("type", "string", "description", "Nome do repositório")
            ),
            Arrays.asList("owner", "repo")
        ));

        // github_list_issues
        tools.add(createToolSchema(
            "github_list_issues",
            "Lista issues de um repositório",
            Map.of(
                "owner", Map.of("type", "string", "description", "Proprietário do repositório"),
                "repo", Map.of("type", "string", "description", "Nome do repositório"),
                "state", Map.of("type", "string", "description", "Estado: open, closed, all (padrão: open)"),
                "sort", Map.of("type", "string", "description", "Ordenação: created, updated, comments (padrão: created)"),
                "direction", Map.of("type", "string", "description", "Direção: asc, desc (padrão: desc)"),
                "per_page", Map.of("type", "integer", "description", "Itens por página (padrão: 30)"),
                "page", Map.of("type", "integer", "description", "Número da página (padrão: 1)")
            ),
            Arrays.asList("owner", "repo")
        ));

        // github_create_issue
        tools.add(createToolSchema(
            "github_create_issue",
            "Cria uma nova issue em um repositório",
            Map.of(
                "owner", Map.of("type", "string", "description", "Proprietário do repositório"),
                "repo", Map.of("type", "string", "description", "Nome do repositório"),
                "title", Map.of("type", "string", "description", "Título da issue"),
                "body", Map.of("type", "string", "description", "Descrição da issue (opcional)"),
                "labels", Map.of("type", "array", "description", "Labels para a issue (opcional)"),
                "assignees", Map.of("type", "array", "description", "Usuários para atribuir (opcional)")
            ),
            Arrays.asList("owner", "repo", "title")
        ));

        // github_list_pull_requests
        tools.add(createToolSchema(
            "github_list_pull_requests",
            "Lista pull requests de um repositório",
            Map.of(
                "owner", Map.of("type", "string", "description", "Proprietário do repositório"),
                "repo", Map.of("type", "string", "description", "Nome do repositório"),
                "state", Map.of("type", "string", "description", "Estado: open, closed, all (padrão: open)"),
                "sort", Map.of("type", "string", "description", "Ordenação: created, updated, popularity (padrão: created)"),
                "direction", Map.of("type", "string", "description", "Direção: asc, desc (padrão: desc)"),
                "per_page", Map.of("type", "integer", "description", "Itens por página (padrão: 30)"),
                "page", Map.of("type", "integer", "description", "Número da página (padrão: 1)")
            ),
            Arrays.asList("owner", "repo")
        ));

        // github_get_file_content
        tools.add(createToolSchema(
            "github_get_file_content",
            "Obtém conteúdo de um arquivo do repositório",
            Map.of(
                "owner", Map.of("type", "string", "description", "Proprietário do repositório"),
                "repo", Map.of("type", "string", "description", "Nome do repositório"),
                "path", Map.of("type", "string", "description", "Caminho do arquivo"),
                "ref", Map.of("type", "string", "description", "Branch, tag ou commit SHA (opcional)")
            ),
            Arrays.asList("owner", "repo", "path")
        ));

        // github_list_commits
        tools.add(createToolSchema(
            "github_list_commits",
            "Lista commits de um repositório",
            Map.of(
                "owner", Map.of("type", "string", "description", "Proprietário do repositório"),
                "repo", Map.of("type", "string", "description", "Nome do repositório"),
                "sha", Map.of("type", "string", "description", "Branch, tag ou commit SHA (opcional)"),
                "path", Map.of("type", "string", "description", "Filtrar por caminho (opcional)"),
                "author", Map.of("type", "string", "description", "Filtrar por autor (opcional)"),
                "per_page", Map.of("type", "integer", "description", "Itens por página (padrão: 30)"),
                "page", Map.of("type", "integer", "description", "Número da página (padrão: 1)")
            ),
            Arrays.asList("owner", "repo")
        ));

        // github_search_repositories
        tools.add(createToolSchema(
            "github_search_repositories",
            "Busca repositórios no GitHub",
            Map.of(
                "query", Map.of("type", "string", "description", "Consulta de busca (ex: 'machine learning language:python')"),
                "sort", Map.of("type", "string", "description", "Ordenação: stars, forks, updated (padrão: stars)"),
                "order", Map.of("type", "string", "description", "Direção: asc, desc (padrão: desc)"),
                "per_page", Map.of("type", "integer", "description", "Itens por página (padrão: 30)"),
                "page", Map.of("type", "integer", "description", "Número da página (padrão: 1)")
            ),
            Arrays.asList("query")
        ));

        return tools;
    }

    /**
     * Retorna o schema de uma ferramenta específica
     */
    public Map<String, Object> getToolSchema(String toolName) {
        List<Map<String, Object>> tools = getAvailableTools();
        return tools.stream()
            .filter(tool -> toolName.equals(tool.get("name")))
            .findFirst()
            .orElse(null);
    }

    /**
     * Executa uma ferramenta específica com os argumentos fornecidos
     */
    public Map<String, Object> executeTool(String toolName, Map<String, Object> arguments) {
        return executeTool(toolName, arguments, null);
    }

    /**
     * Executa uma ferramenta específica com contexto de usuário
     */
    public Map<String, Object> executeTool(String toolName, Map<String, Object> arguments, UserPrincipal user) {
        try {
            switch (toolName) {
                case "create_task":
                    return executeCreateTask(arguments, user);
                case "create_project":
                    return executeCreateProject(arguments, user);
                case "list_projects":
                    return executeListProjects(user);
                case "list_tasks":
                    return executeListTasks(arguments, user);
                case "update_task":
                    return executeUpdateTask(arguments, user);
                case "move_task":
                    return executeMoveTask(arguments, user);

                // === FERRAMENTAS GITHUB ===
                case "github_list_repositories":
                    return executeGitHubListRepositories(arguments, user);
                case "github_get_repository":
                    return executeGitHubGetRepository(arguments, user);
                case "github_list_issues":
                    return executeGitHubListIssues(arguments, user);
                case "github_create_issue":
                    return executeGitHubCreateIssue(arguments, user);
                case "github_list_pull_requests":
                    return executeGitHubListPullRequests(arguments, user);
                case "github_get_file_content":
                    return executeGitHubGetFileContent(arguments, user);
                case "github_list_commits":
                    return executeGitHubListCommits(arguments, user);
                case "github_search_repositories":
                    return executeGitHubSearchRepositories(arguments, user);

                default:
                    return createErrorResponse("Ferramenta não encontrada: " + toolName);
            }
        } catch (Exception e) {
            return createErrorResponse("Erro ao executar " + toolName + ": " + e.getMessage());
        }
    }

    // Métodos privados para execução de cada ferramenta

    private Map<String, Object> executeCreateTask(Map<String, Object> args, UserPrincipal user) {
        try {
            String title = (String) args.get("title");
            Long projectId = Long.valueOf(String.valueOf(args.get("projectId")));
            String description = (String) args.getOrDefault("description", "Tarefa criada via IA");

            // Validar se o projeto existe primeiro
            Authentication auth = createAuthentication(user);
            ResponseEntity<List<Project>> projectsResponse = projectController.getAllProjects(auth);
            List<Project> projects = projectsResponse.getBody();

            boolean projectExists = projects != null && projects.stream()
                .anyMatch(p -> p.getId().equals(projectId));

            if (!projectExists) {
                String availableProjects = projects != null ?
                    projects.stream()
                        .map(p -> "ID: " + p.getId() + " - " + p.getName())
                        .collect(java.util.stream.Collectors.joining(", ")) :
                    "nenhum";
                return createErrorResponse("Projeto com ID " + projectId + " não existe. Projetos disponíveis: " + availableProjects + ". Use list_projects para ver os IDs corretos.");
            }

            Task task = new Task();
            task.setTitle(title);
            task.setDescription(description);
            task.setStatus("BACKLOG");
            task.setPriority(TaskPriority.MEDIUM);

            // Buscar o projeto
            Project project = new Project();
            project.setId(projectId);
            task.setProject(project);

            ResponseEntity<Task> response = taskController.createTask(task, auth);

            if (response.getStatusCode().is2xxSuccessful()) {
                Task createdTask = response.getBody();
                return Map.of(
                    "success", true,
                    "taskId", createdTask.getId(),
                    "title", title,
                    "projectId", projectId,
                    "message", "Tarefa '" + title + "' criada com sucesso no projeto ID " + projectId + "!"
                );
            } else {
                return createErrorResponse("Erro ao criar tarefa");
            }
        } catch (Exception e) {
            return createErrorResponse("Erro ao criar tarefa: " + e.getMessage());
        }
    }

    private Map<String, Object> executeCreateProject(Map<String, Object> args, UserPrincipal user) {
        try {
            String name = (String) args.get("name");
            String description = (String) args.getOrDefault("description", "Projeto criado via IA");

            Project project = new Project();
            project.setName(name);
            project.setDescription(description);
            project.setStatus(ProjectStatus.ACTIVE);

            Authentication auth = createAuthentication(user);
            ResponseEntity<Project> response = projectController.createProject(project, auth);

            if (response.getStatusCode().is2xxSuccessful()) {
                Project createdProject = response.getBody();
                return Map.of(
                    "success", true,
                    "projectId", createdProject.getId(),
                    "name", name,
                    "message", "Projeto '" + name + "' criado com sucesso!"
                );
            } else {
                return createErrorResponse("Erro ao criar projeto");
            }
        } catch (Exception e) {
            return createErrorResponse("Erro ao criar projeto: " + e.getMessage());
        }
    }

    private Map<String, Object> executeListProjects(UserPrincipal user) {
        try {
            Authentication auth = createAuthentication(user);
            ResponseEntity<List<Project>> response = projectController.getAllProjects(auth);
            List<Project> projects = response.getBody();

            List<Map<String, Object>> projectMaps = new ArrayList<>();
            if (projects != null) {
                for (Project project : projects) {
                    Map<String, Object> projectMap = new HashMap<>();
                    projectMap.put("id", project.getId());
                    projectMap.put("name", project.getName());
                    projectMap.put("description", project.getDescription());
                    projectMap.put("status", project.getStatus());
                    projectMaps.add(projectMap);
                }
            }

            String message = projectMaps.isEmpty() ?
                "Nenhum projeto encontrado. Crie um projeto primeiro antes de criar tarefas." :
                "Projetos disponíveis encontrados. Use o ID do projeto para criar tarefas.";

            return Map.of(
                "success", true,
                "projects", projectMaps,
                "count", projectMaps.size(),
                "message", message,
                "instruction", "Para criar tarefas, use o 'id' de um destes projetos existentes como 'projectId'"
            );
        } catch (Exception e) {
            return createErrorResponse("Erro ao listar projetos: " + e.getMessage());
        }
    }

    private Map<String, Object> executeListTasks(Map<String, Object> args, UserPrincipal user) {
        try {
            Authentication auth = createAuthentication(user);
            Object projectIdObj = args.get("projectId");

            List<Task> tasks;
            if (projectIdObj != null) {
                Long projectId = Long.valueOf(String.valueOf(projectIdObj));
                ResponseEntity<List<Task>> response = taskController.getTasksByProject(projectId, auth);
                tasks = response.getBody();
            } else {
                ResponseEntity<List<Task>> response = taskController.getAllTasks(auth);
                tasks = response.getBody();
            }

            List<Map<String, Object>> taskMaps = new ArrayList<>();
            if (tasks != null) {
                for (Task task : tasks) {
                    Map<String, Object> taskMap = new HashMap<>();
                    taskMap.put("id", task.getId());
                    taskMap.put("title", task.getTitle());
                    taskMap.put("description", task.getDescription());
                    taskMap.put("status", task.getStatus());
                    taskMap.put("priority", task.getPriority());
                    taskMap.put("projectId", task.getProject().getId());
                    taskMaps.add(taskMap);
                }
            }

            String message = taskMaps.isEmpty() ?
                "Nenhuma tarefa encontrada." :
                "Tarefas encontradas. Use o 'id' da tarefa para atualizar ou mover tarefas específicas.";

            return Map.of(
                "success", true,
                "tasks", taskMaps,
                "count", taskMaps.size(),
                "message", message,
                "instruction", "Para atualizar/mover tarefas, use o 'id' de uma destas tarefas como 'taskId'"
            );
        } catch (Exception e) {
            return createErrorResponse("Erro ao listar tarefas: " + e.getMessage());
        }
    }

    private Map<String, Object> executeUpdateTask(Map<String, Object> args, UserPrincipal user) {
        try {
            Long taskId = Long.valueOf(String.valueOf(args.get("taskId")));
            String title = (String) args.get("title");
            String description = (String) args.get("description");
            String status = (String) args.get("status");

            Authentication auth = createAuthentication(user);

            // Se apenas status foi fornecido, usar método específico
            if (status != null && title == null && description == null) {
                ResponseEntity<Task> response = taskController.updateTaskStatus(taskId, status, auth);
                if (response.getStatusCode().is2xxSuccessful()) {
                    return Map.of(
                        "success", true,
                        "message", "Status da tarefa " + taskId + " atualizado para " + status
                    );
                }
            } else {
                // Atualização geral da tarefa
                Task taskDetails = new Task();
                if (title != null) taskDetails.setTitle(title);
                if (description != null) taskDetails.setDescription(description);
                if (status != null) taskDetails.setStatus(status);

                ResponseEntity<Task> response = taskController.updateTask(taskId, taskDetails, auth);
                if (response.getStatusCode().is2xxSuccessful()) {
                    return Map.of(
                        "success", true,
                        "message", "Tarefa " + taskId + " atualizada com sucesso!"
                    );
                }
            }

            return createErrorResponse("Erro ao atualizar tarefa");
        } catch (Exception e) {
            return createErrorResponse("Erro ao atualizar tarefa: " + e.getMessage());
        }
    }

    private Map<String, Object> executeMoveTask(Map<String, Object> args, UserPrincipal user) {
        try {
            Long taskId = Long.valueOf(String.valueOf(args.get("taskId")));
            String status = (String) args.get("status");

            Authentication auth = createAuthentication(user);
            ResponseEntity<Task> response = taskController.updateTaskStatus(taskId, status, auth);

            if (response.getStatusCode().is2xxSuccessful()) {
                return Map.of(
                    "success", true,
                    "message", "Tarefa " + taskId + " movida para " + status + " com sucesso!"
                );
            } else {
                return createErrorResponse("Erro ao mover tarefa");
            }
        } catch (Exception e) {
            return createErrorResponse("Erro ao mover tarefa: " + e.getMessage());
        }
    }

    // Métodos auxiliares

    private Map<String, Object> createToolSchema(String name, String description, 
                                                Map<String, Object> properties, 
                                                List<String> required) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("name", name);
        schema.put("description", description);
        
        Map<String, Object> inputSchema = new HashMap<>();
        inputSchema.put("type", "object");
        inputSchema.put("properties", properties);
        inputSchema.put("required", required);
        
        schema.put("input_schema", inputSchema);
        return schema;
    }

    private Map<String, Object> createErrorResponse(String message) {
        return Map.of(
            "success", false,
            "error", message
        );
    }

    private Authentication createAuthentication(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return null;
        }
        return new UsernamePasswordAuthenticationToken(
            userPrincipal,
            null,
            userPrincipal.getAuthorities()
        );
    }

    // === MÉTODOS GITHUB MCP ===

    private Map<String, Object> executeGitHubListRepositories(Map<String, Object> args, UserPrincipal user) {
        try {
            Map<String, Object> result = gitHubMCPService.listRepositories(args);

            if ((Boolean) result.get("success")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> repos = (List<Map<String, Object>>) result.get("repositories");

                return Map.of(
                    "success", true,
                    "repositories", repos,
                    "count", repos.size(),
                    "message", "Repositórios listados com sucesso"
                );
            } else {
                return createErrorResponse("Erro ao listar repositórios: " + result.get("error"));
            }
        } catch (Exception e) {
            return createErrorResponse("Erro ao executar github_list_repositories: " + e.getMessage());
        }
    }

    private Map<String, Object> executeGitHubGetRepository(Map<String, Object> args, UserPrincipal user) {
        try {
            String owner = (String) args.get("owner");
            String repo = (String) args.get("repo");

            Map<String, Object> result = gitHubMCPService.getRepository(owner, repo);

            if ((Boolean) result.get("success")) {
                return Map.of(
                    "success", true,
                    "repository", result.get("repository"),
                    "message", "Repositório obtido com sucesso"
                );
            } else {
                return createErrorResponse("Erro ao obter repositório: " + result.get("error"));
            }
        } catch (Exception e) {
            return createErrorResponse("Erro ao executar github_get_repository: " + e.getMessage());
        }
    }

    private Map<String, Object> executeGitHubListIssues(Map<String, Object> args, UserPrincipal user) {
        try {
            String owner = (String) args.get("owner");
            String repo = (String) args.get("repo");

            Map<String, Object> result = gitHubMCPService.listIssues(owner, repo, args);

            if ((Boolean) result.get("success")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> issues = (List<Map<String, Object>>) result.get("issues");

                return Map.of(
                    "success", true,
                    "issues", issues,
                    "count", issues.size(),
                    "message", "Issues listadas com sucesso"
                );
            } else {
                return createErrorResponse("Erro ao listar issues: " + result.get("error"));
            }
        } catch (Exception e) {
            return createErrorResponse("Erro ao executar github_list_issues: " + e.getMessage());
        }
    }

    private Map<String, Object> executeGitHubCreateIssue(Map<String, Object> args, UserPrincipal user) {
        try {
            String owner = (String) args.get("owner");
            String repo = (String) args.get("repo");

            Map<String, Object> issueData = new HashMap<>();
            issueData.put("title", args.get("title"));
            if (args.containsKey("body")) issueData.put("body", args.get("body"));
            if (args.containsKey("labels")) issueData.put("labels", args.get("labels"));
            if (args.containsKey("assignees")) issueData.put("assignees", args.get("assignees"));

            Map<String, Object> result = gitHubMCPService.createIssue(owner, repo, issueData);

            if ((Boolean) result.get("success")) {
                return Map.of(
                    "success", true,
                    "issue", result.get("issue"),
                    "issueNumber", result.get("issueNumber"),
                    "message", "Issue criada com sucesso"
                );
            } else {
                return createErrorResponse("Erro ao criar issue: " + result.get("error"));
            }
        } catch (Exception e) {
            return createErrorResponse("Erro ao executar github_create_issue: " + e.getMessage());
        }
    }

    private Map<String, Object> executeGitHubListPullRequests(Map<String, Object> args, UserPrincipal user) {
        try {
            String owner = (String) args.get("owner");
            String repo = (String) args.get("repo");

            Map<String, Object> result = gitHubMCPService.listPullRequests(owner, repo, args);

            if ((Boolean) result.get("success")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> pullRequests = (List<Map<String, Object>>) result.get("pullRequests");

                return Map.of(
                    "success", true,
                    "pullRequests", pullRequests,
                    "count", pullRequests.size(),
                    "message", "Pull requests listados com sucesso"
                );
            } else {
                return createErrorResponse("Erro ao listar pull requests: " + result.get("error"));
            }
        } catch (Exception e) {
            return createErrorResponse("Erro ao executar github_list_pull_requests: " + e.getMessage());
        }
    }

    private Map<String, Object> executeGitHubGetFileContent(Map<String, Object> args, UserPrincipal user) {
        try {
            String owner = (String) args.get("owner");
            String repo = (String) args.get("repo");
            String path = (String) args.get("path");
            String ref = (String) args.get("ref");

            Map<String, Object> result = gitHubMCPService.getFileContent(owner, repo, path, ref);

            if ((Boolean) result.get("success")) {
                return Map.of(
                    "success", true,
                    "file", result.get("file"),
                    "message", "Conteúdo do arquivo obtido com sucesso"
                );
            } else {
                return createErrorResponse("Erro ao obter conteúdo do arquivo: " + result.get("error"));
            }
        } catch (Exception e) {
            return createErrorResponse("Erro ao executar github_get_file_content: " + e.getMessage());
        }
    }

    private Map<String, Object> executeGitHubListCommits(Map<String, Object> args, UserPrincipal user) {
        try {
            String owner = (String) args.get("owner");
            String repo = (String) args.get("repo");

            Map<String, Object> result = gitHubMCPService.listCommits(owner, repo, args);

            if ((Boolean) result.get("success")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> commits = (List<Map<String, Object>>) result.get("commits");

                return Map.of(
                    "success", true,
                    "commits", commits,
                    "count", commits.size(),
                    "message", "Commits listados com sucesso"
                );
            } else {
                return createErrorResponse("Erro ao listar commits: " + result.get("error"));
            }
        } catch (Exception e) {
            return createErrorResponse("Erro ao executar github_list_commits: " + e.getMessage());
        }
    }

    private Map<String, Object> executeGitHubSearchRepositories(Map<String, Object> args, UserPrincipal user) {
        try {
            String query = (String) args.get("query");

            Map<String, Object> result = gitHubMCPService.searchRepositories(query, args);

            if ((Boolean) result.get("success")) {
                return Map.of(
                    "success", true,
                    "searchResult", result.get("searchResult"),
                    "message", "Busca de repositórios realizada com sucesso"
                );
            } else {
                return createErrorResponse("Erro ao buscar repositórios: " + result.get("error"));
            }
        } catch (Exception e) {
            return createErrorResponse("Erro ao executar github_search_repositories: " + e.getMessage());
        }
    }
}
