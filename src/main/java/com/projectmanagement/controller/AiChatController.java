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
        String userMessage = String.valueOf(req.getOrDefault("message", ""));

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(sysMsg());
        messages.add(roleMsg("user", userMessage));

        List<Map<String, Object>> tools = buildToolsSchema();
        Map<String, Object> response = openRouterClient.chatCompletion(messages, tools, "anthropic/claude-3.5-sonnet");

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

            response = openRouterClient.chatCompletion(messages, tools, "anthropic/claude-3.5-sonnet");
        }

        Map<String, Object> finalMsg = extractMessage(response);
        Map<String, Object> out = new HashMap<>();
        out.put("message", finalMsg != null ? String.valueOf(finalMsg.get("content")) : "");
        return ResponseEntity.ok(out);
    }

    private Map<String, Object> sysMsg() {
        String content = "Você é um assistente para gerenciamento de projetos. Utilize ferramentas quando precisar executar ações. " +
                "Ferramentas disponíveis: list_projects, create_project, list_tasks, create_task, move_task. " +
                "Responda em português. Ao criar tarefas/projetos, seja objetivo.";
        return roleMsg("system", content);
    }

    private Map<String, Object> roleMsg(String role, String content) {
        Map<String, Object> m = new HashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
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
            return (Map<String, Object>) msg;
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
}

