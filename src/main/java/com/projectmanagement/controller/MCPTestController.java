package com.projectmanagement.controller;

import com.projectmanagement.service.MCPClient;
import com.projectmanagement.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller para testar ferramentas MCP diretamente sem depender da API Anthropic
 * Útil para testes e demonstrações
 */
@RestController
@RequestMapping("/api/mcp-test")
public class MCPTestController {

    @Autowired
    private MCPClient mcpClient;

    /**
     * Endpoint para testar criação de tarefa
     * POST /api/mcp-test/create-task
     */
    @PostMapping("/create-task")
    public ResponseEntity<Map<String, Object>> testCreateTask(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        try {
            String title = (String) request.getOrDefault("title", "Tarefa de Teste");
            Long projectId = Long.valueOf(String.valueOf(request.getOrDefault("projectId", 1)));
            String description = (String) request.getOrDefault("description", "Descrição de teste");

            Map<String, Object> arguments = new HashMap<>();
            arguments.put("title", title);
            arguments.put("projectId", projectId);
            arguments.put("description", description);

            Map<String, Object> result = mcpClient.executeTool("create_task", arguments, user);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Erro ao criar tarefa: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Endpoint para testar criação de projeto
     * POST /api/mcp-test/create-project
     */
    @PostMapping("/create-project")
    public ResponseEntity<Map<String, Object>> testCreateProject(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        try {
            String name = (String) request.getOrDefault("name", "Projeto de Teste");
            String description = (String) request.getOrDefault("description", "Descrição de teste");

            Map<String, Object> arguments = new HashMap<>();
            arguments.put("name", name);
            arguments.put("description", description);

            Map<String, Object> result = mcpClient.executeTool("create_project", arguments, user);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Erro ao criar projeto: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Endpoint para testar listagem de projetos
     * GET /api/mcp-test/list-projects
     */
    @GetMapping("/list-projects")
    public ResponseEntity<Map<String, Object>> testListProjects(
            @AuthenticationPrincipal UserPrincipal user) {
        
        try {
            Map<String, Object> result = mcpClient.executeTool("list_projects", new HashMap<>(), user);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Erro ao listar projetos: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Endpoint para testar listagem de tarefas
     * GET /api/mcp-test/list-tasks?projectId=1
     */
    @GetMapping("/list-tasks")
    public ResponseEntity<Map<String, Object>> testListTasks(
            @RequestParam(required = false) Long projectId,
            @AuthenticationPrincipal UserPrincipal user) {
        
        try {
            Map<String, Object> arguments = new HashMap<>();
            if (projectId != null) {
                arguments.put("projectId", projectId);
            }

            Map<String, Object> result = mcpClient.executeTool("list_tasks", arguments, user);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Erro ao listar tarefas: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Endpoint para testar atualização de tarefa
     * PUT /api/mcp-test/update-task
     */
    @PutMapping("/update-task")
    public ResponseEntity<Map<String, Object>> testUpdateTask(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        try {
            Long taskId = Long.valueOf(String.valueOf(request.get("taskId")));
            String title = (String) request.get("title");
            String description = (String) request.get("description");
            String status = (String) request.get("status");

            Map<String, Object> arguments = new HashMap<>();
            arguments.put("taskId", taskId);
            if (title != null) arguments.put("title", title);
            if (description != null) arguments.put("description", description);
            if (status != null) arguments.put("status", status);

            Map<String, Object> result = mcpClient.executeTool("update_task", arguments, user);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Erro ao atualizar tarefa: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Endpoint para testar movimentação de tarefa
     * PUT /api/mcp-test/move-task
     */
    @PutMapping("/move-task")
    public ResponseEntity<Map<String, Object>> testMoveTask(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        try {
            Long taskId = Long.valueOf(String.valueOf(request.get("taskId")));
            String status = (String) request.get("status");

            Map<String, Object> arguments = new HashMap<>();
            arguments.put("taskId", taskId);
            arguments.put("status", status);

            Map<String, Object> result = mcpClient.executeTool("move_task", arguments, user);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Erro ao mover tarefa: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Endpoint para listar todas as ferramentas MCP disponíveis
     * GET /api/mcp-test/tools
     */
    @GetMapping("/tools")
    public ResponseEntity<Map<String, Object>> listTools() {
        try {
            List<Map<String, Object>> tools = mcpClient.getAvailableTools();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tools", tools);
            response.put("count", tools.size());
            response.put("message", "Ferramentas MCP disponíveis para teste direto");
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Erro ao listar ferramentas: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Endpoint para executar qualquer ferramenta MCP
     * POST /api/mcp-test/execute
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeAnyTool(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        try {
            String toolName = (String) request.get("tool");
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = (Map<String, Object>) request.getOrDefault("arguments", new HashMap<>());

            if (toolName == null || toolName.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Nome da ferramenta é obrigatório");
                return ResponseEntity.badRequest().body(error);
            }

            Map<String, Object> result = mcpClient.executeTool(toolName, arguments, user);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Erro ao executar ferramenta: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Endpoint de demonstração que cria um projeto e algumas tarefas
     * POST /api/mcp-test/demo
     */
    @PostMapping("/demo")
    public ResponseEntity<Map<String, Object>> runDemo(
            @AuthenticationPrincipal UserPrincipal user) {
        
        try {
            List<Map<String, Object>> results = new ArrayList<>();
            
            // 1. Criar projeto
            Map<String, Object> projectArgs = new HashMap<>();
            projectArgs.put("name", "Projeto Demo MCP");
            projectArgs.put("description", "Projeto criado para demonstração do MCP");
            
            Map<String, Object> projectResult = mcpClient.executeTool("create_project", projectArgs, user);
            results.add(Map.of("step", "create_project", "result", projectResult));
            
            if (projectResult.get("success").equals(true)) {
                Long projectId = Long.valueOf(String.valueOf(projectResult.get("projectId")));
                
                // 2. Criar tarefas
                String[] taskTitles = {
                    "Configurar ambiente de desenvolvimento",
                    "Implementar funcionalidade principal", 
                    "Escrever testes unitários",
                    "Documentar API"
                };
                
                for (String title : taskTitles) {
                    Map<String, Object> taskArgs = new HashMap<>();
                    taskArgs.put("title", title);
                    taskArgs.put("projectId", projectId);
                    taskArgs.put("description", "Tarefa criada na demonstração MCP");
                    
                    Map<String, Object> taskResult = mcpClient.executeTool("create_task", taskArgs, user);
                    results.add(Map.of("step", "create_task", "task", title, "result", taskResult));
                }
                
                // 3. Listar tarefas criadas
                Map<String, Object> listArgs = new HashMap<>();
                listArgs.put("projectId", projectId);
                Map<String, Object> listResult = mcpClient.executeTool("list_tasks", listArgs, user);
                results.add(Map.of("step", "list_tasks", "result", listResult));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Demonstração MCP executada com sucesso!");
            response.put("steps", results);
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Erro na demonstração: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
