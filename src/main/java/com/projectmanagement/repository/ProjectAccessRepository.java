package com.projectmanagement.repository;

import com.projectmanagement.model.ProjectAccess;
import com.projectmanagement.model.AccessRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectAccessRepository extends JpaRepository<ProjectAccess, Long> {
    
    List<ProjectAccess> findByProjectId(Long projectId);
    
    List<ProjectAccess> findByUserId(Long userId);
    
    Optional<ProjectAccess> findByProjectIdAndUserId(Long projectId, Long userId);
    
    List<ProjectAccess> findByProjectIdAndRole(Long projectId, AccessRole role);
    
    @Query("SELECT pa FROM ProjectAccess pa WHERE pa.project.id = :projectId ORDER BY pa.role, pa.user.fullName")
    List<ProjectAccess> findByProjectIdOrderByRoleAndUserName(@Param("projectId") Long projectId);
    
    @Query("SELECT COUNT(pa) FROM ProjectAccess pa WHERE pa.project.id = :projectId AND pa.role = :role")
    Long countByProjectIdAndRole(@Param("projectId") Long projectId, @Param("role") AccessRole role);
    
    boolean existsByProjectIdAndUserId(Long projectId, Long userId);
}
