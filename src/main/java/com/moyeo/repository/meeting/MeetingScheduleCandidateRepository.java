package com.moyeo.repository.meeting;

import com.moyeo.domain.meeting.MeetingScheduleCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingScheduleCandidateRepository extends JpaRepository<MeetingScheduleCandidate, Long> {

    List<MeetingScheduleCandidate> findAllByMeetingIdOrderByCandidateDateAsc(Long meetingId);
}
