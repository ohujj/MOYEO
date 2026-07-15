package com.moyeo.repository.meeting;

import com.moyeo.domain.meeting.MeetingParticipantScheduleAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MeetingParticipantScheduleAvailabilityRepository extends JpaRepository<MeetingParticipantScheduleAvailability, Long> {

    long countByParticipantId(Long participantId);

    @Query("""
            select availability
            from MeetingParticipantScheduleAvailability availability
            join fetch availability.participant participant
            join fetch availability.scheduleCandidate scheduleCandidate
            where participant.meeting.id = :meetingId
            """)
    List<MeetingParticipantScheduleAvailability> findAllByParticipantMeetingId(@Param("meetingId") Long meetingId);

    void deleteAllByParticipantId(Long participantId);
}
