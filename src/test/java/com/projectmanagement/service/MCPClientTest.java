package com.projectmanagement.service;

import com.projectmanagement.controller.ProjectController;
import com.projectmanagement.controller.TaskController;
import com.projectmanagement.model.Project;
import com.projectmanagement.model.Task;
import com.projectmanagement.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class MCPClientTest {

    @Mock
    private ProjectController projectController;

    @Mock
    private TaskController taskController;

    @InjectMocks
    private MCPClient mcpClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetAvailableTools() {
        // Act
        List<Map<String, Object>> tools = mcpClient.getAvailableTools();

        // Assert
        assertNotNull(tools);
        assertFalse(tools.isEmpty());
        
        // Verificar se contém as ferramentas esperadas
        List<String> toolNames = tools.stream()
            .map(tool -> (String) tool.get("name"))
            .toList();
        
        assertTrue(toolNames.contains("create_task"));
        assertTrue(toolNames.contains("create_project"));
        assertTrue(toolNames.contains("list_projects"));
        assertTrue(toolNames.contains("list_tasks"));
        assertTrue(toolNames.contains("update_task"));
        assertTrue(toolNames.contains("move_task"));
    }

    @Test
    public void testGetToolSchema() {
        // Act
        Map<String, Object> schema = mcpClient.getToolSchema("create_task");

        // Assert
        assertNotNull(schema);
        assertEquals("create_task", schema.get("name"));
        assertEquals("Cria uma nova tarefa em um projeto específico", schema.get("description"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> inputSchema = (Map<String, Object>) schema.get("input_schema");
        assertNotNull(inputSchema);
        assertEquals("object", inputSchema.get("type"));
    }

    @Test
    public void testGetToolSchemaNotFound() {
        // Act
        Map<String, Object> schema = mcpClient.getToolSchema("nonexistent_tool");

        // Assert
        assertNull(schema);
    }

    @Test
    public void testExecuteCreateTaskSuccess() {
        // Arrange
        UserPrincipal user = mock(UserPrincipal.class);
        Task mockTask = new Task();
        mockTask.setId(1L);
        mockTask.setTitle("Test Task");
        
        ResponseEntity<Task> mockResponse = ResponseEntity.ok(mockTask);
        when(taskController.createTask(any(Task.class), any(Authentication.class)))
            .thenReturn(mockResponse);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("title", "Test Task");
        arguments.put("projectId", 1);
        arguments.put("description", "Test Description");

        // Act
        Map<String, Object> result = mcpClient.executeTool("create_task", arguments, user);

        // Assert
        assertNotNull(result);
        assertEquals(true, result.get("success"));
        assertEquals(1L, result.get("taskId"));
        assertEquals("Test Task", result.get("title"));
        assertEquals(1L, result.get("projectId"));
        assertTrue(result.get("message").toString().contains("Test Task"));

        verify(taskController, times(1)).createTask(any(Task.class), any(Authentication.class));
    }

    @Test
    public void testExecuteListProjects() {
        // Arrange
        UserPrincipal user = mock(UserPrincipal.class);
        
        List<Project> mockProjects = new ArrayList<>();
        Project project1 = new Project();
        project1.setId(1L);
        project1.setName("Project 1");
        project1.setDescription("Description 1");
        mockProjects.add(project1);
        
        ResponseEntity<List<Project>> mockResponse = ResponseEntity.ok(mockProjects);
        when(projectController.getAllProjects(any(Authentication.class)))
            .thenReturn(mockResponse);

        // Act
        Map<String, Object> result = mcpClient.executeTool("list_projects", new HashMap<>(), user);

        // Assert
        assertNotNull(result);
        assertEquals(true, result.get("success"));
        assertEquals(1, result.get("count"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> projects = (List<Map<String, Object>>) result.get("projects");
        assertNotNull(projects);
        assertEquals(1, projects.size());
        assertEquals(1L, projects.get(0).get("id"));
        assertEquals("Project 1", projects.get(0).get("name"));

        verify(projectController, times(1)).getAllProjects(any(Authentication.class));
    }

    @Test
    public void testExecuteToolNotFound() {
        // Arrange
        UserPrincipal user = mock(UserPrincipal.class);

        // Act
        Map<String, Object> result = mcpClient.executeTool("nonexistent_tool", new HashMap<>(), user);

        // Assert
        assertNotNull(result);
        assertEquals(false, result.get("success"));
        assertTrue(result.get("error").toString().contains("Ferramenta não encontrada"));
    }
}
