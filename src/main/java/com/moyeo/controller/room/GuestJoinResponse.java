package com.moyeo.controller.room;

import com.moyeo.service.room.GuestJoinResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게스트 참여 응답")
public record GuestJoinResponse(
        @Schema(description = "모임 ID", example = "1")
        Long roomId,

        @Schema(description = "참여자 ID", example = "2")
        Long participantId,

        @Schema(description = "모임 안에서 사용할 표시 닉네임", example = "guest1")
        String nickname,

        @Schema(description = "참여자 타입", example = "GUEST")
        String participantType
) {

    public static GuestJoinResponse from(GuestJoinResult result) {
        return new GuestJoinResponse(
                result.roomId(),
                result.participantId(),
                result.nickname(),
                result.participantType()
        );
    }
}
