package com.moyeo.controller.room;

import com.moyeo.service.room.RoomCreateResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모임 생성 응답")
public record CreateRoomResponse(
        @Schema(description = "모임 ID", example = "1")
        Long roomId,

        @Schema(description = "모임 이름", example = "토요일 저녁 모임")
        String name,

        @Schema(description = "모임 설명", example = "오랜만에 같이 저녁 먹어요.")
        String description,

        @Schema(description = "최대 참여 인원. 방장 포함 기준입니다.", example = "6")
        Integer maxParticipants,

        @Schema(description = "초대 코드", example = "ABCD234567")
        String inviteCode,

        @Schema(description = "프론트에서 초대 링크를 만들 때 사용할 수 있는 경로", example = "/rooms/invitations/ABCD234567")
        String invitePath,

        @Schema(description = "방장 참여자 ID", example = "1")
        Long hostParticipantId
) {

    public static CreateRoomResponse from(RoomCreateResult result) {
        return new CreateRoomResponse(
                result.roomId(),
                result.name(),
                result.description(),
                result.maxParticipants(),
                result.inviteCode(),
                result.invitePath(),
                result.hostParticipantId()
        );
    }
}
