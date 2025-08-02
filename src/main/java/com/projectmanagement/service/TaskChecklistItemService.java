package com.projectmanagement.service;

import com.projectmanagement.model.Task;
import com.projectmanagement.model.TaskChecklistItem;
import com.projectmanagement.model.User;
import com.projectmanagement.repository.TaskChecklistItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TaskChecklistItemService {

    @Autowired
    private TaskChecklistItemRepository taskChecklistItemRepository;

    public List<TaskChecklistItem> getChecklistItemsByTaskId(Long taskId) {
        return taskChecklistItemRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    public TaskChecklistItem createChecklistItem(String description, Task task, User user) {
        TaskChecklistItem item = new TaskChecklistItem(description, task, user);
        return taskChecklistItemRepository.save(item);
    }

    public Optional<TaskChecklistItem> getChecklistItemById(Long id) {
        return taskChecklistItemRepository.findById(id);
    }

    public TaskChecklistItem updateChecklistItem(Long id, String description) {
        Optional<TaskChecklistItem> itemOpt = taskChecklistItemRepository.findById(id);
        if (itemOpt.isPresent()) {
            TaskChecklistItem item = itemOpt.get();
            item.setDescription(description);
            return taskChecklistItemRepository.save(item);
        }
        return null;
    }

    public TaskChecklistItem toggleChecklistItem(Long id, User user) {
        Optional<TaskChecklistItem> itemOpt = taskChecklistItemRepository.findById(id);
        if (itemOpt.isPresent()) {
            TaskChecklistItem item = itemOpt.get();
            item.setIsCompleted(!item.getIsCompleted());
            if (item.getIsCompleted()) {
                item.setCompletedBy(user);
                item.setCompletedAt(LocalDateTime.now());
            } else {
                item.setCompletedBy(null);
                item.setCompletedAt(null);
            }
            return taskChecklistItemRepository.save(item);
        }
        return null;
    }

    public boolean deleteChecklistItem(Long id) {
        if (taskChecklistItemRepository.existsById(id)) {
            taskChecklistItemRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Long getChecklistItemCountByTaskId(Long taskId) {
        return taskChecklistItemRepository.countByTaskId(taskId);
    }

    public Long getCompletedChecklistItemCountByTaskId(Long taskId) {
        return taskChecklistItemRepository.countCompletedByTaskId(taskId);
    }

    public List<TaskChecklistItem> getChecklistItemsByTaskIdAndCompleted(Long taskId, Boolean completed) {
        return taskChecklistItemRepository.findByTaskIdAndCompletedOrderByCreatedAtAsc(taskId, completed);
    }
}
