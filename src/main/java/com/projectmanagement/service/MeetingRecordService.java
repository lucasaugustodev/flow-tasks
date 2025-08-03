package com.projectmanagement.service;

import com.projectmanagement.model.MeetingRecord;
import com.projectmanagement.model.Project;
import com.projectmanagement.model.User;
import com.projectmanagement.repository.MeetingRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MeetingRecordService {

    @Autowired
    private MeetingRecordRepository meetingRecordRepository;

    @Autowired
    private ProjectService projectService;

    public List<MeetingRecord> getMeetingRecordsByProject(Long projectId) {
        return meetingRecordRepository.findByProjectIdOrderByMeetingDateDesc(projectId);
    }

    public Optional<MeetingRecord> getMeetingRecordById(Long id) {
        return meetingRecordRepository.findById(id);
    }

    public MeetingRecord createMeetingRecord(MeetingRecord meetingRecord, Long projectId, User createdBy) {
        Optional<Project> project = projectService.getProjectById(projectId);
        if (project.isEmpty()) {
            throw new RuntimeException("Project not found");
        }

        meetingRecord.setProject(project.get());
        meetingRecord.setCreatedBy(createdBy);
        
        return meetingRecordRepository.save(meetingRecord);
    }

    public MeetingRecord updateMeetingRecord(Long id, MeetingRecord updatedMeetingRecord, User user) {
        Optional<MeetingRecord> existingMeetingRecord = meetingRecordRepository.findById(id);
        if (existingMeetingRecord.isEmpty()) {
            throw new RuntimeException("Meeting record not found");
        }

        MeetingRecord meetingRecord = existingMeetingRecord.get();
        
        // Check if user has permission to update (creator or project owner)
        if (!meetingRecord.getCreatedBy().getId().equals(user.getId()) && 
            !meetingRecord.getProject().getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("User does not have permission to update this meeting record");
        }

        meetingRecord.setTitle(updatedMeetingRecord.getTitle());
        meetingRecord.setSummary(updatedMeetingRecord.getSummary());
        meetingRecord.setMeetingDate(updatedMeetingRecord.getMeetingDate());
        meetingRecord.setParticipants(updatedMeetingRecord.getParticipants());
        meetingRecord.setDecisions(updatedMeetingRecord.getDecisions());
        meetingRecord.setNextActions(updatedMeetingRecord.getNextActions());

        return meetingRecordRepository.save(meetingRecord);
    }

    public void deleteMeetingRecord(Long id, User user) {
        Optional<MeetingRecord> meetingRecord = meetingRecordRepository.findById(id);
        if (meetingRecord.isEmpty()) {
            throw new RuntimeException("Meeting record not found");
        }

        MeetingRecord record = meetingRecord.get();
        
        // Check if user has permission to delete (creator or project owner)
        if (!record.getCreatedBy().getId().equals(user.getId()) && 
            !record.getProject().getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("User does not have permission to delete this meeting record");
        }

        meetingRecordRepository.deleteById(id);
    }

    public Long countMeetingRecordsByProject(Long projectId) {
        return meetingRecordRepository.countByProjectId(projectId);
    }

    public boolean hasUserAccessToMeetingRecord(Long meetingRecordId, Long userId) {
        Optional<MeetingRecord> meetingRecord = meetingRecordRepository.findById(meetingRecordId);
        if (meetingRecord.isEmpty()) {
            return false;
        }

        // Check if user has access to the project
        return projectService.hasUserAccess(meetingRecord.get().getProject().getId(), userId);
    }
}
