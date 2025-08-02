package com.projectmanagement;

import com.projectmanagement.model.User;
import com.projectmanagement.service.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ProjectManagementApplicationTests {

    @Autowired
    private UserService userService;

    @Test
    public void contextLoads() {
        // Test that the application context loads successfully
        assertNotNull(userService);
    }

    @Test
    public void testUserCreation() {
        User user = new User("testuser", "test@example.com", "password123", "Test User");
        User savedUser = userService.createUser(user);
        
        assertNotNull(savedUser);
        assertNotNull(savedUser.getId());
        assertEquals("testuser", savedUser.getUsername());
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("Test User", savedUser.getFullName());
        assertTrue(savedUser.getIsActive());
    }
}
