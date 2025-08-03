package com.projectmanagement.controller;

import com.projectmanagement.model.MeetingRecord;
import com.projectmanagement.model.User;
import com.projectmanagement.security.UserPrincipal;
import com.projectmanagement.service.MeetingRecordService;
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
@RequestMapping("/api/projects/{projectId}/meetings")
public class MeetingRecordController {

    @Autowired
    private MeetingRecordService meetingRecordService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<MeetingRecord>> getMeetingRecordsByProject(
            @PathVariable Long projectId, 
            Authentication authentication) {
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // Check if user has access to this project
        if (!projectService.hasUserAccess(projectId, userPrincipal.getId())) {
            return ResponseEntity.status(403).build();
        }

        List<MeetingRecord> meetingRecords = meetingRecordService.getMeetingRecordsByProject(projectId);
        return ResponseEntity.ok(meetingRecords);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MeetingRecord> getMeetingRecordById(
            @PathVariable Long projectId,
            @PathVariable Long id, 
            Authentication authentication) {
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // Check if user has access to this meeting record
        if (!meetingRecordService.hasUserAccessToMeetingRecord(id, userPrincipal.getId())) {
            return ResponseEntity.status(403).build();
        }

        Optional<MeetingRecord> meetingRecord = meetingRecordService.getMeetingRecordById(id);
        return meetingRecord.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MeetingRecord> createMeetingRecord(
            @PathVariable Long projectId,
            @Valid @RequestBody MeetingRecord meetingRecord, 
            Authentication authentication) {
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // Check if user has access to this project
        if (!projectService.hasUserAccess(projectId, userPrincipal.getId())) {
            return ResponseEntity.status(403).build();
        }

        Optional<User> user = userService.getUserById(userPrincipal.getId());
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            MeetingRecord createdMeetingRecord = meetingRecordService.createMeetingRecord(
                meetingRecord, projectId, user.get());
            return ResponseEntity.ok(createdMeetingRecord);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<MeetingRecord> updateMeetingRecord(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @Valid @RequestBody MeetingRecord meetingRecord, 
            Authentication authentication) {
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        Optional<User> user = userService.getUserById(userPrincipal.getId());
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            MeetingRecord updatedMeetingRecord = meetingRecordService.updateMeetingRecord(
                id, meetingRecord, user.get());
            return ResponseEntity.ok(updatedMeetingRecord);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMeetingRecord(
            @PathVariable Long projectId,
            @PathVariable Long id, 
            Authentication authentication) {
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        Optional<User> user = userService.getUserById(userPrincipal.getId());
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            meetingRecordService.deleteMeetingRecord(id, user.get());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }
}
