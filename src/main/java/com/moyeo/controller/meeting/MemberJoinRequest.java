package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.SaveParticipationCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "로그인 회원 참여자 생성과 참여 상세 정보 저장 요청")
public record MemberJoinRequest(
        @Schema(
                description = "모임 안에서 사용할 표시 닉네임입니다. 회원 기본 닉네임과 다르게 입력할 수 있습니다.",
                example = "member1",
                minLength = 1,
                maxLength = 30
        )
        @NotBlank
        @Size(min = 1, max = 30)
        String nickname,

        @Schema(description = "참여자가 선택한 가능한 일정 슬롯 목록입니다. 일정 조율 모임에서 필수입니다.")
        @Valid List<SaveParticipationRequest.ScheduleAvailabilityRequest> scheduleAvailabilities,

        @Schema(description = "참여자 출발지와 이동수단입니다. 장소 조율 모임에서 필수입니다.")
        @Valid
        SaveParticipationRequest.DepartureRequest departure
) {

    public SaveParticipationCommand toParticipationCommand() {
        return SaveParticipationRequest.toCommand(scheduleAvailabilities, departure);
    }
}
