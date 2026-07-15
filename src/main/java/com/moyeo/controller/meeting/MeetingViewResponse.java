package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.MeetingViewResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "모임 현황 조회 응답")
public record MeetingViewResponse(
        @Schema(description = "모임 ID", example = "1")
        Long meetingId,

        @Schema(description = "모임명", example = "주말 저녁 모임")
        String name,

        @Schema(description = "모임 설명. 입력하지 않은 경우 null입니다.", example = "오랜만에 같이 저녁 먹어요.")
        String description,

        @Schema(description = "모임 생성 유형", example = "SCHEDULE_AND_PLACE", allowableValues = {"SCHEDULE_ONLY", "PLACE_ONLY", "SCHEDULE_AND_PLACE"})
        String planningType,

        @Schema(description = "일정 설정 방식입니다. 현재 MVP에서는 일정 조율 모임이면 VOTE, 일정 조율이 없으면 NONE을 반환합니다.", example = "VOTE", allowableValues = {"VOTE", "NONE"})
        String scheduleMode,

        @Schema(description = "장소 설정 방식입니다. 현재 MVP에서는 장소 조율 모임이면 RECOMMEND, 장소 조율이 없으면 NONE을 반환합니다.", example = "RECOMMEND", allowableValues = {"RECOMMEND", "NONE"})
        String placeMode,

        @Schema(description = "장소 추천 방식입니다. placeMode가 RECOMMEND일 때만 반환하고, 그 외에는 null입니다.", example = "MIDDLE_POINT", allowableValues = {"MIDDLE_POINT", "RANDOM"})
        String placeRecommendationStrategy,

        @Schema(description = "최대 참여 인원. 방장을 포함합니다.", example = "6")
        int maxParticipants,

        @Schema(description = "현재 참여 인원. 방장을 포함합니다.", example = "3")
        long participantCount,

        @Schema(description = "모임 참여/응답 마감 일시", example = "2026-07-12T18:00:00")
        LocalDateTime deadlineAt,

        @Schema(description = "현재 서버 시간 기준 마감까지 남은 분. 이미 마감된 경우 0입니다.", example = "360")
        long remainingMinutes,

        @Schema(description = "필수 참여 정보를 모두 입력한 인원 수", example = "2")
        long respondedParticipantCount,

        @Schema(description = "응답 완료 비율. 0부터 1 사이의 소수입니다.", example = "0.6667")
        double responseRate,

        @Schema(description = "참여자 응답 상태 목록. 방장이 먼저 오고 이후 참여 순서로 정렬됩니다.")
        List<ParticipantStatusResponse> participants
) {

    public static MeetingViewResponse from(MeetingViewResult result) {
        return new MeetingViewResponse(
                result.meetingId(),
                result.name(),
                result.description(),
                result.planningType(),
                result.scheduleMode(),
                result.placeMode(),
                result.placeRecommendationStrategy(),
                result.maxParticipants(),
                result.participantCount(),
                result.deadlineAt(),
                result.remainingMinutes(),
                result.respondedParticipantCount(),
                result.responseRate(),
                result.participants().stream().map(ParticipantStatusResponse::from).toList()
        );
    }

    @Schema(description = "참여자 응답 상태")
    public record ParticipantStatusResponse(
            @Schema(description = "모임 참여자 ID", example = "1")
            Long participantId,

            @Schema(description = "모임 안에서 표시할 닉네임", example = "moyeo1")
            String nickname,

            @Schema(description = "참여자 유형", example = "HOST", allowableValues = {"HOST", "MEMBER", "GUEST"})
            String participantType,

            @Schema(description = "일정 조율 응답 여부입니다. scheduleMode가 NONE이면 항상 false입니다.", example = "true")
            boolean scheduleResponded,

            @Schema(description = "장소 조율 출발지 입력 여부입니다. placeMode가 NONE이면 항상 false입니다.", example = "true")
            boolean placeResponded,

            @Schema(description = "이 모임에서 필요한 참여 정보를 모두 입력했는지 여부", example = "true")
            boolean responseCompleted
    ) {

        private static ParticipantStatusResponse from(MeetingViewResult.ParticipantStatus status) {
            return new ParticipantStatusResponse(
                    status.participantId(),
                    status.nickname(),
                    status.participantType(),
                    status.scheduleResponded(),
                    status.placeResponded(),
                    status.responseCompleted()
            );
        }
    }
}
