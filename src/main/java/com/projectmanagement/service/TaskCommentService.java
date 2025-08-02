package com.projectmanagement.service;

import com.projectmanagement.model.Task;
import com.projectmanagement.model.TaskComment;
import com.projectmanagement.model.User;
import com.projectmanagement.repository.TaskCommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TaskCommentService {

    @Autowired
    private TaskCommentRepository taskCommentRepository;

    public List<TaskComment> getCommentsByTaskId(Long taskId) {
        return taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    public TaskComment createComment(String content, Task task, User user) {
        TaskComment comment = new TaskComment(content, task, user);
        return taskCommentRepository.save(comment);
    }

    public Optional<TaskComment> getCommentById(Long id) {
        return taskCommentRepository.findById(id);
    }

    public TaskComment updateComment(Long id, String content) {
        Optional<TaskComment> commentOpt = taskCommentRepository.findById(id);
        if (commentOpt.isPresent()) {
            TaskComment comment = commentOpt.get();
            comment.setContent(content);
            return taskCommentRepository.save(comment);
        }
        return null;
    }

    public boolean deleteComment(Long id) {
        if (taskCommentRepository.existsById(id)) {
            taskCommentRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Long getCommentCountByTaskId(Long taskId) {
        return taskCommentRepository.countByTaskId(taskId);
    }

    public List<TaskComment> getCommentsByUserId(Long userId) {
        return taskCommentRepository.findByCreatedByIdOrderByCreatedAtDesc(userId);
    }
}
