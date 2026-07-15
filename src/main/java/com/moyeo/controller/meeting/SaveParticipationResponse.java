package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.SaveParticipationResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "INV-02 모임 참여 정보 저장 응답")
public record SaveParticipationResponse(
        @Schema(description = "참여한 모임 ID", example = "1")
        Long meetingId,

        @Schema(description = "참여자 ID", example = "2")
        Long participantId,

        @Schema(description = "저장된 가능한 일정 슬롯 수", example = "2")
        int scheduleAvailabilityCount,

        @Schema(description = "출발지 스냅샷 저장 여부", example = "true")
        boolean hasDeparture
) {

    public static SaveParticipationResponse from(SaveParticipationResult result) {
        return new SaveParticipationResponse(
                result.meetingId(),
                result.participantId(),
                result.scheduleAvailabilityCount(),
                result.hasDeparture()
        );
    }
}
