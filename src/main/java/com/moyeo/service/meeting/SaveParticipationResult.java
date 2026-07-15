package com.moyeo.service.meeting;

public record SaveParticipationResult(
        Long meetingId,
        Long participantId,
        int scheduleAvailabilityCount,
        boolean hasDeparture
) {
}
