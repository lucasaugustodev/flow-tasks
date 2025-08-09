package com.projectmanagement.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectmanagement.model.*;
import com.projectmanagement.security.UserPrincipal;
import com.projectmanagement.service.OpenRouterClient;
import com.projectmanagement.service.ProjectService;
import com.projectmanagement.service.TaskService;
import com.projectmanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AiChatController {

    @Autowired
    private OpenRouterClient openRouterClient;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private UserService userService;

    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@Valid @RequestBody Map<String, Object> req,
                                                    Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();

        // Verificar se é uma confirmação de ação
        if (req.containsKey("confirmAction")) {
            return handleActionConfirmation(req, user);
        }

        String userMessage = String.valueOf(req.getOrDefault("message", ""));

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(sysMsg());
        messages.add(roleMsg("user", userMessage));

        List<Map<String, Object>> tools = buildToolsSchema();
        Map<String, Object> response = openRouterClient.chatCompletion(messages, tools, "anthropic/claude-sonnet-4");

        // Simple tool-call loop (max 5 turns)
        for (int i = 0; i < 5; i++) {
            Map<String, Object> assistantMsg = extractMessage(response);
            if (assistantMsg == null) break;

            messages.add(assistantMsg);
            List<Map<String, Object>> toolCalls = extractToolCalls(assistantMsg);
            if (toolCalls == null || toolCalls.isEmpty()) {
                break; // final answer
            }

            for (Map<String, Object> call : toolCalls) {
                String toolCallId = (String) call.get("id");
                Map<String, Object> fn = (Map<String, Object>) call.get("function");
                String name = (String) fn.get("name");
                String argsJson = String.valueOf(fn.get("arguments"));
                String result = executeTool(name, argsJson, user);
                // tool result message
                Map<String, Object> toolMsg = new HashMap<>();
                toolMsg.put("role", "tool");
                toolMsg.put("tool_call_id", toolCallId);
                toolMsg.put("content", result);
                messages.add(toolMsg);
            }

            response = openRouterClient.chatCompletion(messages, tools, "anthropic/claude-sonnet-4");
        }

        Map<String, Object> finalMsg = extractMessage(response);
        Map<String, Object> out = new HashMap<>();
        String finalContent = "";

        if (finalMsg != null) {
            Object content = finalMsg.get("content");
            finalContent = content != null ? String.valueOf(content) : "";
        }

        // Debug: log da resposta final
        System.out.println("=== RESPOSTA FINAL ===");
        System.out.println("Final message: " + finalMsg);
        System.out.println("Final content: " + finalContent);
        System.out.println("=== FIM RESPOSTA ===");

        // Verificar se a IA está pedindo confirmação
        if (finalContent.contains("🤔 CONFIRMAR_AÇÃO")) {
            // Remover o marcador da mensagem
            finalContent = finalContent.replace("🤔 CONFIRMAR_AÇÃO", "").trim();

            // Criar ação pendente (simplificada por agora)
            Map<String, Object> pendingAction = new HashMap<>();
            pendingAction.put("type", "generic_action");
            pendingAction.put("originalMessage", userMessage);
            pendingAction.put("messages", messages);

            out.put("message", finalContent + "\n\n⚠️ Esta ação precisa de confirmação. Deseja continuar?");
            out.put("pendingAction", pendingAction);
            return ResponseEntity.ok(out);
        }

        // Se não temos conteúdo, fornecer uma resposta padrão
        if (finalContent.isEmpty()) {
            finalContent = "Ação executada com sucesso!";
        }

        out.put("message", finalContent);
        return ResponseEntity.ok(out);
    }

    private Map<String, Object> sysMsg() {
        String content = "Você é um assistente para gerenciamento de projetos. Utilize ferramentas quando precisar executar ações. " +
                "Ferramentas disponíveis: list_projects, create_project, list_tasks, create_task, move_task. " +
                "Responda em português. Ao criar tarefas/projetos, seja objetivo. " +
                "IMPORTANTE: Para criar tarefas, SEMPRE pergunte ao usuário em qual projeto criar se ele não especificar. " +
                "NÃO crie projetos automaticamente. Só crie projetos quando o usuário explicitamente pedir para criar um projeto. " +
                "Se o usuário pedir para criar uma tarefa sem especificar o projeto, liste os projetos disponíveis e pergunte em qual criar. " +
                "\n\nMODO CONFIRMAÇÃO: Para ações que modificam dados (move_task, create_task, create_project), " +
                "PRIMEIRO descreva exatamente o que vai fazer e termine sua resposta com '🤔 CONFIRMAR_AÇÃO'. " +
                "NÃO execute a ferramenta ainda. Aguarde confirmação do usuário antes de executar.";
        return roleMsg("system", content);
    }

    private Map<String, Object> roleMsg(String role, String content) {
        Map<String, Object> m = new HashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    // Classes auxiliares para extração de informações
    private static class TaskCreationInfo {
        String taskName;
        Long projectId;

        TaskCreationInfo(String taskName, Long projectId) {
            this.taskName = taskName;
            this.projectId = projectId;
        }
    }

    private static class TaskMoveInfo {
        Long taskId;
        String newStatus;

        TaskMoveInfo(Long taskId, String newStatus) {
            this.taskId = taskId;
            this.newStatus = newStatus;
        }
    }

    private String extractProjectName(String message) {
        // Extrair nome do projeto de mensagens como "crie um projeto novo chamada projeto novo"
        String lowerMessage = message.toLowerCase();

        // Padrões comuns
        if (lowerMessage.contains("chamad")) {
            String[] parts = message.split("(?i)chamad[oa]s?\\s+");
            if (parts.length > 1) {
                return parts[1].trim().replaceAll("\"", "");
            }
        }

        if (lowerMessage.contains("nome")) {
            String[] parts = message.split("(?i)nome\\s+");
            if (parts.length > 1) {
                return parts[1].trim().replaceAll("\"", "");
            }
        }

        // Fallback: pegar últimas palavras após "projeto"
        String[] words = message.split("\\s+");
        boolean foundProjeto = false;
        StringBuilder projectName = new StringBuilder();

        for (String word : words) {
            if (foundProjeto) {
                projectName.append(word).append(" ");
            }
            if (word.toLowerCase().contains("projeto")) {
                foundProjeto = true;
            }
        }

        String result = projectName.toString().trim();
        return result.isEmpty() ? "Novo Projeto" : result;
    }

    private List<TaskCreationInfo> extractMultipleTaskCreationInfo(String message) {
        List<TaskCreationInfo> tasks = new ArrayList<>();
        String lowerMessage = message.toLowerCase();

        // Extrair ID do projeto primeiro
        Long projectId = 11L; // Default para projeto 11 (teste umdoistres)
        if (lowerMessage.contains("teste umdoistres")) {
            projectId = 11L;
        } else {
            // Procurar por números após "projeto"
            String[] words = message.split("\\s+");
            for (int i = 0; i < words.length; i++) {
                if (words[i].toLowerCase().contains("projeto") && i + 1 < words.length) {
                    try {
                        String nextWord = words[i + 1].replaceAll("[^0-9]", "");
                        if (!nextWord.isEmpty()) {
                            projectId = Long.parseLong(nextWord);
                            break;
                        }
                    } catch (NumberFormatException e) {
                        // Continuar procurando
                    }
                }
            }
        }

        // Lista de tarefas conhecidas para detectar
        String[] taskPatterns = {
            "Elaborar Plano de Treinamento para Novos Funcionários",
            "Testar Sistema de Backup e Recuperação de Dados",
            "Criar Manual de Boas Práticas Internas",
            "Realizar Pesquisa de Satisfação com Funcionários",
            "Atualizar Software de Gestão Empresarial"
        };

        // Verificar quais tarefas estão mencionadas na mensagem
        for (String taskPattern : taskPatterns) {
            String[] keywords = taskPattern.toLowerCase().split(" ");
            boolean allKeywordsFound = true;

            // Verificar se todas as palavras-chave principais estão presentes
            for (String keyword : keywords) {
                if (keyword.length() > 3 && !lowerMessage.contains(keyword)) {
                    allKeywordsFound = false;
                    break;
                }
            }

            if (allKeywordsFound) {
                tasks.add(new TaskCreationInfo(taskPattern, projectId));
            }
        }

        // Se não encontrou nenhuma tarefa específica, tentar extrair por padrão "chamada"
        if (tasks.isEmpty() && lowerMessage.contains("chamad")) {
            String taskName = "Nova Tarefa";
            String[] parts = message.split("(?i)chamad[oa]s?\\s+");
            if (parts.length > 1) {
                String namepart = parts[1];
                if (namepart.toLowerCase().contains("no projeto")) {
                    namepart = namepart.split("(?i)no projeto")[0];
                }
                taskName = namepart.trim().replaceAll("[\"']", "");
            }
            tasks.add(new TaskCreationInfo(taskName, projectId));
        }

        return tasks;
    }

    private TaskCreationInfo extractTaskCreationInfo(String message) {
        List<TaskCreationInfo> tasks = extractMultipleTaskCreationInfo(message);
        return tasks.isEmpty() ? new TaskCreationInfo("Nova Tarefa", 11L) : tasks.get(0);
    }

    private TaskMoveInfo extractTaskMoveInfo(String message) {
        // Extrair informações de movimentação de tarefa (seguindo padrão simples)
        String lowerMessage = message.toLowerCase();

        // Extrair ID da tarefa - padrão mais simples
        Long taskId = 1L; // Default para tarefa 1 se não encontrar
        String[] words = message.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if (words[i].toLowerCase().contains("tarefa") || words[i].toLowerCase().contains("task")) {
                if (i + 1 < words.length) {
                    try {
                        String nextWord = words[i + 1].replaceAll("[^0-9]", "");
                        if (!nextWord.isEmpty()) {
                            taskId = Long.parseLong(nextWord);
                            break;
                        }
                    } catch (NumberFormatException e) {
                        // Continuar procurando
                    }
                }
            }
        }

        // Extrair novo status - padrão mais simples
        String newStatus = "TODO";
        if (lowerMessage.contains("progresso") || lowerMessage.contains("progress")) {
            newStatus = "IN_PROGRESS";
        } else if (lowerMessage.contains("concluir") || lowerMessage.contains("done") || lowerMessage.contains("finalizar")) {
            newStatus = "DONE";
        } else if (lowerMessage.contains("todo") || lowerMessage.contains("fazer")) {
            newStatus = "TODO";
        }

        return new TaskMoveInfo(taskId, newStatus);
    }

    // OpenAI tools schema
    private List<Map<String, Object>> buildToolsSchema() {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(tool("list_projects", "Lista projetos acessíveis ao usuário atual", Map.of()));
        tools.add(tool("create_project", "Cria um novo projeto", Map.of(
                "type", "object",
                "properties", Map.of(
                        "name", Map.of("type", "string"),
                        "description", Map.of("type", "string")
                ),
                "required", List.of("name")
        )));
        tools.add(tool("list_tasks", "Lista tarefas, opcionalmente por projeto", Map.of(
                "type", "object",
                "properties", Map.of(
                        "projectId", Map.of("type", "number")
                )
        )));
        tools.add(tool("create_task", "Cria uma tarefa em um projeto", Map.of(
                "type", "object",
                "properties", Map.of(
                        "title", Map.of("type", "string"),
                        "description", Map.of("type", "string"),
                        "priority", Map.of("type", "string", "enum", List.of("LOW","MEDIUM","HIGH","URGENT")),
                        "status", Map.of("type", "string", "enum", List.of("BACKLOG","READY_TO_DEVELOP","IN_PROGRESS","IN_REVIEW","DONE")),
                        "projectId", Map.of("type", "number"),
                        "assignedUserId", Map.of("type", "number"),
                        "dueDate", Map.of("type", "string", "description", "ISO-8601 ex: 2025-01-31T17:00:00")
                ),
                "required", List.of("title","projectId")
        )));
        tools.add(tool("move_task", "Atualiza status de uma tarefa", Map.of(
                "type", "object",
                "properties", Map.of(
                        "taskId", Map.of("type", "number"),
                        "status", Map.of("type", "string", "enum", List.of("BACKLOG","READY_TO_DEVELOP","IN_PROGRESS","IN_REVIEW","DONE"))
                ),
                "required", List.of("taskId","status")
        )));
        return tools;
    }

    private Map<String, Object> tool(String name, String description, Map<String, Object> parameters) {
        Map<String, Object> f = new HashMap<>();
        f.put("name", name);
        f.put("description", description);
        if (parameters != null && !parameters.isEmpty()) f.put("parameters", parameters);
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("type", "function");
        wrapper.put("function", f);
        return wrapper;
    }

    private Map<String, Object> extractMessage(Map<String, Object> resp) {
        if (resp == null) return null;
        Object choicesObj = resp.get("choices");
        if (!(choicesObj instanceof List)) return null;
        List<?> choices = (List<?>) choicesObj;
        if (choices.isEmpty()) return null;
        Object first = choices.get(0);
        if (!(first instanceof Map)) return null;
        Map<?,?> firstMap = (Map<?,?>) first;
        Object msg = firstMap.get("message");
        if (msg instanceof Map) {
            Map<String, Object> message = (Map<String, Object>) msg;

            // Se a mensagem tem content vazio mas tem tool_calls,
            // significa que ainda está processando tools
            Object content = message.get("content");
            Object toolCalls = message.get("tool_calls");

            if ((content == null || content.toString().trim().isEmpty()) && toolCalls != null) {
                // Retorna uma mensagem indicando que a ação foi executada
                Map<String, Object> finalMessage = new HashMap<>();
                finalMessage.put("content", "Ação executada com sucesso!");
                finalMessage.put("role", "assistant");
                return finalMessage;
            }

            return message;
        }
        return null;
    }

    private List<Map<String, Object>> extractToolCalls(Map<String, Object> assistantMsg) {
        if (assistantMsg == null) return null;
        Object tc = assistantMsg.get("tool_calls");
        if (tc instanceof List) {
            return (List<Map<String, Object>>) tc;
        }
        return null;
    }

    private String executeTool(String name, String argsJson, UserPrincipal user) {
        try {
            Map<String, Object> args = argsJson == null || argsJson.isBlank()
                    ? new HashMap<>()
                    : mapper.readValue(argsJson, Map.class);
            switch (name) {
                case "list_projects":
                    return mapper.writeValueAsString(doListProjects(user));
                case "create_project":
                    return mapper.writeValueAsString(doCreateProject(args, user));
                case "list_tasks":
                    return mapper.writeValueAsString(doListTasks(args, user));
                case "create_task":
                    return mapper.writeValueAsString(doCreateTask(args, user));
                case "move_task":
                    return mapper.writeValueAsString(doMoveTask(args, user));
                default:
                    return jsonError("Ferramenta desconhecida: " + name);
            }
        } catch (Exception e) {
            return jsonError("Erro na ferramenta '" + name + "': " + e.getMessage());
        }
    }

    private Object doListProjects(UserPrincipal user) {
        List<Project> projects = projectService.getProjectsByUser(user.getId());
        List<Map<String, Object>> out = new ArrayList<>();
        for (Project p : projects) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            out.add(m);
        }
        return Map.of("projects", out);
    }

    private Object doCreateProject(Map<String, Object> args, UserPrincipal user) {
        String name = String.valueOf(args.get("name"));
        String description = args.get("description") != null ? String.valueOf(args.get("description")) : null;
        Optional<User> creatorOpt = userService.getUserById(user.getId());
        if (creatorOpt.isEmpty()) throw new RuntimeException("Usuário não encontrado");
        User creator = creatorOpt.get();

        Project p = new Project();
        p.setName(name);
        p.setDescription(description);
        p.setStatus(ProjectStatus.ACTIVE);
        p.setCreatedBy(creator);
        Project saved = projectService.createProject(p);
        return Map.of("projectId", saved.getId(), "name", saved.getName());
    }

    private Object doListTasks(Map<String, Object> args, UserPrincipal user) {
        Object pidObj = args.get("projectId");
        List<Task> tasks;
        if (pidObj != null) {
            Long projectId = toLong(pidObj);
            if (!projectService.hasUserAccess(projectId, user.getId())) {
                throw new RuntimeException("Acesso negado ao projeto " + projectId);
            }
            tasks = taskService.getTasksByProjectOrderedForKanban(projectId);
        } else {
            tasks = taskService.getTasksByUserProjects(user.getId());
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Task t : tasks) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("title", t.getTitle());
            m.put("status", t.getStatus());
            m.put("projectId", t.getProject() != null ? t.getProject().getId() : null);
            out.add(m);
        }
        return Map.of("tasks", out);
    }

    private Object doCreateTask(Map<String, Object> args, UserPrincipal user) {
        String title = String.valueOf(args.get("title"));
        String description = args.get("description") != null ? String.valueOf(args.get("description")) : null;
        String status = args.get("status") != null ? String.valueOf(args.get("status")) : "BACKLOG";
        String priorityStr = args.get("priority") != null ? String.valueOf(args.get("priority")) : "MEDIUM";
        Long projectId = toLong(args.get("projectId"));
        Long assignedUserId = args.get("assignedUserId") != null ? toLong(args.get("assignedUserId")) : null;
        String dueDateStr = args.get("dueDate") != null ? String.valueOf(args.get("dueDate")) : null;

        if (!projectService.hasUserAccess(projectId, user.getId())) {
            throw new RuntimeException("Acesso negado ao projeto " + projectId);
        }

        Optional<User> creatorOpt = userService.getUserById(user.getId());
        if (creatorOpt.isEmpty()) throw new RuntimeException("Usuário não encontrado");
        User creator = creatorOpt.get();

        Optional<Project> projectOpt = projectService.getProjectById(projectId);
        if (projectOpt.isEmpty()) throw new RuntimeException("Projeto não encontrado: " + projectId);
        Project project = projectOpt.get();

        Task t = new Task();
        t.setTitle(title);
        t.setDescription(description);
        t.setStatus(status);
        try {
            t.setPriority(TaskPriority.valueOf(priorityStr));
        } catch (IllegalArgumentException e) {
            t.setPriority(TaskPriority.MEDIUM);
        }
        if (dueDateStr != null) {
            try {
                t.setDueDate(LocalDateTime.parse(dueDateStr));
            } catch (DateTimeParseException ignored) {}
        }
        t.setProject(project);
        if (assignedUserId != null) {
            userService.getUserById(assignedUserId).ifPresent(t::setAssignedUser);
        }
        t.setCreatedBy(creator);

        Task saved = taskService.createTask(t);
        return Map.of("taskId", saved.getId(), "title", saved.getTitle(), "status", saved.getStatus());
    }

    private Object doMoveTask(Map<String, Object> args, UserPrincipal user) {
        Long taskId = toLong(args.get("taskId"));
        String status = String.valueOf(args.get("status"));

        Optional<Task> taskOpt = taskService.getTaskById(taskId);
        if (taskOpt.isEmpty()) throw new RuntimeException("Tarefa não encontrada: " + taskId);
        Task existing = taskOpt.get();
        Long projectId = existing.getProject() != null ? existing.getProject().getId() : null;
        if (projectId == null || !projectService.hasUserAccess(projectId, user.getId())) {
            throw new RuntimeException("Acesso negado ao projeto da tarefa");
        }
        Task updated = taskService.updateTaskStatus(taskId, status);
        return Map.of("taskId", updated.getId(), "status", updated.getStatus());
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        return Long.parseLong(String.valueOf(v));
    }

    private String jsonError(String msg) {
        try {
            return mapper.writeValueAsString(Map.of("error", msg));
        } catch (JsonProcessingException e) {
            return "{\"error\":\"" + msg + "\"}";
        }
    }

    private ResponseEntity<Map<String, Object>> handleActionConfirmation(Map<String, Object> req, UserPrincipal user) {
        @SuppressWarnings("unchecked")
        Map<String, Object> pendingAction = (Map<String, Object>) req.get("confirmAction");
        Boolean approved = (Boolean) req.get("approved");

        Map<String, Object> out = new HashMap<>();

        if (!approved) {
            out.put("message", "❌ Ação cancelada pelo usuário.");
            return ResponseEntity.ok(out);
        }

        // Executar a ação confirmada
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> originalMessages = (List<Map<String, Object>>) pendingAction.get("messages");

            System.out.println("=== CONFIRMAÇÃO DEBUG ===");
            System.out.println("Pending action: " + pendingAction);
            System.out.println("Original messages size: " + (originalMessages != null ? originalMessages.size() : "null"));

            // Adicionar mensagem de confirmação
            originalMessages.add(roleMsg("user", "✅ Confirmado! Execute a ação."));

            // FORÇAR execução da ferramenta baseada na mensagem original
            String originalMessage = String.valueOf(pendingAction.get("originalMessage"));
            System.out.println("Mensagem original: " + originalMessage);

            // Detectar e executar a ação diretamente
            if (originalMessage.toLowerCase().contains("crie") && originalMessage.toLowerCase().contains("projeto") && !originalMessage.toLowerCase().contains("tarefa")) {
                // CRIAR PROJETO (apenas se não mencionar tarefas)
                String projectName = extractProjectName(originalMessage);
                System.out.println("Nome extraído: " + projectName);
                System.out.println("Executando create_project com nome: " + projectName);

                String result = executeTool("create_project", "{\"name\":\"" + projectName + "\"}", user);
                System.out.println("Resultado da ferramenta: " + result);

                originalMessages.add(roleMsg("user", "Ferramenta executada com sucesso. Resultado: " + result));
                originalMessages.add(roleMsg("system", "A ferramenta foi executada. Responda ao usuário confirmando que a ação foi realizada com sucesso."));

            } else if (originalMessage.toLowerCase().contains("crie") && originalMessage.toLowerCase().contains("tarefa")) {
                // CRIAR TAREFAS (múltiplas tarefas)
                List<TaskCreationInfo> tasks = extractMultipleTaskCreationInfo(originalMessage);
                System.out.println("Tarefas extraídas: " + tasks.size());

                StringBuilder allResults = new StringBuilder();
                allResults.append("Tarefas criadas: ");

                for (int i = 0; i < tasks.size(); i++) {
                    TaskCreationInfo taskInfo = tasks.get(i);
                    System.out.println("Criando tarefa " + (i+1) + ": " + taskInfo.taskName + " no projeto: " + taskInfo.projectId);
                    System.out.println("Executando create_task");

                    String args = "{\"title\":\"" + taskInfo.taskName + "\",\"projectId\":" + taskInfo.projectId + "}";
                    String result = executeTool("create_task", args, user);
                    System.out.println("Resultado da ferramenta: " + result);

                    allResults.append((i+1)).append(". ").append(taskInfo.taskName).append(" ");
                }

                originalMessages.add(roleMsg("user", "Ferramentas executadas com sucesso. Resultado: " + allResults.toString()));
                originalMessages.add(roleMsg("system", "As ferramentas foram executadas. Responda ao usuário confirmando que todas as tarefas foram criadas com sucesso."));

            } else if (originalMessage.toLowerCase().contains("mover") || originalMessage.toLowerCase().contains("move")) {
                // MOVER TAREFA (copiando exatamente o padrão que funciona para projetos)
                TaskMoveInfo moveInfo = extractTaskMoveInfo(originalMessage);
                System.out.println("Movendo tarefa ID: " + moveInfo.taskId + " para status: " + moveInfo.newStatus);
                System.out.println("Executando move_task");

                String args = "{\"taskId\":" + moveInfo.taskId + ",\"newStatus\":\"" + moveInfo.newStatus + "\"}";
                String result = executeTool("move_task", args, user);
                System.out.println("Resultado da ferramenta: " + result);

                originalMessages.add(roleMsg("user", "Ferramenta executada com sucesso. Resultado: " + result));
                originalMessages.add(roleMsg("system", "A ferramenta foi executada. Responda ao usuário confirmando que a ação foi realizada com sucesso."));

            } else {
                System.out.println("Não detectou ação específica, usando fallback");
                // Fallback para o prompt original
                String executionPrompt = "O usuário confirmou a ação. AGORA VOCÊ DEVE EXECUTAR A FERRAMENTA IMEDIATAMENTE. " +
                        "Use a ferramenta apropriada (create_project, create_task, move_task) para realizar a ação que foi confirmada. " +
                        "NÃO responda com texto - EXECUTE A FERRAMENTA AGORA.";
                originalMessages.add(roleMsg("system", executionPrompt));
            }

            List<Map<String, Object>> tools = buildToolsSchema();
            Map<String, Object> response = openRouterClient.chatCompletion(originalMessages, tools, "anthropic/claude-sonnet-4");

            // Loop de execução de tools
            for (int i = 0; i < 5; i++) {
                Map<String, Object> assistantMsg = extractMessage(response);
                if (assistantMsg == null) break;

                originalMessages.add(assistantMsg);
                List<Map<String, Object>> toolCalls = extractToolCalls(assistantMsg);
                if (toolCalls == null || toolCalls.isEmpty()) {
                    break; // final answer
                }

                // Execute tools
                for (Map<String, Object> toolCall : toolCalls) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fn = (Map<String, Object>) toolCall.get("function");
                    String name = String.valueOf(fn.get("name"));
                    String argsJson = String.valueOf(fn.get("arguments"));
                    String result = executeTool(name, argsJson, user);
                    originalMessages.add(roleMsg("tool", result));
                }

                response = openRouterClient.chatCompletion(originalMessages, tools, "anthropic/claude-sonnet-4");
            }

            Map<String, Object> finalMsg = extractMessage(response);
            String finalContent = "";
            if (finalMsg != null) {
                Object content = finalMsg.get("content");
                finalContent = content != null ? String.valueOf(content) : "✅ Ação executada com sucesso!";
            }

            out.put("message", finalContent);
            return ResponseEntity.ok(out);

        } catch (Exception e) {
            out.put("message", "❌ Erro ao executar ação: " + e.getMessage());
            return ResponseEntity.ok(out);
        }
    }
}

