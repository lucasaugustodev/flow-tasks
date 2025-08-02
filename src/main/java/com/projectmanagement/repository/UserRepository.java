package com.projectmanagement.repository;

import com.projectmanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Boolean existsByUsername(String username);
    
    Boolean existsByEmail(String email);
    
    List<User> findByIsActiveTrue();
    
    @Query("SELECT u FROM User u WHERE u.fullName LIKE %:name% AND u.isActive = true")
    List<User> findByFullNameContainingAndIsActiveTrue(@Param("name") String name);
    
    @Query("SELECT u FROM User u JOIN u.projects p WHERE p.id = :projectId")
    List<User> findUsersByProjectId(@Param("projectId") Long projectId);
}
