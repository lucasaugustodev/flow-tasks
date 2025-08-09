package com.projectmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectmanagement.security.UserPrincipal;
import com.projectmanagement.service.AnthropicClient;
import com.projectmanagement.service.MCPClient;
import com.projectmanagement.service.ZepMemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    // Endpoint de teste removido - não mais necessário com MCP

    @Autowired
    private AnthropicClient anthropicClient;

    @Autowired
    private MCPClient mcpClient;

    @Autowired
    private ZepMemoryService zepMemoryService;

    // Contexto de chat por sessão para lembrar de tarefas e projetos recentes
    private final Map<String, ChatContext> sessionContexts = new ConcurrentHashMap<>();

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(
            @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal UserPrincipal user,
            HttpSession session) {

        String message = String.valueOf(req.get("message"));

        System.out.println("=== CHAT MCP ===");
        System.out.println("Mensagem recebida: " + message);

        // Processar com MCP - estratégia única
        return processWithMCP(message, user, session);
    }

    // Método processNewMessage removido - substituído por processWithMCP

    /**
     * Método único para processamento com MCP
     * Sempre usa ferramentas MCP - sem detecção manual
     */
    private ResponseEntity<Map<String, Object>> processWithMCP(String message, UserPrincipal user, HttpSession session) {
        try {
            System.out.println("=== PROCESSANDO COM MCP ===");
            System.out.println("Mensagem: " + message);

            // Obter ou criar contexto da sessão
            String sessionId = session.getId();
            String userId = user.getId().toString();

            // Inicializar sessão Zep se necessário
            zepMemoryService.createOrUpdateSession(sessionId, userId);

            // Adicionar mensagem do usuário ao Zep
            zepMemoryService.addMessage(sessionId, "user", message, null);

            ChatContext context = sessionContexts.computeIfAbsent(sessionId, k -> new ChatContext());

            // Limpar contextos expirados
            sessionContexts.entrySet().removeIf(entry -> entry.getValue().isExpired());

            List<Map<String, Object>> messages = new ArrayList<>();

            // Obter contexto relevante do Zep Memory
            String zepContext = zepMemoryService.getRelevantContext(sessionId, message);
            String sessionSummary = zepMemoryService.getSessionSummary(sessionId);

            // System prompt para MCP - agora com contexto do Zep
            String contextInfo = context.getContextSummary();
            String systemPrompt = "Você é um assistente especializado em gerenciamento de projetos. " +
                "REGRAS IMPORTANTES:\n" +
                "1. SEMPRE liste os projetos existentes ANTES de criar tarefas\n" +
                "2. NUNCA crie novos projetos a menos que explicitamente solicitado\n" +
                "3. Use SEMPRE projetos existentes quando possível\n" +
                "4. Quando o usuário mencionar 'projeto X', procure por esse projeto específico\n" +
                "5. Se o usuário pedir ação em UMA tarefa específica, foque APENAS nela\n" +
                "6. Execute APENAS o que foi solicitado - não faça ações extras\n" +
                "7. Responda em português de forma clara e objetiva\n" +
                "8. Use o CONTEXTO ATUAL para entender referências como 'essa tarefa', 'este projeto', etc.\n\n";

            // Adicionar contexto do Zep Memory
            if (!zepContext.isEmpty()) {
                systemPrompt += "CONTEXTO RELEVANTE (Zep Memory):\n" + zepContext + "\n";
            }

            if (!sessionSummary.isEmpty()) {
                systemPrompt += "RESUMO DA SESSÃO:\n" + sessionSummary + "\n";
            }

            if (!contextInfo.isEmpty()) {
                systemPrompt += "CONTEXTO ATUAL DA CONVERSA:\n" + contextInfo + "\n";
            }

            systemPrompt += "FERRAMENTAS DISPONÍVEIS:\n" +
                "- list_projects: Lista projetos existentes\n" +
                "- list_tasks: Lista tarefas (use projectId para filtrar)\n" +
                "- create_task: Cria tarefa em projeto EXISTENTE\n" +
                "- update_task: Atualiza tarefa específica\n" +
                "- move_task: Move tarefa para novo status\n" +
                "- create_project: Use APENAS se explicitamente solicitado\n\n" +
                "FLUXO RECOMENDADO:\n" +
                "1. Se usuário mencionar projeto, use list_projects primeiro\n" +
                "2. Identifique o projeto correto pelo nome/ID\n" +
                "3. Execute a ação solicitada no projeto correto\n" +
                "4. Confirme o resultado\n" +
                "5. Se usuário disser 'essa tarefa' ou similar, use o contexto para identificar qual tarefa";

            messages.add(roleMsg("system", systemPrompt));

            // Processar mensagem do usuário para resolver referências contextuais
            String processedMessage = resolveContextualReferences(message, context);
            messages.add(roleMsg("user", processedMessage));

            // Obter ferramentas MCP
            List<Map<String, Object>> tools = getMCPTools();
            System.out.println("Ferramentas MCP disponíveis: " + tools.size());

            // Chamar IA com ferramentas MCP
            Map<String, Object> response = anthropicClient.chatCompletion(messages, tools, "anthropic/claude-sonnet-4");
            System.out.println("Resposta inicial recebida");

            // Loop de execução de ferramentas MCP
            for (int i = 0; i < 5; i++) {
                System.out.println("=== ITERAÇÃO MCP " + (i+1) + " ===");

                Map<String, Object> assistantMsg = extractMessage(response);
                if (assistantMsg == null) {
                    System.out.println("Nenhuma mensagem extraída, parando loop");
                    break;
                }

                messages.add(assistantMsg);
                List<Map<String, Object>> toolCalls = extractToolCalls(assistantMsg);
                System.out.println("Tool calls encontradas: " + (toolCalls != null ? toolCalls.size() : 0));

                if (toolCalls == null || toolCalls.isEmpty()) {
                    System.out.println("Nenhuma tool call, finalizando");
                    break; // Resposta final
                }

                // Executar ferramentas via MCPClient
                for (Map<String, Object> toolCall : toolCalls) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fn = (Map<String, Object>) toolCall.get("function");
                    String name = String.valueOf(fn.get("name"));
                    String argsJson = String.valueOf(fn.get("arguments"));

                    System.out.println("Executando ferramenta: " + name);
                    System.out.println("Argumentos: " + argsJson);

                    String result = executeMCPTool(name, argsJson, user, context, sessionId);
                    System.out.println("Resultado: " + result);

                    Map<String, Object> toolResult = new HashMap<>();
                    toolResult.put("role", "tool");
                    toolResult.put("tool_call_id", toolCall.get("id"));
                    toolResult.put("content", result);
                    messages.add(toolResult);
                }

                response = anthropicClient.chatCompletion(messages, tools, "anthropic/claude-sonnet-4");
            }

            // Extrair resposta final
            Map<String, Object> finalMsg = extractMessage(response);
            String finalContent = "✅ Processado com sucesso!";
            if (finalMsg != null) {
                Object content = finalMsg.get("content");
                finalContent = content != null ? String.valueOf(content) : finalContent;
            }

            // Adicionar resposta da IA ao Zep Memory
            zepMemoryService.addMessage(sessionId, "assistant", finalContent,
                Map.of("processed_with_mcp", true, "timestamp", System.currentTimeMillis()));

            return createResponse(finalContent);

        } catch (Exception e) {
            System.err.println("Erro no processamento MCP: " + e.getMessage());
            e.printStackTrace();
            return createResponse("❌ Erro ao processar com MCP: " + e.getMessage());
        }
    }

    /**
     * Obtém ferramentas MCP no formato esperado pelo AnthropicClient
     */
    private List<Map<String, Object>> getMCPTools() {
        List<Map<String, Object>> mcpTools = mcpClient.getAvailableTools();
        List<Map<String, Object>> anthropicTools = new ArrayList<>();

        for (Map<String, Object> mcpTool : mcpTools) {
            Map<String, Object> anthropicTool = new HashMap<>();
            anthropicTool.put("type", "function");

            Map<String, Object> function = new HashMap<>();
            function.put("name", mcpTool.get("name"));
            function.put("description", mcpTool.get("description"));
            function.put("parameters", mcpTool.get("input_schema"));

            anthropicTool.put("function", function);
            anthropicTools.add(anthropicTool);
        }

        return anthropicTools;
    }

    /**
     * Executa ferramenta MCP e retorna resultado como JSON string
     */
    private String executeMCPTool(String toolName, String argsJson, UserPrincipal user, ChatContext context, String sessionId) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> args = mapper.readValue(argsJson, Map.class);

            Map<String, Object> result = mcpClient.executeTool(toolName, args, user);

            // Atualizar contexto baseado no resultado
            updateContextFromResult(toolName, result, context, sessionId);

            return mapper.writeValueAsString(result);

        } catch (Exception e) {
            System.err.println("Erro ao executar ferramenta MCP " + toolName + ": " + e.getMessage());
            return "{\"success\": false, \"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * Resolve referências contextuais na mensagem do usuário
     */
    private String resolveContextualReferences(String message, ChatContext context) {
        String processedMessage = message;

        // Detectar referências a "essa tarefa", "esta tarefa", "a tarefa"
        if (message.toLowerCase().matches(".*\\b(essa|esta|a) tarefa\\b.*")) {
            Map<String, Object> lastTask = context.getLastMentionedTask();
            if (!lastTask.isEmpty()) {
                String taskInfo = "tarefa ID " + lastTask.get("id") + " ('" + lastTask.get("title") + "')";
                processedMessage = processedMessage.replaceAll("(?i)\\b(essa|esta|a) tarefa\\b", taskInfo);
                System.out.println("Referência resolvida: 'essa tarefa' -> " + taskInfo);
            }
        }

        // Detectar referências a "esse projeto", "este projeto", "o projeto"
        if (message.toLowerCase().matches(".*\\b(esse|este|o) projeto\\b.*")) {
            Map<String, Object> lastProject = context.getLastMentionedProject();
            if (!lastProject.isEmpty()) {
                String projectInfo = "projeto ID " + lastProject.get("id") + " ('" + lastProject.get("name") + "')";
                processedMessage = processedMessage.replaceAll("(?i)\\b(esse|este|o) projeto\\b", projectInfo);
                System.out.println("Referência resolvida: 'esse projeto' -> " + projectInfo);
            }
        }

        // Detectar "mova para", "mude para", "coloque em" sem especificar tarefa
        if (message.toLowerCase().matches(".*\\b(mova|mude|coloque|ponha)\\b.*\\b(para|em)\\b.*") &&
            !message.toLowerCase().contains("tarefa") && !message.toLowerCase().contains("id")) {
            Map<String, Object> lastTask = context.getLastMentionedTask();
            if (!lastTask.isEmpty()) {
                String taskInfo = " a tarefa ID " + lastTask.get("id") + " ('" + lastTask.get("title") + "')";
                processedMessage = "Mova" + taskInfo + " " + message.toLowerCase().replaceFirst(".*\\b(mova|mude|coloque|ponha)\\b", "");
                System.out.println("Referência implícita resolvida: '" + message + "' -> " + processedMessage);
            }
        }

        return processedMessage;
    }

    /**
     * Atualiza o contexto do chat baseado no resultado das ferramentas MCP
     */
    private void updateContextFromResult(String toolName, Map<String, Object> result, ChatContext context, String sessionId) {
        try {
            if (result == null || !Boolean.TRUE.equals(result.get("success"))) {
                return; // Não atualizar contexto se houve erro
            }

            switch (toolName) {
                case "create_task":
                    if (result.containsKey("taskId") && result.containsKey("title")) {
                        Map<String, Object> task = new HashMap<>();
                        task.put("id", result.get("taskId"));
                        task.put("title", result.get("title"));
                        task.put("status", "BACKLOG"); // Status padrão
                        task.put("projectId", result.get("projectId"));
                        context.addTask(task);

                        // Adicionar ao Zep Memory
                        zepMemoryService.addTaskContext(sessionId, "create_task", task);
                        zepMemoryService.addFact(sessionId,
                            "Tarefa '" + result.get("title") + "' foi criada com ID " + result.get("taskId"),
                            Map.of("task_id", result.get("taskId"), "action", "created"));

                        System.out.println("Contexto atualizado: Nova tarefa ID " + result.get("taskId"));
                    }
                    break;

                case "create_project":
                    if (result.containsKey("projectId") && result.containsKey("name")) {
                        Map<String, Object> project = new HashMap<>();
                        project.put("id", result.get("projectId"));
                        project.put("name", result.get("name"));
                        context.addProject(project);

                        // Adicionar ao Zep Memory
                        zepMemoryService.addProjectContext(sessionId, "create_project", project);
                        zepMemoryService.addFact(sessionId,
                            "Projeto '" + result.get("name") + "' foi criado com ID " + result.get("projectId"),
                            Map.of("project_id", result.get("projectId"), "action", "created"));

                        System.out.println("Contexto atualizado: Novo projeto ID " + result.get("projectId"));
                    }
                    break;

                case "list_tasks":
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> tasks = (List<Map<String, Object>>) result.get("tasks");
                    if (tasks != null && !tasks.isEmpty()) {
                        // Adicionar a primeira tarefa como última mencionada
                        context.addTask(tasks.get(0));
                        System.out.println("Contexto atualizado: Tarefas listadas, foco na primeira");
                    }
                    break;

                case "list_projects":
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> projects = (List<Map<String, Object>>) result.get("projects");
                    if (projects != null && !projects.isEmpty()) {
                        // Adicionar o primeiro projeto como último mencionado
                        context.addProject(projects.get(0));
                        System.out.println("Contexto atualizado: Projetos listados, foco no primeiro");
                    }
                    break;

                case "update_task":
                case "move_task":
                    if (result.containsKey("taskId")) {
                        // Manter a tarefa atualizada no contexto
                        Map<String, Object> task = new HashMap<>();
                        task.put("id", result.get("taskId"));
                        task.put("title", result.getOrDefault("title", "Tarefa atualizada"));
                        task.put("status", result.getOrDefault("status", "UNKNOWN"));
                        context.addTask(task);

                        // Adicionar ao Zep Memory
                        zepMemoryService.addTaskContext(sessionId, toolName, task);
                        zepMemoryService.addFact(sessionId,
                            "Tarefa ID " + result.get("taskId") + " foi " +
                            (toolName.equals("move_task") ? "movida" : "atualizada"),
                            Map.of("task_id", result.get("taskId"), "action", toolName));

                        System.out.println("Contexto atualizado: Tarefa " + result.get("taskId") + " modificada");
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("Erro ao atualizar contexto: " + e.getMessage());
        }
    }

    // Método processWithAI removido - substituído por processWithMCP

    // Método listProjects removido - substituído por MCP

    // Método listTasks removido - substituído por MCP

    // Método executeTaskCreationDirectly removido - substituído por MCP

    // Método extractTaskNames removido - substituído por MCP

    // Método executeConfirmedAction removido - sistema de confirmação não mais necessário com MCP

    // Método handleActionConfirmation removido - sistema de confirmação não mais necessário com MCP

    // Métodos auxiliares

    private Map<String, Object> roleMsg(String role, String content) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        return msg;
    }

    private ResponseEntity<Map<String, Object>> createResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        return ResponseEntity.ok(response);
    }

    // Métodos auxiliares para extração e execução de ferramentas
    private Map<String, Object> extractMessage(Map<String, Object> response) {
        if (response == null) return null;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) return null;

        Map<String, Object> choice = choices.get(0);
        return (Map<String, Object>) choice.get("message");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractToolCalls(Map<String, Object> message) {
        if (message == null) return null;
        return (List<Map<String, Object>>) message.get("tool_calls");
    }

    // Métodos buildToolsSchema e createTool removidos - substituídos por MCP

    // Método executeTool antigo removido - substituído por executeMCPTool

    // Método createAuthentication removido - não mais necessário com MCP

    // Classe para armazenar contexto do chat
    private static class ChatContext {
        private final List<Map<String, Object>> recentTasks = new ArrayList<>();
        private final List<Map<String, Object>> recentProjects = new ArrayList<>();
        private final Map<String, Object> lastMentionedTask = new HashMap<>();
        private final Map<String, Object> lastMentionedProject = new HashMap<>();
        private long lastUpdated = System.currentTimeMillis();

        public void addTask(Map<String, Object> task) {
            recentTasks.add(0, task); // Adiciona no início
            if (recentTasks.size() > 10) { // Mantém apenas as 10 mais recentes
                recentTasks.remove(recentTasks.size() - 1);
            }
            lastMentionedTask.clear();
            lastMentionedTask.putAll(task);
            lastUpdated = System.currentTimeMillis();
        }

        public void addProject(Map<String, Object> project) {
            recentProjects.add(0, project); // Adiciona no início
            if (recentProjects.size() > 5) { // Mantém apenas os 5 mais recentes
                recentProjects.remove(recentProjects.size() - 1);
            }
            lastMentionedProject.clear();
            lastMentionedProject.putAll(project);
            lastUpdated = System.currentTimeMillis();
        }

        public String getContextSummary() {
            StringBuilder context = new StringBuilder();

            if (!lastMentionedTask.isEmpty()) {
                context.append("ÚLTIMA TAREFA MENCIONADA: ")
                       .append("ID ").append(lastMentionedTask.get("id"))
                       .append(" - ").append(lastMentionedTask.get("title"))
                       .append(" (Status: ").append(lastMentionedTask.get("status")).append(")\n");
            }

            if (!lastMentionedProject.isEmpty()) {
                context.append("ÚLTIMO PROJETO MENCIONADO: ")
                       .append("ID ").append(lastMentionedProject.get("id"))
                       .append(" - ").append(lastMentionedProject.get("name")).append("\n");
            }

            if (!recentTasks.isEmpty()) {
                context.append("TAREFAS RECENTES NO CONTEXTO:\n");
                for (int i = 0; i < Math.min(5, recentTasks.size()); i++) {
                    Map<String, Object> task = recentTasks.get(i);
                    context.append("- ID ").append(task.get("id"))
                           .append(": ").append(task.get("title"))
                           .append(" (").append(task.get("status")).append(")\n");
                }
            }

            return context.toString();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - lastUpdated > 30 * 60 * 1000; // 30 minutos
        }

        public Map<String, Object> getLastMentionedTask() {
            return new HashMap<>(lastMentionedTask);
        }

        public Map<String, Object> getLastMentionedProject() {
            return new HashMap<>(lastMentionedProject);
        }

        public List<Map<String, Object>> getRecentTasks() {
            return new ArrayList<>(recentTasks);
        }

        public List<Map<String, Object>> getRecentProjects() {
            return new ArrayList<>(recentProjects);
        }
    }
}