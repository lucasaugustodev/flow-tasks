package com.projectmanagement.controller;

import com.projectmanagement.model.Project;
import com.projectmanagement.model.User;
import com.projectmanagement.security.UserPrincipal;
import com.projectmanagement.service.ProjectService;
import com.projectmanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<Project>> getAllProjects(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        List<Project> projects = projectService.getProjectsByUser(userPrincipal.getId());
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(@PathVariable Long id, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // Check if user has access to this project
        if (!projectService.hasUserAccess(id, userPrincipal.getId())) {
            return ResponseEntity.status(403).build();
        }

        Optional<Project> project = projectService.getProjectById(id);
        return project.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Project> createProject(@Valid @RequestBody Project project, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        Optional<User> user = userService.getUserById(userPrincipal.getId());
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        project.setCreatedBy(user.get());
        Project savedProject = projectService.createProject(project);
        return ResponseEntity.ok(savedProject);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(@PathVariable Long id, @Valid @RequestBody Project projectDetails,
                                               Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // Check if user has admin access to this project
        Optional<com.projectmanagement.model.AccessRole> userRole = projectService.getUserRole(id, userPrincipal.getId());
        if (userRole.isEmpty() || (!userRole.get().equals(com.projectmanagement.model.AccessRole.OWNER) &&
                                   !userRole.get().equals(com.projectmanagement.model.AccessRole.ADMIN))) {
            return ResponseEntity.status(403).build();
        }

        try {
            Project updatedProject = projectService.updateProject(id, projectDetails);
            return ResponseEntity.ok(updatedProject);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Long id, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // Check if user is the owner of this project
        Optional<com.projectmanagement.model.AccessRole> userRole = projectService.getUserRole(id, userPrincipal.getId());
        if (userRole.isEmpty() || !userRole.get().equals(com.projectmanagement.model.AccessRole.OWNER)) {
            return ResponseEntity.status(403).build();
        }

        try {
            projectService.deleteProject(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Project>> searchProjects(@RequestParam String name, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        List<Project> projects = projectService.searchProjectsByName(name);
        
        // Filter projects that user has access to
        projects.removeIf(project -> !projectService.hasUserAccess(project.getId(), userPrincipal.getId()));
        
        return ResponseEntity.ok(projects);
    }
}
