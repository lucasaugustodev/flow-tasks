package com.projectmanagement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {

    // Redirect root to Angular app
    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }

    // Handle Angular routes - forward to index.html for client-side routing
    @GetMapping(value = {
        "/login",
        "/register", 
        "/dashboard",
        "/projects",
        "/projects/**",
        "/kanban",
        "/profile",
        "/settings"
    })
    public String angular() {
        return "forward:/index.html";
    }
}
