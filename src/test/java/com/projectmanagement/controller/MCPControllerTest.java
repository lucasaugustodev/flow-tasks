package com.projectmanagement.controller;

import com.projectmanagement.service.MCPClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Teste unitário simples sem contexto Spring
public class MCPControllerTest {

    @Mock
    private MCPClient mcpClient;

    @InjectMocks
    private MCPController mcpController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetTools() {
        // Arrange
        List<Map<String, Object>> mockTools = new ArrayList<>();
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", "create_task");
        tool.put("description", "Cria uma nova tarefa");
        mockTools.add(tool);

        when(mcpClient.getAvailableTools()).thenReturn(mockTools);

        // Act
        ResponseEntity<Map<String, Object>> response = mcpController.getTools();

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("tools"));
        assertTrue(body.containsKey("count"));
        assertEquals(1, body.get("count"));

        verify(mcpClient, times(1)).getAvailableTools();
    }

    @Test
    public void testCallTool() {
        // Arrange
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("success", true);
        mockResult.put("message", "Tarefa criada com sucesso!");

        Map<String, Object> request = new HashMap<>();
        request.put("name", "create_task");
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("title", "Nova tarefa");
        arguments.put("projectId", 1);
        request.put("arguments", arguments);

        when(mcpClient.executeTool(eq("create_task"), eq(arguments))).thenReturn(mockResult);

        // Act
        ResponseEntity<Map<String, Object>> response = mcpController.callTool(request);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals("Tarefa criada com sucesso!", body.get("message"));

        verify(mcpClient, times(1)).executeTool(eq("create_task"), eq(arguments));
    }

    @Test
    public void testCallToolWithoutName() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("title", "Nova tarefa");
        request.put("arguments", arguments);
        // Não incluir "name" no request

        // Act
        ResponseEntity<Map<String, Object>> response = mcpController.callTool(request);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Nome da ferramenta é obrigatório", body.get("error"));

        verify(mcpClient, never()).executeTool(any(), any());
    }

    @Test
    public void testGetToolSchema() {
        // Arrange
        Map<String, Object> mockSchema = new HashMap<>();
        mockSchema.put("name", "create_task");
        mockSchema.put("description", "Cria uma nova tarefa");

        when(mcpClient.getToolSchema("create_task")).thenReturn(mockSchema);

        // Act
        ResponseEntity<Map<String, Object>> response = mcpController.getToolSchema("create_task");

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("create_task", body.get("name"));
        assertEquals("Cria uma nova tarefa", body.get("description"));

        verify(mcpClient, times(1)).getToolSchema("create_task");
    }

    @Test
    public void testGetToolSchemaNotFound() {
        // Arrange
        when(mcpClient.getToolSchema("nonexistent_tool")).thenReturn(null);

        // Act
        ResponseEntity<Map<String, Object>> response = mcpController.getToolSchema("nonexistent_tool");

        // Assert
        assertNotNull(response);
        assertEquals(404, response.getStatusCodeValue());

        verify(mcpClient, times(1)).getToolSchema("nonexistent_tool");
    }
}
