package com.projectmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectmanagement.security.UserPrincipal;
import com.projectmanagement.service.AnthropicClient;
import com.projectmanagement.service.MCPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpSession;

import java.util.*;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    // Endpoint de teste removido - não mais necessário com MCP

    @Autowired
    private AnthropicClient anthropicClient;

    @Autowired
    private MCPClient mcpClient;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(
            @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal UserPrincipal user,
            HttpSession session) {

        String message = String.valueOf(req.get("message"));

        System.out.println("=== CHAT MCP ===");
        System.out.println("Mensagem recebida: " + message);

        // Processar com MCP - estratégia única
        return processWithMCP(message, user);
    }

    // Método processNewMessage removido - substituído por processWithMCP

    /**
     * Método único para processamento com MCP
     * Sempre usa ferramentas MCP - sem detecção manual
     */
    private ResponseEntity<Map<String, Object>> processWithMCP(String message, UserPrincipal user) {
        try {
            System.out.println("=== PROCESSANDO COM MCP ===");
            System.out.println("Mensagem: " + message);

            List<Map<String, Object>> messages = new ArrayList<>();

            // System prompt para MCP
            messages.add(roleMsg("system",
                "Você é um assistente para gerenciamento de projetos. " +
                "Você tem acesso a ferramentas para criar tarefas, listar projetos, atualizar tarefas, etc. " +
                "Use as ferramentas disponíveis sempre que apropriado. " +
                "Responda em português de forma útil e amigável. " +
                "Quando o usuário pedir para criar tarefas, use a ferramenta create_task. " +
                "Quando pedir para listar projetos, use list_projects. " +
                "Quando pedir para listar tarefas, use list_tasks. " +
                "Execute as ações diretamente usando as ferramentas - não peça confirmação."
            ));

            messages.add(roleMsg("user", message));

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

                    String result = executeMCPTool(name, argsJson, user);
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
    private String executeMCPTool(String toolName, String argsJson, UserPrincipal user) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> args = mapper.readValue(argsJson, Map.class);

            Map<String, Object> result = mcpClient.executeTool(toolName, args, user);
            return mapper.writeValueAsString(result);

        } catch (Exception e) {
            System.err.println("Erro ao executar ferramenta MCP " + toolName + ": " + e.getMessage());
            return "{\"success\": false, \"error\": \"" + e.getMessage() + "\"}";
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
}