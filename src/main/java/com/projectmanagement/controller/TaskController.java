package com.projectmanagement.controller;

import com.projectmanagement.model.Task;
import com.projectmanagement.model.TaskStatus;
import com.projectmanagement.model.User;
import com.projectmanagement.security.UserPrincipal;
import com.projectmanagement.service.TaskService;
import com.projectmanagement.service.UserService;
import com.projectmanagement.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        List<Task> tasks = taskService.getTasksByAssignedUserId(userPrincipal.getId());
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        Optional<Task> task = taskService.getTaskById(id);
        if (task.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Check if user has access to the project this task belongs to
        if (!projectService.hasUserAccess(task.get().getProject().getId(), userPrincipal.getId())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(task.get());
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Task>> getTasksByProject(@PathVariable Long projectId, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // Check if user has access to this project
        if (!projectService.hasUserAccess(projectId, userPrincipal.getId())) {
            return ResponseEntity.status(403).build();
        }

        List<Task> tasks = taskService.getTasksByProjectOrderedForKanban(projectId);
        return ResponseEntity.ok(tasks);
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@Valid @RequestBody Task task, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // Check if user has access to the project
        if (!projectService.hasUserAccess(task.getProject().getId(), userPrincipal.getId())) {
            return ResponseEntity.status(403).build();
        }

        Optional<User> user = userService.getUserById(userPrincipal.getId());
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        task.setCreatedBy(user.get());
        Task savedTask = taskService.createTask(task);
        return ResponseEntity.ok(savedTask);
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<?> assignTask(@PathVariable Long id, @RequestBody Map<String, Object> assignData,
                                      Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            Optional<Task> taskOpt = taskService.getTaskById(id);
            if (taskOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Task task = taskOpt.get();

            // Check if user has access to the project
            if (!projectService.hasUserAccess(task.getProject().getId(), userPrincipal.getId())) {
                return ResponseEntity.status(403).build();
            }

            Object assignedUserIdObj = assignData.get("assignedUserId");
            if (assignedUserIdObj != null) {
                Long assignedUserId = Long.valueOf(assignedUserIdObj.toString());
                Optional<User> assignedUserOpt = userService.getUserById(assignedUserId);
                if (assignedUserOpt.isPresent()) {
                    task.setAssignedUser(assignedUserOpt.get());
                } else {
                    return ResponseEntity.badRequest().body("Assigned user not found");
                }
            } else {
                task.setAssignedUser(null); // Unassign
            }

            Task updatedTask = taskService.updateTask(id, task);
            return ResponseEntity.ok(updatedTask);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error assigning task: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @Valid @RequestBody Task taskDetails,
                                         Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        Optional<Task> existingTask = taskService.getTaskById(id);
        if (existingTask.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Check if user has access to the project
        if (!projectService.hasUserAccess(existingTask.get().getProject().getId(), userPrincipal.getId())) {
            return ResponseEntity.status(403).build();
        }

        try {
            Task updatedTask = taskService.updateTask(id, taskDetails);
            return ResponseEntity.ok(updatedTask);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Task> updateTaskStatus(@PathVariable Long id, @RequestParam TaskStatus status,
                                               Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        Optional<Task> existingTask = taskService.getTaskById(id);
        if (existingTask.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Check if user has access to the project
        if (!projectService.hasUserAccess(existingTask.get().getProject().getId(), userPrincipal.getId())) {
            return ResponseEntity.status(403).build();
        }

        try {
            Task updatedTask = taskService.updateTaskStatus(id, status);
            return ResponseEntity.ok(updatedTask);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        Optional<Task> existingTask = taskService.getTaskById(id);
        if (existingTask.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Check if user has access to the project and is admin/owner or task creator
        Long projectId = existingTask.get().getProject().getId();
        Optional<com.projectmanagement.model.AccessRole> userRole = projectService.getUserRole(projectId, userPrincipal.getId());
        
        boolean canDelete = userRole.isPresent() && 
                           (userRole.get().equals(com.projectmanagement.model.AccessRole.OWNER) ||
                            userRole.get().equals(com.projectmanagement.model.AccessRole.ADMIN) ||
                            existingTask.get().getCreatedBy().getId().equals(userPrincipal.getId()));

        if (!canDelete) {
            return ResponseEntity.status(403).build();
        }

        try {
            taskService.deleteTask(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
