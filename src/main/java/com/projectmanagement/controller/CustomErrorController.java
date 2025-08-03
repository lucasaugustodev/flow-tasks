package com.projectmanagement.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            
            // For 404 errors on non-API routes, redirect to Angular
            if (statusCode == 404) {
                String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
                
                // If it's not an API call, redirect to Angular
                if (requestUri != null && !requestUri.startsWith("/api/") && !requestUri.startsWith("/h2-console/")) {
                    return "forward:/index.html";
                }
            }
        }
        
        // For API errors or other errors, return error page
        return "error";
    }
}
