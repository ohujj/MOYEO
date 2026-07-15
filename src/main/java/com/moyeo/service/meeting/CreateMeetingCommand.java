package com.moyeo.service.meeting;

import com.moyeo.domain.meeting.PlaceMode;
import com.moyeo.domain.meeting.PlaceRecommendationStrategy;
import com.moyeo.domain.meeting.PlanningType;
import com.moyeo.domain.meeting.ScheduleMode;
import com.moyeo.domain.meeting.TransportationMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record CreateMeetingCommand(
        String name,
        String description,
        int maxParticipants,
        PlanningType planningType,
        ScheduleMode scheduleMode,
        LocalDateTime fixedScheduleAt,
        List<LocalDate> scheduleCandidateDates,
        LocalTime availableStartTime,
        LocalTime availableEndTime,
        PlaceMode placeMode,
        PlaceRecommendationStrategy placeRecommendationStrategy,
        String fixedPlaceName,
        String fixedPlaceAddress,
        String hostDepartureName,
        String hostDepartureAddress,
        BigDecimal hostDepartureLatitude,
        BigDecimal hostDepartureLongitude,
        TransportationMode hostTransportationMode,
        int deadlineMinutes
) {
}
