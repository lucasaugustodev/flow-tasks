package com.projectmanagement.controller;

import com.projectmanagement.dto.TaskColumnDTO;
import com.projectmanagement.model.TaskColumn;
import com.projectmanagement.model.User;
import com.projectmanagement.security.UserPrincipal;
import com.projectmanagement.service.ProjectService;
import com.projectmanagement.service.TaskColumnService;
import com.projectmanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/projects/{projectId}/columns")
public class TaskColumnController {

    @Autowired
    private TaskColumnService taskColumnService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<TaskColumnDTO>> getColumnsByProject(
            @PathVariable Long projectId,
            Authentication authentication) {

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        // Check if user has access to this project
        if (!projectService.hasUserAccess(projectId, userPrincipal.getId())) {
            return ResponseEntity.status(403).build();
        }

        List<TaskColumn> columns = taskColumnService.getColumnsByProject(projectId);
        List<TaskColumnDTO> columnDTOs = columns.stream()
                .map(TaskColumnDTO::new)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(columnDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskColumn> getColumnById(
            @PathVariable Long projectId,
            @PathVariable Long id, 
            Authentication authentication) {
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // Check if user has access to this column
        if (!taskColumnService.hasUserAccessToColumn(id, userPrincipal.getId())) {
            return ResponseEntity.status(403).build();
        }

        Optional<TaskColumn> column = taskColumnService.getColumnById(id);
        return column.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TaskColumn> createColumn(
            @PathVariable Long projectId,
            @Valid @RequestBody TaskColumn column, 
            Authentication authentication) {
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // Check if user has access to this project
        if (!projectService.hasUserAccess(projectId, userPrincipal.getId())) {
            return ResponseEntity.status(403).build();
        }

        Optional<User> user = userService.getUserById(userPrincipal.getId());
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            TaskColumn createdColumn = taskColumnService.createColumn(column, projectId, user.get());
            return ResponseEntity.ok(createdColumn);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskColumn> updateColumn(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @Valid @RequestBody TaskColumn column, 
            Authentication authentication) {
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        Optional<User> user = userService.getUserById(userPrincipal.getId());
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            TaskColumn updatedColumn = taskColumnService.updateColumn(id, column, user.get());
            return ResponseEntity.ok(updatedColumn);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteColumn(
            @PathVariable Long projectId,
            @PathVariable Long id, 
            Authentication authentication) {
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        Optional<User> user = userService.getUserById(userPrincipal.getId());
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            taskColumnService.deleteColumn(id, user.get());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }
}
