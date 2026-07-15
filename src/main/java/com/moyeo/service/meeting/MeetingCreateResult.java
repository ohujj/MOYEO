package com.moyeo.service.meeting;

import com.moyeo.domain.meeting.Meeting;
import com.moyeo.domain.meeting.MeetingParticipant;
import com.moyeo.domain.meeting.MeetingScheduleCandidate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.List;

public record MeetingCreateResult(
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
        String inviteCode,
        String invitePath,
        String hostDepartureName,
        String hostDepartureAddress,
        BigDecimal hostDepartureLatitude,
        BigDecimal hostDepartureLongitude,
        String hostTransportationMode,
        Long hostParticipantId
) {

    public static MeetingCreateResult from(
            Meeting meeting,
            MeetingParticipant hostParticipant,
            List<MeetingScheduleCandidate> scheduleCandidates
    ) {
        return new MeetingCreateResult(
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
                meeting.getInviteCode(),
                "/meetings/invitations/" + meeting.getInviteCode(),
                hostParticipant.getDepartureName(),
                hostParticipant.getDepartureAddress(),
                hostParticipant.getDepartureLatitude(),
                hostParticipant.getDepartureLongitude(),
                hostParticipant.getTransportationMode() != null ? hostParticipant.getTransportationMode().name() : null,
                hostParticipant.getId()
        );
    }
}
