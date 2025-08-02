package com.projectmanagement.repository;

import com.projectmanagement.model.MeetingMinute;
import com.projectmanagement.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingMinuteRepository extends JpaRepository<MeetingMinute, Long> {
    
    List<MeetingMinute> findByProject(Project project);
    
    List<MeetingMinute> findByProjectId(Long projectId);
    
    @Query("SELECT m FROM MeetingMinute m WHERE m.project.id = :projectId ORDER BY m.meetingDate DESC, m.uploadedAt DESC")
    List<MeetingMinute> findByProjectIdOrderByMeetingDateDesc(@Param("projectId") Long projectId);
    
    @Query("SELECT m FROM MeetingMinute m WHERE m.title LIKE %:title% AND m.project.id = :projectId")
    List<MeetingMinute> findByTitleContainingAndProjectId(@Param("title") String title, @Param("projectId") Long projectId);
}
