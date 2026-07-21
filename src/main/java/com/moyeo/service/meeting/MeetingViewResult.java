package com.moyeo.service.meeting;

import java.time.LocalDateTime;
import java.util.List;

public record MeetingViewResult(
        Long meetingId,
        String name,
        String description,
        String coverImageUrl,
        String planningType,
        String scheduleMode,
        String scheduleInputType,
        String placeMode,
        String placeRecommendationStrategy,
        int maxParticipants,
        long participantCount,
        LocalDateTime deadlineAt,
        long remainingMinutes,
        List<Participant> participants
) {

    public record Participant(
            Long participantId,
            String nickname,
            String participantType
    ) {
    }
}
