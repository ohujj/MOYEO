package com.moyeo.service.meeting;

import com.moyeo.domain.meeting.TransportationMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record SaveParticipationCommand(
        List<ScheduleAvailability> scheduleAvailabilities,
        Departure departure
) {

    public record ScheduleAvailability(
            LocalDate candidateDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
    }

    public record Departure(
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            TransportationMode transportationMode
    ) {
    }
}
