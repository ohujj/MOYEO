package com.moyeo.service.meeting;

import java.math.BigDecimal;
import java.util.List;

public record PlaceViewResult(
        Long meetingId,
        String placeRecommendationStrategy,
        String recommendationBasis,
        Coordinate center,
        long participantCount,
        long departureRespondedParticipantCount,
        List<ParticipantDepartureStatus> participants,
        List<Recommendation> recommendations,
        String emptyMessage
) {

    public record Coordinate(
            BigDecimal latitude,
            BigDecimal longitude
    ) {
    }

    public record ParticipantDepartureStatus(
            Long participantId,
            String nickname,
            String participantType,
            boolean departureResponded,
            String departureName,
            String departureAddress,
            String transportationMode
    ) {
    }

    public record Recommendation(
            int rank,
            String areaCode,
            String areaName,
            String categoryName,
            BigDecimal latitude,
            BigDecimal longitude,
            String guName,
            String dongName,
            Long averageStraightDistanceMeters
    ) {
    }
}
