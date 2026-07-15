package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.ParticipantJoinResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모임 참여 응답")
public record ParticipantJoinResponse(
        @Schema(description = "참여한 모임 ID", example = "1")
        Long meetingId,

        @Schema(description = "서버에서 생성한 모임 참여자 ID", example = "2")
        Long participantId,

        @Schema(description = "모임 안에서 사용할 표시 닉네임", example = "member1")
        String nickname,

        @Schema(
                description = """
                        참여자 타입입니다.
                        <ul>
                          <li>HOST: 방장</li>
                          <li>MEMBER: 로그인 회원 참여자</li>
                          <li>GUEST: 게스트 참여자</li>
                        </ul>
                        """,
                example = "MEMBER",
                allowableValues = {"HOST", "MEMBER", "GUEST"}
        )
        String participantType
) {

    public static ParticipantJoinResponse from(ParticipantJoinResult result) {
        return new ParticipantJoinResponse(
                result.meetingId(),
                result.participantId(),
                result.nickname(),
                result.participantType()
        );
    }
}
