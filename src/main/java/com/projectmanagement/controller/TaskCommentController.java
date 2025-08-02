package com.projectmanagement.controller;

import com.projectmanagement.model.Task;
import com.projectmanagement.model.TaskComment;
import com.projectmanagement.model.User;
import com.projectmanagement.service.TaskCommentService;
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
@RequestMapping("/api/tasks/{taskId}/comments")
public class TaskCommentController {

    @Autowired
    private TaskCommentService taskCommentService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<TaskComment>> getCommentsByTaskId(@PathVariable Long taskId) {
        List<TaskComment> comments = taskCommentService.getCommentsByTaskId(taskId);
        return ResponseEntity.ok(comments);
    }

    @PostMapping
    public ResponseEntity<?> createComment(@PathVariable Long taskId, 
                                         @RequestBody Map<String, String> request,
                                         Authentication authentication) {
        try {
            String content = request.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Content is required");
            }

            Optional<Task> taskOpt = taskService.getTaskById(taskId);
            if (!taskOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Optional<User> userOpt = userService.getUserByUsername(authentication.getName());
            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest().body("User not found");
            }

            TaskComment comment = taskCommentService.createComment(content, taskOpt.get(), userOpt.get());
            return ResponseEntity.ok(comment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating comment: " + e.getMessage());
        }
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(@PathVariable Long taskId,
                                         @PathVariable Long commentId,
                                         @RequestBody Map<String, String> request,
                                         Authentication authentication) {
        try {
            String content = request.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Content is required");
            }

            Optional<TaskComment> commentOpt = taskCommentService.getCommentById(commentId);
            if (!commentOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            // Check if user is the owner of the comment
            TaskComment comment = commentOpt.get();
            if (!comment.getCreatedBy().getUsername().equals(authentication.getName())) {
                return ResponseEntity.status(403).body("You can only edit your own comments");
            }

            TaskComment updatedComment = taskCommentService.updateComment(commentId, content);
            return ResponseEntity.ok(updatedComment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating comment: " + e.getMessage());
        }
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long taskId,
                                         @PathVariable Long commentId,
                                         Authentication authentication) {
        try {
            Optional<TaskComment> commentOpt = taskCommentService.getCommentById(commentId);
            if (!commentOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            // Check if user is the owner of the comment
            TaskComment comment = commentOpt.get();
            if (!comment.getCreatedBy().getUsername().equals(authentication.getName())) {
                return ResponseEntity.status(403).body("You can only delete your own comments");
            }

            boolean deleted = taskCommentService.deleteComment(commentId);
            if (deleted) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting comment: " + e.getMessage());
        }
    }
}
