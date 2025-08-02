package com.projectmanagement.controller;

import com.projectmanagement.model.*;
import com.projectmanagement.security.UserPrincipal;
import com.projectmanagement.security.JwtUtils;
import com.projectmanagement.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

@Controller
public class WebController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectAccessService projectAccessService;

    @Autowired
    private MeetingMinuteService meetingMinuteService;

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        List<Project> userProjects = projectService.getProjectsByUser(userPrincipal.getId());
        List<Task> userTasks = taskService.getTasksByAssignedUserId(userPrincipal.getId());

        model.addAttribute("projects", userProjects);
        model.addAttribute("tasks", userTasks);
        model.addAttribute("user", userPrincipal);

        return "dashboard";
    }

    @GetMapping("/projects")
    public String projects(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        List<Project> userProjects = projectService.getProjectsByUser(userPrincipal.getId());

        model.addAttribute("projects", userProjects);
        model.addAttribute("user", userPrincipal);

        return "projects";
    }

    @GetMapping("/projects/{id}")
    public String projectDetails(@PathVariable Long id, Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // Check if user has access to this project
        if (!projectAccessService.hasAccess(id, userPrincipal.getId())) {
            return "redirect:/projects?error=access_denied";
        }

        Optional<Project> project = projectService.getProjectById(id);
        if (project.isEmpty()) {
            return "redirect:/projects?error=project_not_found";
        }

        List<Task> projectTasks = taskService.getTasksByProject(id);
        List<MeetingMinute> meetingMinutes = meetingMinuteService.getMeetingMinutesByProject(id);
        List<ProjectAccess> projectAccesses = projectAccessService.getProjectAccessesByProject(id);

        model.addAttribute("project", project.get());
        model.addAttribute("tasks", projectTasks);
        model.addAttribute("meetingMinutes", meetingMinutes);
        model.addAttribute("projectAccesses", projectAccesses);
        model.addAttribute("user", userPrincipal);

        return "project-details";
    }

    @GetMapping("/projects/{id}/kanban")
    public String kanban(@PathVariable Long id, Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // Check if user has access to this project
        if (!projectAccessService.hasAccess(id, userPrincipal.getId())) {
            return "redirect:/projects?error=access_denied";
        }

        Optional<Project> project = projectService.getProjectById(id);
        if (project.isEmpty()) {
            return "redirect:/projects?error=project_not_found";
        }

        List<Task> tasks = taskService.getTasksByProjectOrderedForKanban(id);

        model.addAttribute("project", project.get());
        model.addAttribute("tasks", tasks);
        model.addAttribute("user", userPrincipal);

        return "kanban";
    }

    @GetMapping("/kanban")
    public String allKanban(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        List<Task> userTasks = taskService.getTasksByAssignedUserId(userPrincipal.getId());

        model.addAttribute("tasks", userTasks);
        model.addAttribute("user", userPrincipal);

        return "kanban-all";
    }
}
