package com.moyeo.service.meeting;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ScheduleViewResult(
        Long meetingId,
        String sort,
        long participantCount,
        long respondedParticipantCount,
        List<Candidate> candidates,
        String emptyMessage
) {

    public record Candidate(
            LocalDate candidateDate,
            LocalTime startTime,
            LocalTime endTime,
            long availableParticipantCount,
            long totalParticipantCount
    ) {
    }
}
