package com.projectmanagement.service;

import com.projectmanagement.model.Project;
import com.projectmanagement.model.TaskColumn;
import com.projectmanagement.model.User;
import com.projectmanagement.repository.TaskColumnRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class TaskColumnService {

    @Autowired
    private TaskColumnRepository taskColumnRepository;

    @Autowired
    private ProjectService projectService;

    public List<TaskColumn> getColumnsByProject(Long projectId) {
        List<TaskColumn> columns = taskColumnRepository.findByProjectIdOrderByOrder(projectId);
        
        // If no columns exist, create default ones
        if (columns.isEmpty()) {
            columns = createDefaultColumns(projectId);
        }
        
        return columns;
    }

    public Optional<TaskColumn> getColumnById(Long id) {
        return taskColumnRepository.findById(id);
    }

    public TaskColumn createColumn(TaskColumn column, Long projectId, User createdBy) {
        Optional<Project> project = projectService.getProjectById(projectId);
        if (project.isEmpty()) {
            throw new RuntimeException("Project not found");
        }

        column.setProject(project.get());
        column.setCreatedBy(createdBy);
        
        // Set order to be last
        if (column.getOrder() == null) {
            Integer maxOrder = taskColumnRepository.findMaxOrderByProjectId(projectId);
            column.setOrder(maxOrder + 1);
        }

        return taskColumnRepository.save(column);
    }

    public TaskColumn updateColumn(Long id, TaskColumn updatedColumn, User user) {
        Optional<TaskColumn> existingColumn = taskColumnRepository.findById(id);
        if (existingColumn.isEmpty()) {
            throw new RuntimeException("Column not found");
        }

        TaskColumn column = existingColumn.get();
        
        // Check if user has permission to update (project owner or column creator)
        if (!column.getProject().getCreatedBy().getId().equals(user.getId()) && 
            !column.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("User does not have permission to update this column");
        }

        column.setName(updatedColumn.getName());
        column.setDescription(updatedColumn.getDescription());
        column.setColor(updatedColumn.getColor());
        if (updatedColumn.getOrder() != null) {
            column.setOrder(updatedColumn.getOrder());
        }

        return taskColumnRepository.save(column);
    }

    public void deleteColumn(Long id, User user) {
        Optional<TaskColumn> column = taskColumnRepository.findById(id);
        if (column.isEmpty()) {
            throw new RuntimeException("Column not found");
        }

        TaskColumn col = column.get();
        
        // Check if user has permission to delete (project owner or column creator)
        if (!col.getProject().getCreatedBy().getId().equals(user.getId()) && 
            !col.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("User does not have permission to delete this column");
        }

        // Don't allow deletion of default columns
        if (col.getIsDefault()) {
            throw new RuntimeException("Cannot delete default columns");
        }

        taskColumnRepository.deleteById(id);
    }

    public List<TaskColumn> createDefaultColumns(Long projectId) {
        Optional<Project> project = projectService.getProjectById(projectId);
        if (project.isEmpty()) {
            throw new RuntimeException("Project not found");
        }

        Project proj = project.get();
        User projectOwner = proj.getCreatedBy();

        List<TaskColumn> defaultColumns = Arrays.asList(
            new TaskColumn("Backlog", "Tarefas em espera", 1, proj, projectOwner),
            new TaskColumn("A Fazer", "Tarefas prontas para desenvolvimento", 2, proj, projectOwner),
            new TaskColumn("Em Progresso", "Tarefas sendo desenvolvidas", 3, proj, projectOwner),
            new TaskColumn("Em Revisão", "Tarefas em revisão", 4, proj, projectOwner),
            new TaskColumn("Concluído", "Tarefas finalizadas", 5, proj, projectOwner)
        );

        // Mark as default columns
        defaultColumns.forEach(col -> col.setIsDefault(true));

        // Set colors
        defaultColumns.get(0).setColor("#6B7280"); // Gray
        defaultColumns.get(1).setColor("#3B82F6"); // Blue
        defaultColumns.get(2).setColor("#F59E0B"); // Yellow
        defaultColumns.get(3).setColor("#8B5CF6"); // Purple
        defaultColumns.get(4).setColor("#10B981"); // Green

        return taskColumnRepository.saveAll(defaultColumns);
    }

    public boolean hasUserAccessToColumn(Long columnId, Long userId) {
        Optional<TaskColumn> column = taskColumnRepository.findById(columnId);
        if (column.isEmpty()) {
            return false;
        }

        // Check if user has access to the project
        return projectService.hasUserAccess(column.get().getProject().getId(), userId);
    }
}
