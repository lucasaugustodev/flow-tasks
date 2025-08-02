package com.projectmanagement.repository;

import com.projectmanagement.model.TaskChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskChecklistItemRepository extends JpaRepository<TaskChecklistItem, Long> {
    
    @Query("SELECT tci FROM TaskChecklistItem tci WHERE tci.task.id = :taskId ORDER BY tci.createdAt ASC")
    List<TaskChecklistItem> findByTaskIdOrderByCreatedAtAsc(@Param("taskId") Long taskId);
    
    @Query("SELECT COUNT(tci) FROM TaskChecklistItem tci WHERE tci.task.id = :taskId")
    Long countByTaskId(@Param("taskId") Long taskId);
    
    @Query("SELECT COUNT(tci) FROM TaskChecklistItem tci WHERE tci.task.id = :taskId AND tci.isCompleted = true")
    Long countCompletedByTaskId(@Param("taskId") Long taskId);
    
    @Query("SELECT tci FROM TaskChecklistItem tci WHERE tci.task.id = :taskId AND tci.isCompleted = :completed ORDER BY tci.createdAt ASC")
    List<TaskChecklistItem> findByTaskIdAndCompletedOrderByCreatedAtAsc(@Param("taskId") Long taskId, @Param("completed") Boolean completed);
}
