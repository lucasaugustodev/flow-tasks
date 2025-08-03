package com.projectmanagement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }

    // Angular routes - forward to Angular SPA
    @GetMapping(value = {"/login", "/register", "/dashboard", "/projects", "/projects/**", "/kanban", "/profile", "/settings"})
    public String angular() {
        return "forward:/index.html";
    }


}
