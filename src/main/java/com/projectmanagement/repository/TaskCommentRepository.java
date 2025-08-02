package com.projectmanagement.repository;

import com.projectmanagement.model.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    
    @Query("SELECT tc FROM TaskComment tc WHERE tc.task.id = :taskId ORDER BY tc.createdAt ASC")
    List<TaskComment> findByTaskIdOrderByCreatedAtAsc(@Param("taskId") Long taskId);
    
    @Query("SELECT tc FROM TaskComment tc WHERE tc.createdBy.id = :userId ORDER BY tc.createdAt DESC")
    List<TaskComment> findByCreatedByIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(tc) FROM TaskComment tc WHERE tc.task.id = :taskId")
    Long countByTaskId(@Param("taskId") Long taskId);
}
