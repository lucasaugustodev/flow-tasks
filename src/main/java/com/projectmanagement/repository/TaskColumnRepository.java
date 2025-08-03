package com.projectmanagement.repository;

import com.projectmanagement.model.TaskColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskColumnRepository extends JpaRepository<TaskColumn, Long> {
    
    @Query("SELECT tc FROM TaskColumn tc WHERE tc.project.id = :projectId ORDER BY tc.order ASC")
    List<TaskColumn> findByProjectIdOrderByOrder(@Param("projectId") Long projectId);
    
    @Query("SELECT tc FROM TaskColumn tc WHERE tc.project.id = :projectId AND tc.isDefault = true ORDER BY tc.order ASC")
    List<TaskColumn> findDefaultColumnsByProjectId(@Param("projectId") Long projectId);
    
    @Query("SELECT COALESCE(MAX(tc.order), 0) FROM TaskColumn tc WHERE tc.project.id = :projectId")
    Integer findMaxOrderByProjectId(@Param("projectId") Long projectId);
    
    @Query("SELECT COUNT(tc) FROM TaskColumn tc WHERE tc.project.id = :projectId")
    Long countByProjectId(@Param("projectId") Long projectId);
}
