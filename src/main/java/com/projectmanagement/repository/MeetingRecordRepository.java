package com.projectmanagement.repository;

import com.projectmanagement.model.MeetingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingRecordRepository extends JpaRepository<MeetingRecord, Long> {
    
    @Query("SELECT m FROM MeetingRecord m WHERE m.project.id = :projectId ORDER BY m.meetingDate DESC, m.createdAt DESC")
    List<MeetingRecord> findByProjectIdOrderByMeetingDateDesc(@Param("projectId") Long projectId);
    
    @Query("SELECT m FROM MeetingRecord m WHERE m.project.id = :projectId AND m.createdBy.id = :userId ORDER BY m.meetingDate DESC, m.createdAt DESC")
    List<MeetingRecord> findByProjectIdAndCreatedByIdOrderByMeetingDateDesc(@Param("projectId") Long projectId, @Param("userId") Long userId);
    
    @Query("SELECT COUNT(m) FROM MeetingRecord m WHERE m.project.id = :projectId")
    Long countByProjectId(@Param("projectId") Long projectId);
}
