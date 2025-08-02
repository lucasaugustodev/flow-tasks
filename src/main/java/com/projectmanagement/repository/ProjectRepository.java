package com.projectmanagement.repository;

import com.projectmanagement.model.Project;
import com.projectmanagement.model.ProjectStatus;
import com.projectmanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    
    List<Project> findByStatus(ProjectStatus status);
    
    List<Project> findByCreatedBy(User createdBy);
    
    @Query("SELECT p FROM Project p WHERE p.name LIKE %:name%")
    List<Project> findByNameContaining(@Param("name") String name);
    
    @Query("SELECT p FROM Project p JOIN p.users u WHERE u.id = :userId")
    List<Project> findProjectsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT p FROM Project p WHERE p.createdBy.id = :userId OR :userId IN (SELECT u.id FROM p.users u)")
    List<Project> findProjectsByUserIdIncludingCreated(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(p) FROM Project p WHERE p.status = :status")
    Long countByStatus(@Param("status") ProjectStatus status);
}
