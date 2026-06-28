package com.moyeo.controller.room;

import com.moyeo.service.room.RoomInvitationResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "초대 코드 모임 조회 응답")
public record RoomInvitationResponse(
        @Schema(description = "모임 ID", example = "1")
        Long roomId,

        @Schema(description = "모임 이름", example = "토요일 저녁 모임")
        String name,

        @Schema(description = "모임 설명", example = "오랜만에 같이 저녁 먹어요.")
        String description,

        @Schema(description = "최대 참여 인원. 방장 포함 기준입니다.", example = "6")
        Integer maxParticipants,

        @Schema(description = "현재 참여 인원", example = "1")
        long participantCount,

        @Schema(description = "방장 닉네임", example = "moyeo1")
        String hostNickname
) {

    public static RoomInvitationResponse from(RoomInvitationResult result) {
        return new RoomInvitationResponse(
                result.roomId(),
                result.name(),
                result.description(),
                result.maxParticipants(),
                result.participantCount(),
                result.hostNickname()
        );
    }
}
