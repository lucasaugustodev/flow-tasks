package com.projectmanagement.service;

import com.projectmanagement.model.MeetingMinute;
import com.projectmanagement.model.Project;
import com.projectmanagement.model.User;
import com.projectmanagement.repository.MeetingMinuteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MeetingMinuteService {

    @Autowired
    private MeetingMinuteRepository meetingMinuteRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public List<MeetingMinute> getAllMeetingMinutes() {
        return meetingMinuteRepository.findAll();
    }

    public Optional<MeetingMinute> getMeetingMinuteById(Long id) {
        return meetingMinuteRepository.findById(id);
    }

    public List<MeetingMinute> getMeetingMinutesByProject(Long projectId) {
        return meetingMinuteRepository.findByProjectIdOrderByMeetingDateDesc(projectId);
    }

    public List<MeetingMinute> searchMeetingMinutesByTitle(String title, Long projectId) {
        return meetingMinuteRepository.findByTitleContainingAndProjectId(title, projectId);
    }

    public MeetingMinute uploadMeetingMinute(String title, MultipartFile file, Project project, User uploadedBy) 
            throws IOException {
        
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        // Save file to disk
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Create MeetingMinute entity
        MeetingMinute meetingMinute = new MeetingMinute(
            title,
            originalFilename,
            filePath.toString(),
            file.getSize(),
            file.getContentType(),
            project,
            uploadedBy
        );

        return meetingMinuteRepository.save(meetingMinute);
    }

    public MeetingMinute updateMeetingMinute(Long id, MeetingMinute meetingMinuteDetails) {
        MeetingMinute meetingMinute = meetingMinuteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting minute not found with id: " + id));

        meetingMinute.setTitle(meetingMinuteDetails.getTitle());
        meetingMinute.setMeetingDate(meetingMinuteDetails.getMeetingDate());

        return meetingMinuteRepository.save(meetingMinute);
    }

    public void deleteMeetingMinute(Long id) throws IOException {
        MeetingMinute meetingMinute = meetingMinuteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting minute not found with id: " + id));

        // Delete file from disk
        if (meetingMinute.getFilePath() != null) {
            Path filePath = Paths.get(meetingMinute.getFilePath());
            Files.deleteIfExists(filePath);
        }

        meetingMinuteRepository.delete(meetingMinute);
    }

    public byte[] downloadMeetingMinute(Long id) throws IOException {
        MeetingMinute meetingMinute = meetingMinuteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting minute not found with id: " + id));

        Path filePath = Paths.get(meetingMinute.getFilePath());
        return Files.readAllBytes(filePath);
    }
}
