package com.projectmanagement.controller;

import com.projectmanagement.service.MCPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller MCP (Model Context Protocol) para integração com Claude
 * Fornece endpoints para descoberta e execução de ferramentas
 */
@RestController
@RequestMapping("/mcp")
public class MCPController {

    @Autowired
    private MCPClient mcpClient;

    /**
     * Endpoint para listar todas as ferramentas disponíveis
     * GET /mcp/tools
     */
    @GetMapping("/tools")
    public ResponseEntity<Map<String, Object>> getTools() {
        try {
            List<Map<String, Object>> tools = mcpClient.getAvailableTools();
            
            Map<String, Object> response = new HashMap<>();
            response.put("tools", tools);
            response.put("count", tools.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Erro ao listar ferramentas: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Endpoint para executar uma ferramenta específica
     * POST /mcp/call
     * Body: {
     *   "name": "create_task",
     *   "arguments": {
     *     "title": "Nova tarefa",
     *     "projectId": 1
     *   }
     * }
     */
    @PostMapping("/call")
    public ResponseEntity<Map<String, Object>> callTool(@RequestBody Map<String, Object> request) {
        try {
            String toolName = (String) request.get("name");
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = (Map<String, Object>) request.get("arguments");
            
            if (toolName == null || toolName.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Nome da ferramenta é obrigatório");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Executar ferramenta através do MCPClient
            Map<String, Object> result = mcpClient.executeTool(toolName, arguments);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Erro ao executar ferramenta: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Endpoint para obter schema de uma ferramenta específica
     * GET /mcp/tools/{toolName}
     */
    @GetMapping("/tools/{toolName}")
    public ResponseEntity<Map<String, Object>> getToolSchema(@PathVariable String toolName) {
        try {
            Map<String, Object> schema = mcpClient.getToolSchema(toolName);
            
            if (schema == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Ferramenta não encontrada: " + toolName);
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(schema);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Erro ao obter schema: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
