package com.moyeo.service.meeting;

import com.moyeo.domain.meeting.Meeting;
import com.moyeo.domain.meeting.MeetingScheduleCandidate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record MeetingInvitationResult(
        Long meetingId,
        String name,
        String description,
        Integer maxParticipants,
        String planningType,
        String scheduleMode,
        List<LocalDate> scheduleCandidateDates,
        LocalTime availableStartTime,
        LocalTime availableEndTime,
        String placeMode,
        String placeRecommendationStrategy,
        LocalDateTime deadlineAt,
        long participantCount,
        String hostNickname,
        ParticipationStatus participationStatus
) {

    public static MeetingInvitationResult from(
            Meeting meeting,
            long participantCount,
            List<MeetingScheduleCandidate> scheduleCandidates
    ) {
        ParticipationStatus participationStatus = ParticipationStatus.from(meeting, participantCount);
        return new MeetingInvitationResult(
                meeting.getId(),
                meeting.getName(),
                meeting.getDescription(),
                meeting.getMaxParticipants(),
                meeting.getPlanningType().name(),
                meeting.getScheduleMode().name(),
                scheduleCandidates.stream().map(MeetingScheduleCandidate::getCandidateDate).toList(),
                meeting.getAvailableStartTime(),
                meeting.getAvailableEndTime(),
                meeting.getPlaceMode().name(),
                meeting.getPlaceRecommendationStrategy() != null ? meeting.getPlaceRecommendationStrategy().name() : null,
                meeting.getDeadlineAt(),
                participantCount,
                meeting.getHostUser().getNickname(),
                participationStatus
        );
    }

    public record ParticipationStatus(
            boolean canJoin,
            String reason,
            String message
    ) {

        private static final String AVAILABLE = "AVAILABLE";
        private static final String DEADLINE_PASSED = "DEADLINE_PASSED";
        private static final String PARTICIPANT_LIMIT_EXCEEDED = "PARTICIPANT_LIMIT_EXCEEDED";
        private static final String DEADLINE_PASSED_MESSAGE = "\uAE30\uD55C\uC774 \uC9C0\uB09C \uBAA8\uC784\uC774\uC5D0\uC694. \uC544\uC27D\uC9C0\uB9CC \uD604\uC7AC\uB294 \uB354 \uC774\uC0C1 \uCC38\uC5EC\uD560 \uC218 \uC5C6\uC5B4\uC694.";
        private static final String PARTICIPANT_LIMIT_EXCEEDED_MESSAGE = "\uBAA8\uC778 \uC778\uC6D0\uC774 \uBAA8\uB450 \uCC3C\uC5B4\uC694. \uC544\uC27D\uC9C0\uB9CC \uD604\uC7AC\uB294 \uB354 \uC774\uC0C1 \uCC38\uC5EC\uD560 \uC218 \uC5C6\uC5B4\uC694.";

        private static ParticipationStatus from(Meeting meeting, long participantCount) {
            if (!meeting.getDeadlineAt().isAfter(LocalDateTime.now())) {
                return new ParticipationStatus(false, DEADLINE_PASSED, DEADLINE_PASSED_MESSAGE);
            }

            if (participantCount >= meeting.getMaxParticipants()) {
                return new ParticipationStatus(false, PARTICIPANT_LIMIT_EXCEEDED, PARTICIPANT_LIMIT_EXCEEDED_MESSAGE);
            }

            return new ParticipationStatus(true, AVAILABLE, null);
        }
    }
}
