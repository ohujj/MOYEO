package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.MeetingCreateResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모임과 방장 참여를 함께 저장한 성공 응답입니다.")
public record CreateMeetingResponse(
        @Schema(description = "생성된 모임 식별자입니다.", example = "1")
        Long meetingId,
        @Schema(description = "초대 코드입니다.", example = "ABCD234567")
        String inviteCode,
        @Schema(description = "공유 링크를 만들 때 사용하는 상대 경로입니다.", example = "/meetings/invitations/ABCD234567")
        String invitePath
) {

    public static CreateMeetingResponse from(MeetingCreateResult result) {
        return new CreateMeetingResponse(result.meetingId(), result.inviteCode(), result.invitePath());
    }
}
