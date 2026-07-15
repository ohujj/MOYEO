package com.moyeo.repository.meeting;

import com.moyeo.domain.meeting.ParticipantType;
import com.moyeo.domain.meeting.Meeting;
import com.moyeo.domain.meeting.MeetingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

    long countByMeetingId(Long meetingId);

    boolean existsByMeetingAndNicknameAndParticipantType(Meeting meeting, String nickname, ParticipantType participantType);

    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);

    List<MeetingParticipant> findAllByMeetingIdOrderByIdAsc(Long meetingId);

    Optional<MeetingParticipant> findByIdAndMeetingId(Long id, Long meetingId);
}
