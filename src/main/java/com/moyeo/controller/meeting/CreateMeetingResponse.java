package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.MeetingCreateResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모임 생성 성공 응답입니다. CRT-08 링크 공유 화면에 필요한 식별·초대 정보만 반환합니다. 모임 상세 정보는 초대 코드 조회 API로 조회합니다.")
public record CreateMeetingResponse(
        @Schema(description = "생성된 모임 식별자입니다.", example = "1")
        Long meetingId,

        @Schema(description = "초대 코드 조회 API와 공유 링크를 만들 때 사용하는 코드입니다.", example = "ABCD234567")
        String inviteCode,

        @Schema(description = "프론트가 공유 링크를 만들 때 사용하는 기존 상대 경로입니다. 현재 값은 변경하지 않습니다.", example = "/meetings/invitations/ABCD234567")
        String invitePath
) {

    public static CreateMeetingResponse from(MeetingCreateResult result) {
        return new CreateMeetingResponse(
                result.meetingId(),
                result.inviteCode(),
                result.invitePath()
        );
    }
}
