package com.projectmanagement.service;

import com.projectmanagement.model.Task;
import com.projectmanagement.model.TaskStatus;
import com.projectmanagement.model.User;
import com.projectmanagement.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    public List<Task> getTasksByProject(Long projectId) {
        return taskRepository.findByProjectId(projectId);
    }

    public List<Task> getTasksByProjectOrderedForKanban(Long projectId) {
        return taskRepository.findByProjectIdOrderByStatusAndPriority(projectId);
    }

    public List<Task> getTasksByAssignedUser(User assignedUser) {
        return taskRepository.findByAssignedUser(assignedUser);
    }

    public List<Task> getTasksByAssignedUserId(Long userId) {
        return taskRepository.findByAssignedUserIdOrderByPriorityAndDueDate(userId);
    }

    public List<Task> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    public List<Task> getTasksByProjectAndStatus(Long projectId, TaskStatus status) {
        return taskRepository.findByProjectIdAndStatus(projectId, status);
    }

    public Task createTask(Task task) {
        return taskRepository.save(task);
    }

    public Task updateTask(Long id, Task taskDetails) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));

        task.setTitle(taskDetails.getTitle());
        task.setDescription(taskDetails.getDescription());
        task.setStatus(taskDetails.getStatus());
        task.setPriority(taskDetails.getPriority());
        task.setDueDate(taskDetails.getDueDate());
        task.setAssignedUser(taskDetails.getAssignedUser());

        return taskRepository.save(task);
    }

    public Task updateTaskStatus(Long id, String status) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));

        task.setStatus(status);
        return taskRepository.save(task);
    }

    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
        taskRepository.delete(task);
    }

    public Long getTaskCountByProjectAndStatus(Long projectId, TaskStatus status) {
        return taskRepository.countByProjectIdAndStatus(projectId, status);
    }
}
