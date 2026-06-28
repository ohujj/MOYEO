package com.moyeo.controller.room;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "모임 생성 요청")
public record CreateRoomRequest(
        @Schema(description = "모임 이름", example = "토요일 저녁 모임", minLength = 1, maxLength = 15)
        @NotBlank
        @Size(min = 1, max = 15)
        String name,

        @Schema(description = "모임 설명", example = "오랜만에 같이 저녁 먹어요.", maxLength = 100)
        @Size(max = 100)
        String description,

        @Schema(description = "최대 참여 인원. 방장 포함 기준입니다.", example = "6", minimum = "2", maximum = "20")
        @Min(2)
        @Max(20)
        int maxParticipants
) {
}
