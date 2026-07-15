package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.MeetingCreateResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "모임 생성 응답")
public record CreateMeetingResponse(
        @Schema(description = "서버에서 모임을 식별하는 ID", example = "1")
        Long meetingId,

        @Schema(description = "모임 카드와 초대 화면에 표시할 이름", example = "토요일 저녁 모임")
        String name,

        @Schema(description = "모임에 대한 간단한 설명. 입력하지 않은 경우 null입니다.", example = "오랜만에 같이 저녁 먹어요.")
        String description,

        @Schema(description = "최대 참여 인원. 방장을 포함합니다.", example = "6")
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
                        서버가 planningType 기준으로 파생한 일정 설정 방식.
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
                        서버가 planningType 기준으로 파생한 장소 설정 방식.
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
                        1차 MVP에서는 생성 후 변경하지 않으며, 추후 전환 기능을 검토할 수 있습니다.
                        생성 시점에는 추천 결과나 확정 장소를 만들지 않습니다.
                        <ul>
                          <li>MIDDLE_POINT: 참여자 출발지를 기준으로 나중에 중간지점 추천 진행</li>
                          <li>RANDOM: 나중에 랜덤 방식으로 장소 추천 진행</li>
                        </ul>
                        """,
                example = "MIDDLE_POINT",
                allowableValues = {"MIDDLE_POINT", "RANDOM"}
        )
        String placeRecommendationStrategy,

        @Schema(description = "서버가 계산한 모임 참여/응답 마감 일시", example = "2026-07-01T18:00:00")
        LocalDateTime deadlineAt,

        @Schema(description = "초대 코드 조회 API에서 사용하는 코드", example = "ABCD234567")
        String inviteCode,

        @Schema(description = "프론트에서 초대 링크를 만들 때 사용할 수 있는 경로", example = "/meetings/invitations/ABCD234567")
        String invitePath,

        @Schema(description = "방장 출발지 이름. 중간지점 추천을 선택한 생성 요청에서만 값이 있습니다.", example = "회사")
        String hostDepartureName,

        @Schema(description = "방장 출발지 주소. 중간지점 추천을 선택한 생성 요청에서만 값이 있습니다.", example = "서울 강남구 테헤란로 123")
        String hostDepartureAddress,

        @Schema(description = "방장 출발지 위도. 중간지점 추천을 선택한 생성 요청에서만 값이 있습니다.", example = "37.498095")
        java.math.BigDecimal hostDepartureLatitude,

        @Schema(description = "방장 출발지 경도. 중간지점 추천을 선택한 생성 요청에서만 값이 있습니다.", example = "127.027610")
        java.math.BigDecimal hostDepartureLongitude,

        @Schema(
                description = """
                        방장 이동수단. 중간지점 추천을 선택한 생성 요청에서만 값이 있습니다.
                        <ul>
                          <li>PUBLIC_TRANSIT: 대중교통</li>
                          <li>CAR: 자동차</li>
                        </ul>
                        """,
                example = "PUBLIC_TRANSIT",
                allowableValues = {"PUBLIC_TRANSIT", "CAR"}
        )
        String hostTransportationMode,

        @Schema(description = "방장을 MeetingParticipant로 식별하는 ID", example = "1")
        Long hostParticipantId
) {

    public static CreateMeetingResponse from(MeetingCreateResult result) {
        return new CreateMeetingResponse(
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
                result.inviteCode(),
                result.invitePath(),
                result.hostDepartureName(),
                result.hostDepartureAddress(),
                result.hostDepartureLatitude(),
                result.hostDepartureLongitude(),
                result.hostTransportationMode(),
                result.hostParticipantId()
        );
    }
}
