package com.projectmanagement.controller;

import com.projectmanagement.model.Task;
import com.projectmanagement.model.TaskChecklistItem;
import com.projectmanagement.model.User;
import com.projectmanagement.service.TaskChecklistItemService;
import com.projectmanagement.service.TaskService;
import com.projectmanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/tasks/{taskId}/checklist")
public class TaskChecklistController {

    @Autowired
    private TaskChecklistItemService taskChecklistItemService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<TaskChecklistItem>> getChecklistByTaskId(@PathVariable Long taskId) {
        List<TaskChecklistItem> items = taskChecklistItemService.getChecklistItemsByTaskId(taskId);
        return ResponseEntity.ok(items);
    }

    @PostMapping
    public ResponseEntity<?> createChecklistItem(@PathVariable Long taskId, 
                                               @RequestBody Map<String, String> request,
                                               Authentication authentication) {
        try {
            String description = request.get("description");
            if (description == null || description.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Description is required");
            }

            Optional<Task> taskOpt = taskService.getTaskById(taskId);
            if (!taskOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Optional<User> userOpt = userService.getUserByUsername(authentication.getName());
            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest().body("User not found");
            }

            TaskChecklistItem item = taskChecklistItemService.createChecklistItem(description, taskOpt.get(), userOpt.get());
            return ResponseEntity.ok(item);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating checklist item: " + e.getMessage());
        }
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<?> updateChecklistItem(@PathVariable Long taskId,
                                               @PathVariable Long itemId,
                                               @RequestBody Map<String, String> request) {
        try {
            String description = request.get("description");
            if (description == null || description.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Description is required");
            }

            TaskChecklistItem updatedItem = taskChecklistItemService.updateChecklistItem(itemId, description);
            if (updatedItem != null) {
                return ResponseEntity.ok(updatedItem);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating checklist item: " + e.getMessage());
        }
    }

    @PutMapping("/{itemId}/toggle")
    public ResponseEntity<?> toggleChecklistItem(@PathVariable Long taskId,
                                                @PathVariable Long itemId,
                                                Authentication authentication) {
        try {
            Optional<User> userOpt = userService.getUserByUsername(authentication.getName());
            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest().body("User not found");
            }

            TaskChecklistItem updatedItem = taskChecklistItemService.toggleChecklistItem(itemId, userOpt.get());
            if (updatedItem != null) {
                return ResponseEntity.ok(updatedItem);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error toggling checklist item: " + e.getMessage());
        }
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<?> deleteChecklistItem(@PathVariable Long taskId,
                                                @PathVariable Long itemId) {
        try {
            boolean deleted = taskChecklistItemService.deleteChecklistItem(itemId);
            if (deleted) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting checklist item: " + e.getMessage());
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getChecklistStats(@PathVariable Long taskId) {
        Long total = taskChecklistItemService.getChecklistItemCountByTaskId(taskId);
        Long completed = taskChecklistItemService.getCompletedChecklistItemCountByTaskId(taskId);
        
        return ResponseEntity.ok(Map.of(
            "total", total,
            "completed", completed,
            "remaining", total - completed
        ));
    }
}
