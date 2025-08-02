package com.projectmanagement.service;

import com.projectmanagement.model.Project;
import com.projectmanagement.model.ProjectStatus;
import com.projectmanagement.model.User;
import com.projectmanagement.model.ProjectAccess;
import com.projectmanagement.model.AccessRole;
import com.projectmanagement.repository.ProjectRepository;
import com.projectmanagement.repository.ProjectAccessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectAccessRepository projectAccessRepository;

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public Optional<Project> getProjectById(Long id) {
        return projectRepository.findById(id);
    }

    public List<Project> getProjectsByStatus(ProjectStatus status) {
        return projectRepository.findByStatus(status);
    }

    public List<Project> getProjectsByCreatedBy(User createdBy) {
        return projectRepository.findByCreatedBy(createdBy);
    }

    public List<Project> getProjectsByUser(Long userId) {
        return projectRepository.findProjectsByUserIdIncludingCreated(userId);
    }

    public List<Project> searchProjectsByName(String name) {
        return projectRepository.findByNameContaining(name);
    }

    @Transactional
    public Project createProject(Project project) {
        Project savedProject = projectRepository.save(project);
        
        // Automatically grant OWNER access to the creator
        ProjectAccess ownerAccess = new ProjectAccess(savedProject, project.getCreatedBy(), 
                                                     AccessRole.OWNER, project.getCreatedBy());
        projectAccessRepository.save(ownerAccess);
        
        return savedProject;
    }

    public Project updateProject(Long id, Project projectDetails) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));

        project.setName(projectDetails.getName());
        project.setDescription(projectDetails.getDescription());
        project.setStatus(projectDetails.getStatus());
        project.setStartDate(projectDetails.getStartDate());
        project.setEndDate(projectDetails.getEndDate());

        return projectRepository.save(project);
    }

    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
        projectRepository.delete(project);
    }

    public Long getProjectCountByStatus(ProjectStatus status) {
        return projectRepository.countByStatus(status);
    }

    public boolean hasUserAccess(Long projectId, Long userId) {
        return projectAccessRepository.existsByProjectIdAndUserId(projectId, userId);
    }

    public Optional<AccessRole> getUserRole(Long projectId, Long userId) {
        Optional<ProjectAccess> access = projectAccessRepository.findByProjectIdAndUserId(projectId, userId);
        return access.map(ProjectAccess::getRole);
    }
}
