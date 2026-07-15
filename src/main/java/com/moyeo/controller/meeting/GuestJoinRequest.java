package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.SaveParticipationCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = """
        게스트 모임 참여 요청입니다.
        게스트 참여자 생성과 참여 상세 정보 저장을 한 번에 처리합니다.
        일정 조율 모임은 scheduleAvailabilities를, 장소 조율 모임은 departure를 함께 입력해야 합니다.
        """)
public record GuestJoinRequest(
        @Schema(
                description = "모임 안에서 사용할 표시 닉네임입니다. 같은 모임 안에서는 중복될 수 없습니다.",
                example = "guest1",
                minLength = 1,
                maxLength = 30
        )
        @NotBlank
        @Size(min = 1, max = 30)
        String nickname,

        @Schema(
                description = """
                        게스트 참여 비밀번호입니다.
                        현재는 참여 정보에 해시로 저장하며, 게스트 재입장/수정 검증 정책은 아직 구현하지 않았습니다.
                        """,
                example = "guestpass123",
                minLength = 8,
                maxLength = 72
        )
        @NotBlank
        @Size(min = 8, max = 72)
        String password,

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
