package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.MeetingCoverResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모임 커버 이미지 수정 응답")
public record MeetingCoverResponse(
        @Schema(description = "교체 후 새로 저장된 커버 이미지를 <img src>에 표시할 상대 API 경로입니다. 기존 화면의 src를 이 값으로 교체하면 새 캐시 버전의 이미지를 받습니다.", example = "/api/meetings/invitations/ABCD234567/cover-image?v=15v9zq")
        String coverImageUrl
) {

    public static MeetingCoverResponse from(MeetingCoverResult result) {
        return new MeetingCoverResponse(result.coverImageUrl());
    }
}
