package com.projectmanagement.service;

import com.projectmanagement.model.ProjectAccess;
import com.projectmanagement.model.AccessRole;
import com.projectmanagement.model.Project;
import com.projectmanagement.model.User;
import com.projectmanagement.repository.ProjectAccessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectAccessService {

    @Autowired
    private ProjectAccessRepository projectAccessRepository;

    public List<ProjectAccess> getAllProjectAccesses() {
        return projectAccessRepository.findAll();
    }

    public Optional<ProjectAccess> getProjectAccessById(Long id) {
        return projectAccessRepository.findById(id);
    }

    public List<ProjectAccess> getProjectAccessesByProject(Long projectId) {
        return projectAccessRepository.findByProjectIdOrderByRoleAndUserName(projectId);
    }

    public List<ProjectAccess> getProjectAccessesByUser(Long userId) {
        return projectAccessRepository.findByUserId(userId);
    }

    public Optional<ProjectAccess> getProjectAccessByProjectAndUser(Long projectId, Long userId) {
        return projectAccessRepository.findByProjectIdAndUserId(projectId, userId);
    }

    public List<ProjectAccess> getProjectAccessesByRole(Long projectId, AccessRole role) {
        return projectAccessRepository.findByProjectIdAndRole(projectId, role);
    }

    public ProjectAccess grantAccess(Project project, User user, AccessRole role, User grantedBy) {
        // Check if access already exists
        Optional<ProjectAccess> existingAccess = projectAccessRepository
                .findByProjectIdAndUserId(project.getId(), user.getId());
        
        if (existingAccess.isPresent()) {
            // Update existing access
            ProjectAccess access = existingAccess.get();
            access.setRole(role);
            access.setGrantedBy(grantedBy);
            return projectAccessRepository.save(access);
        } else {
            // Create new access
            ProjectAccess newAccess = new ProjectAccess(project, user, role, grantedBy);
            return projectAccessRepository.save(newAccess);
        }
    }

    public ProjectAccess updateAccess(Long id, AccessRole role, User updatedBy) {
        ProjectAccess access = projectAccessRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project access not found with id: " + id));

        access.setRole(role);
        access.setGrantedBy(updatedBy);

        return projectAccessRepository.save(access);
    }

    public void revokeAccess(Long id) {
        ProjectAccess access = projectAccessRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project access not found with id: " + id));
        projectAccessRepository.delete(access);
    }

    public void revokeAccessByProjectAndUser(Long projectId, Long userId) {
        Optional<ProjectAccess> access = projectAccessRepository.findByProjectIdAndUserId(projectId, userId);
        access.ifPresent(projectAccessRepository::delete);
    }

    public boolean hasAccess(Long projectId, Long userId) {
        return projectAccessRepository.existsByProjectIdAndUserId(projectId, userId);
    }

    public boolean hasRole(Long projectId, Long userId, AccessRole role) {
        Optional<ProjectAccess> access = projectAccessRepository.findByProjectIdAndUserId(projectId, userId);
        return access.map(pa -> pa.getRole().equals(role)).orElse(false);
    }

    public boolean hasMinimumRole(Long projectId, Long userId, AccessRole minimumRole) {
        Optional<ProjectAccess> access = projectAccessRepository.findByProjectIdAndUserId(projectId, userId);
        if (access.isEmpty()) {
            return false;
        }

        AccessRole userRole = access.get().getRole();
        return isRoleHigherOrEqual(userRole, minimumRole);
    }

    private boolean isRoleHigherOrEqual(AccessRole userRole, AccessRole minimumRole) {
        int userRoleLevel = getRoleLevel(userRole);
        int minimumRoleLevel = getRoleLevel(minimumRole);
        return userRoleLevel >= minimumRoleLevel;
    }

    private int getRoleLevel(AccessRole role) {
        switch (role) {
            case VIEWER: return 1;
            case MEMBER: return 2;
            case ADMIN: return 3;
            case OWNER: return 4;
            default: return 0;
        }
    }

    public Long getAccessCountByProjectAndRole(Long projectId, AccessRole role) {
        return projectAccessRepository.countByProjectIdAndRole(projectId, role);
    }
}
