package com.moyeo.service.meeting;

import java.time.LocalDateTime;
import java.util.List;

public record MeetingViewResult(
        Long meetingId,
        String name,
        String description,
        String planningType,
        String scheduleMode,
        String placeMode,
        String placeRecommendationStrategy,
        int maxParticipants,
        long participantCount,
        LocalDateTime deadlineAt,
        long remainingMinutes,
        long respondedParticipantCount,
        double responseRate,
        List<ParticipantStatus> participants
) {

    public record ParticipantStatus(
            Long participantId,
            String nickname,
            String participantType,
            boolean scheduleResponded,
            boolean placeResponded,
            boolean responseCompleted
    ) {
    }
}
