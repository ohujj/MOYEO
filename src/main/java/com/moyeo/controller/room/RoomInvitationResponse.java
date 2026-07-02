package com.moyeo.controller.room;

import com.moyeo.service.room.RoomInvitationResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "초대 코드 모임 조회 응답")
public record RoomInvitationResponse(
        @Schema(description = "서버에서 모임을 식별하는 ID", example = "1")
        Long roomId,

        @Schema(description = "초대 링크 진입 화면에 표시할 모임 이름", example = "토요일 저녁 모임")
        String name,

        @Schema(description = "초대 링크 진입 화면에 표시할 모임 설명. 입력하지 않은 경우 null일 수 있습니다.", example = "오랜만에 같이 저녁 먹어요.")
        String description,

        @Schema(description = "최대 참여 인원. 방장 포함 기준입니다.", example = "6")
        Integer maxParticipants,

        @Schema(
                description = """
                        모임 생성 유형.
                        <ul>
                          <li>SCHEDULE_ONLY: 일정만 정하기</li>
                          <li>PLACE_ONLY: 장소만 정하기</li>
                          <li>SCHEDULE_AND_PLACE: 일정과 장소 둘 다 정하기</li>
                        </ul>
                        """,
                example = "SCHEDULE_AND_PLACE",
                allowableValues = {"SCHEDULE_ONLY", "PLACE_ONLY", "SCHEDULE_AND_PLACE"}
        )
        String planningType,

        @Schema(
                description = """
                        일정 설정 방식.
                        <ul>
                          <li>VOTE: 일정 투표</li>
                          <li>FIXED: 일정 확정</li>
                          <li>NONE: 일정 미정/건너뛰기</li>
                        </ul>
                        """,
                example = "VOTE",
                allowableValues = {"VOTE", "FIXED", "NONE"}
        )
        String scheduleMode,

        @Schema(description = "확정 일정. scheduleMode=FIXED일 때만 값이 있고, VOTE/NONE에서는 null입니다.", example = "2026-07-04T19:00:00")
        LocalDateTime fixedScheduleAt,

        @Schema(description = "일정 후보 날짜 목록. scheduleMode=VOTE일 때 사용하며, FIXED/NONE에서는 빈 배열입니다.")
        List<LocalDate> scheduleCandidateDates,

        @Schema(description = "일정 조율 시작 시간. scheduleMode=VOTE일 때만 값이 있고, FIXED/NONE에서는 null입니다.", example = "18:00")
        LocalTime availableStartTime,

        @Schema(description = "일정 조율 종료 시간. scheduleMode=VOTE일 때만 값이 있고, FIXED/NONE에서는 null입니다.", example = "22:00")
        LocalTime availableEndTime,

        @Schema(
                description = """
                        장소 설정 방식.
                        <ul>
                          <li>FIXED: 장소 확정</li>
                          <li>RECOMMEND: 장소 추천/조율</li>
                          <li>NONE: 장소 미정/건너뛰기</li>
                        </ul>
                        """,
                example = "RECOMMEND",
                allowableValues = {"FIXED", "RECOMMEND", "NONE"}
        )
        String placeMode,

        @Schema(
                description = """
                        장소 추천 방식. placeMode=RECOMMEND일 때만 값이 있고, FIXED/NONE에서는 null입니다.
                        <ul>
                          <li>MIDDLE_POINT: 중간 지점 기반 추천</li>
                          <li>RANDOM: 랜덤 추천</li>
                        </ul>
                        """,
                example = "MIDDLE_POINT",
                allowableValues = {"MIDDLE_POINT", "RANDOM"}
        )
        String placeRecommendationStrategy,

        @Schema(description = "확정 장소 이름. placeMode=FIXED일 때만 값이 있고, RECOMMEND/NONE에서는 null입니다.", example = "강남역")
        String fixedPlaceName,

        @Schema(description = "확정 장소 주소. placeMode=FIXED일 때만 값이 있고, RECOMMEND/NONE에서는 null입니다.", example = "서울 강남구 강남대로 지하 396")
        String fixedPlaceAddress,

        @Schema(description = "서버가 계산한 모임 응답 마감 일시. 게스트 참여 가능 여부 판단에 사용합니다.", example = "2026-07-01T18:00:00")
        LocalDateTime deadlineAt,

        @Schema(description = "현재 참여 인원. 방장을 포함합니다.", example = "1")
        long participantCount,

        @Schema(description = "방장으로 표시할 닉네임", example = "moyeo1")
        String hostNickname
) {

    public static RoomInvitationResponse from(RoomInvitationResult result) {
        return new RoomInvitationResponse(
                result.roomId(),
                result.name(),
                result.description(),
                result.maxParticipants(),
                result.planningType(),
                result.scheduleMode(),
                result.fixedScheduleAt(),
                result.scheduleCandidateDates(),
                result.availableStartTime(),
                result.availableEndTime(),
                result.placeMode(),
                result.placeRecommendationStrategy(),
                result.fixedPlaceName(),
                result.fixedPlaceAddress(),
                result.deadlineAt(),
                result.participantCount(),
                result.hostNickname()
        );
    }
}
