package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.MeetingInvitationResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "초대 코드 모임 조회 응답")
public record MeetingInvitationResponse(
        @Schema(description = "서버에서 모임을 식별하는 ID", example = "1")
        Long meetingId,

        @Schema(description = "초대 링크 진입 화면에 표시할 모임 이름", example = "주말 저녁 모임")
        String name,

        @Schema(description = "초대 링크 진입 화면에 표시할 모임 설명. 입력하지 않은 경우 null입니다.", example = "오랜만에 같이 저녁 먹어요.")
        String description,

        @Schema(description = "최대 참여 인원. 방장을 포함합니다.", example = "6")
        Integer maxParticipants,

        @Schema(
                description = """
                        모임 생성 유형입니다.
                        <ul>
                          <li>SCHEDULE_ONLY: 일정만 정하기</li>
                          <li>PLACE_ONLY: 장소만 정하기</li>
                          <li>SCHEDULE_AND_PLACE: 일정과 장소 모두 정하기</li>
                        </ul>
                        """,
                example = "SCHEDULE_AND_PLACE",
                allowableValues = {"SCHEDULE_ONLY", "PLACE_ONLY", "SCHEDULE_AND_PLACE"}
        )
        String planningType,

        @Schema(
                description = """
                        일정 설정 방식입니다.
                        <ul>
                          <li>VOTE: 일정 조율</li>
                          <li>NONE: 일정 없음</li>
                        </ul>
                        현재 MVP 생성 API에서는 FIXED 직접 입력을 받지 않습니다.
                        """,
                example = "VOTE",
                allowableValues = {"VOTE", "NONE"}
        )
        String scheduleMode,

        @Schema(description = "일정 후보 날짜 목록. scheduleMode=VOTE일 때 사용하며, scheduleMode=NONE이면 빈 배열입니다.")
        List<LocalDate> scheduleCandidateDates,

        @Schema(description = "일정 조율 시작 시간. scheduleMode=VOTE일 때만 값이 있습니다.", example = "18:00")
        LocalTime availableStartTime,

        @Schema(description = "일정 조율 종료 시간. scheduleMode=VOTE일 때만 값이 있습니다.", example = "22:00")
        LocalTime availableEndTime,

        @Schema(
                description = """
                        장소 설정 방식입니다.
                        <ul>
                          <li>RECOMMEND: 장소 추천/확정 플로우에서 장소를 정함</li>
                          <li>NONE: 장소 없음</li>
                        </ul>
                        현재 MVP 생성 API에서는 FIXED 직접 입력을 받지 않습니다.
                        """,
                example = "RECOMMEND",
                allowableValues = {"RECOMMEND", "NONE"}
        )
        String placeMode,

        @Schema(
                description = """
                        생성 시 선택한 장소 추천 방식입니다. placeMode=RECOMMEND일 때만 값이 있습니다.
                        1차 MVP에서는 생성 후 변경하지 않으며, 생성 시점에는 추천 결과나 확정 장소를 만들지 않습니다.
                        <ul>
                          <li>MIDDLE_POINT: 참여자 출발지를 기준으로 추후 중간지점 추천 진행</li>
                          <li>RANDOM: 추후 랜덤 방식으로 장소 추천 진행</li>
                        </ul>
                        """,
                example = "MIDDLE_POINT",
                allowableValues = {"MIDDLE_POINT", "RANDOM"}
        )
        String placeRecommendationStrategy,

        @Schema(description = "서버가 계산한 모임 참여/응답 마감 일시", example = "2026-07-01T18:00:00")
        LocalDateTime deadlineAt,

        @Schema(description = "현재 참여 인원. 방장을 포함합니다.", example = "1")
        long participantCount,

        @Schema(description = "방장으로 표시할 닉네임", example = "moyeo1")
        String hostNickname,

        @Schema(description = "초대 링크 진입 시점의 참여 가능 상태. 참여하기 버튼 활성화와 안내 문구에 사용합니다.")
        ParticipationStatusResponse participationStatus
) {

    public static MeetingInvitationResponse from(MeetingInvitationResult result) {
        return new MeetingInvitationResponse(
                result.meetingId(),
                result.name(),
                result.description(),
                result.maxParticipants(),
                result.planningType(),
                result.scheduleMode(),
                result.scheduleCandidateDates(),
                result.availableStartTime(),
                result.availableEndTime(),
                result.placeMode(),
                result.placeRecommendationStrategy(),
                result.deadlineAt(),
                result.participantCount(),
                result.hostNickname(),
                ParticipationStatusResponse.from(result.participationStatus())
        );
    }

    @Schema(description = "초대 진입 참여 가능 상태")
    public record ParticipationStatusResponse(
            @Schema(description = "현재 참여하기 버튼을 활성화할 수 있는지 여부", example = "true")
            boolean canJoin,

            @Schema(
                    description = """
                            참여 가능/불가 사유 코드입니다.
                            <ul>
                              <li>AVAILABLE: 참여 가능</li>
                              <li>DEADLINE_PASSED: 참여 기한 지남</li>
                              <li>PARTICIPANT_LIMIT_EXCEEDED: 정원 초과</li>
                            </ul>
                            기한과 정원이 모두 막힌 경우 기한 초과를 우선 반환합니다.
                            """,
                    example = "AVAILABLE",
                    allowableValues = {"AVAILABLE", "DEADLINE_PASSED", "PARTICIPANT_LIMIT_EXCEEDED"}
            )
            String reason,

            @Schema(description = "참여 불가 시 화면에 표시할 안내 문구. 참여 가능하면 null입니다.", example = "기한이 지난 모임이에요. 아쉽지만 현재는 더 이상 참여할 수 없어요.")
            String message
    ) {

        private static ParticipationStatusResponse from(MeetingInvitationResult.ParticipationStatus status) {
            return new ParticipationStatusResponse(status.canJoin(), status.reason(), status.message());
        }
    }
}
