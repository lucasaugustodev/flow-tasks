package com.projectmanagement.repository;

import com.projectmanagement.model.Task;
import com.projectmanagement.model.TaskStatus;
import com.projectmanagement.model.User;
import com.projectmanagement.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    List<Task> findByProject(Project project);
    
    List<Task> findByProjectId(Long projectId);
    
    List<Task> findByAssignedUser(User assignedUser);
    
    List<Task> findByStatus(TaskStatus status);
    
    List<Task> findByProjectAndStatus(Project project, TaskStatus status);
    
    List<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status);
    
    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId ORDER BY t.status, t.priority DESC, t.createdAt")
    List<Task> findByProjectIdOrderByStatusAndPriority(@Param("projectId") Long projectId);
    
    @Query("SELECT t FROM Task t WHERE t.assignedUser.id = :userId ORDER BY t.priority DESC, t.dueDate")
    List<Task> findByAssignedUserIdOrderByPriorityAndDueDate(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.status = :status")
    Long countByProjectIdAndStatus(@Param("projectId") Long projectId, @Param("status") TaskStatus status);
}
