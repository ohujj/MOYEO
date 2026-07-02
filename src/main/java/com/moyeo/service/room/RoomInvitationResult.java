package com.moyeo.service.room;

import com.moyeo.domain.room.Room;
import com.moyeo.domain.room.RoomScheduleCandidate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record RoomInvitationResult(
        Long roomId,
        String name,
        String description,
        Integer maxParticipants,
        String planningType,
        String scheduleMode,
        LocalDateTime fixedScheduleAt,
        List<LocalDate> scheduleCandidateDates,
        LocalTime availableStartTime,
        LocalTime availableEndTime,
        String placeMode,
        String placeRecommendationStrategy,
        String fixedPlaceName,
        String fixedPlaceAddress,
        LocalDateTime deadlineAt,
        long participantCount,
        String hostNickname
) {

    public static RoomInvitationResult from(
            Room room,
            long participantCount,
            List<RoomScheduleCandidate> scheduleCandidates
    ) {
        return new RoomInvitationResult(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.getMaxParticipants(),
                room.getPlanningType().name(),
                room.getScheduleMode().name(),
                room.getFixedScheduleAt(),
                scheduleCandidates.stream().map(RoomScheduleCandidate::getCandidateDate).toList(),
                room.getAvailableStartTime(),
                room.getAvailableEndTime(),
                room.getPlaceMode().name(),
                room.getPlaceRecommendationStrategy() != null ? room.getPlaceRecommendationStrategy().name() : null,
                room.getFixedPlaceName(),
                room.getFixedPlaceAddress(),
                room.getDeadlineAt(),
                participantCount,
                room.getHostUser().getNickname()
        );
    }
}
