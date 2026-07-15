package com.moyeo.controller.meeting;

import com.moyeo.domain.meeting.PlaceMode;
import com.moyeo.domain.meeting.PlaceRecommendationStrategy;
import com.moyeo.domain.meeting.PlanningType;
import com.moyeo.domain.meeting.ScheduleMode;
import com.moyeo.domain.meeting.TransportationMode;
import com.moyeo.service.meeting.CreateMeetingCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = """
        모임 생성 요청입니다. 선택한 생성 플로우에서 입력한 값을 마지막 링크 생성 시 한 번에 전송합니다.
        <ul>
          <li>SCHEDULE_ONLY: 기본 정보, 일정 후보, 마감 시간</li>
          <li>PLACE_ONLY: 기본 정보, 장소 추천 방식, 마감 시간</li>
          <li>SCHEDULE_AND_PLACE: 기본 정보, 일정 후보, 장소 추천 방식, 마감 시간</li>
        </ul>
        확정 일정/확정 장소 직접 입력은 이번 MVP 생성 플로우에서 제외하며, 추후 회의에서 재검토합니다.
        """)
public record CreateMeetingRequest(
        @Schema(description = "모임 카드와 초대 화면에 표시할 이름입니다.", example = "토요일 저녁 모임", minLength = 1, maxLength = 15)
        @NotBlank
        @Size(min = 1, max = 15)
        String name,

        @Schema(description = "모임에 대한 간단한 설명입니다. 선택 입력이며 비워둘 수 있습니다.", example = "오랜만에 같이 저녁 먹어요.", maxLength = 100)
        @Size(max = 100)
        String description,

        @Schema(description = "최대 참여 인원입니다. 방장을 포함합니다.", example = "6", minimum = "2", maximum = "20")
        @Min(2)
        @Max(20)
        int maxParticipants,

        @Schema(
                description = """
                        모임 생성 FAB에서 선택한 생성 유형입니다. 현재 생성 API는 이 값에 따라 서버가 scheduleMode/placeMode를 파생합니다.
                        <ul>
                          <li>SCHEDULE_ONLY: 일정만 정하기. 일정은 후보 날짜와 공통 시간대를 받는 조율(VOTE) 방식만 지원합니다.</li>
                          <li>PLACE_ONLY: 장소만 정하기. 장소는 추천(RECOMMEND) 대상으로 두고, MIDDLE_POINT 또는 RANDOM 중 하나를 선택합니다.</li>
                          <li>SCHEDULE_AND_PLACE: 일정과 장소 둘 다 정하기. 일정 조율(VOTE)과 장소 추천 방식(RECOMMEND)을 함께 저장합니다.</li>
                        </ul>
                        장소 추천 방식은 1차 MVP에서 생성 후 변경하지 않으며, 추후 전환 기능을 검토할 수 있습니다.
                        확정 일정/확정 장소 직접 입력(FIXED)은 이번 MVP 생성 요청에서 받지 않으며, 추후 회의에서 재검토합니다.
                        """,
                example = "SCHEDULE_AND_PLACE",
                allowableValues = {"SCHEDULE_ONLY", "PLACE_ONLY", "SCHEDULE_AND_PLACE"}
        )
        @NotNull
        PlanningType planningType,

        @Schema(description = "일정 후보 날짜 목록입니다. planningType이 일정 정하기를 포함할 때 필수입니다. 최신 3주 정책 확정 전까지 임시로 최대 21개를 허용합니다.", example = "[\"2026-07-04\", \"2026-07-05\"]")
        @Size(max = MeetingCreateConstraints.MAX_SCHEDULE_CANDIDATE_DATES)
        List<LocalDate> scheduleCandidateDates,

        @Schema(description = "모든 일정 후보 날짜에 공통으로 적용할 시작 시간입니다. planningType이 일정 정하기를 포함할 때 필수이며 1시간 단위로 입력합니다.", example = "18:00")
        LocalTime availableStartTime,

        @Schema(description = "모든 일정 후보 날짜에 공통으로 적용할 종료 시간입니다. planningType이 일정 정하기를 포함할 때 필수이며 시작 시간보다 뒤여야 하고 1시간 단위로 입력합니다.", example = "22:00")
        LocalTime availableEndTime,

        @Schema(
                description = """
                        생성 시 선택한 장소 추천 방식입니다. planningType이 장소 정하기를 포함할 때 필수입니다.
                        1차 MVP에서는 생성 후 변경하지 않으며, 추후 전환 기능을 검토할 수 있습니다.
                        생성 시점에는 추천 결과나 확정 장소를 만들지 않습니다.
                        <ul>
                          <li>MIDDLE_POINT: 참여자 출발지를 기준으로 나중에 중간지점 추천을 진행합니다.</li>
                          <li>RANDOM: 나중에 랜덤 방식으로 장소 추천을 진행합니다.</li>
                        </ul>
                        """,
                example = "MIDDLE_POINT",
                allowableValues = {"MIDDLE_POINT", "RANDOM"}
        )
        PlaceRecommendationStrategy placeRecommendationStrategy,

        @Schema(description = "방장 출발지 이름입니다. placeRecommendationStrategy가 MIDDLE_POINT일 때만 필수입니다. 생성 시 방장 참여자 정보에 스냅샷으로 저장합니다.", example = "회사", maxLength = 30)
        @Size(max = 30)
        String hostDepartureName,

        @Schema(description = "방장 출발지 주소입니다. placeRecommendationStrategy가 MIDDLE_POINT일 때만 필수입니다. 생성 시 방장 참여자 정보에 스냅샷으로 저장합니다.", example = "서울 강남구 테헤란로 123", maxLength = 255)
        @Size(max = 255)
        String hostDepartureAddress,

        @Schema(description = "방장 출발지 위도입니다. placeRecommendationStrategy가 MIDDLE_POINT일 때만 필수입니다.", example = "37.498095")
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        BigDecimal hostDepartureLatitude,

        @Schema(description = "방장 출발지 경도입니다. placeRecommendationStrategy가 MIDDLE_POINT일 때만 필수입니다.", example = "127.027610")
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        BigDecimal hostDepartureLongitude,

        @Schema(
                description = """
                        방장 이동수단입니다. placeRecommendationStrategy가 MIDDLE_POINT일 때만 필수입니다.
                        <ul>
                          <li>PUBLIC_TRANSIT: 대중교통</li>
                          <li>CAR: 자동차</li>
                        </ul>
                        """,
                example = "PUBLIC_TRANSIT",
                allowableValues = {"PUBLIC_TRANSIT", "CAR"}
        )
        TransportationMode hostTransportationMode,

        @Schema(description = "생성 요청을 서버가 처리하는 시점부터 마감까지 남은 시간(분)입니다. 서버가 deadlineAt을 계산합니다. 클라이언트의 예상 종료 시간은 미리보기이며, 사용자가 화면에 머무른 시간만큼 실제 저장값과 차이가 날 수 있습니다. 10분 단위, 최소 10분, 최대 72시간입니다.", example = "1440", minimum = "10", maximum = "4320")
        @Min(10)
        @Max(4320)
        int deadlineMinutes
) {
    @AssertTrue(message = "일정 정하기에는 후보 날짜와 공통 시간대가 필요합니다.")
    @Schema(hidden = true)
    public boolean isValidSchedulePlanning() {
        if (!requiresSchedule()) {
            return true;
        }
        return scheduleCandidateDates != null
                && !scheduleCandidateDates.isEmpty()
                && availableStartTime != null
                && availableEndTime != null
                && availableStartTime.isBefore(availableEndTime)
                && isHourUnit(availableStartTime)
                && isHourUnit(availableEndTime);
    }

    @AssertTrue(message = "장소 정하기에는 장소 추천 방식이 필요합니다.")
    @Schema(hidden = true)
    public boolean isValidPlacePlanning() {
        return !requiresPlace() || placeRecommendationStrategy != null;
    }

    @AssertTrue(message = "중간지점 추천에는 방장 출발지 이름, 주소, 좌표, 이동수단이 필요합니다.")
    @Schema(hidden = true)
    public boolean isValidHostDeparture() {
        if (!requiresPlace() || placeRecommendationStrategy != PlaceRecommendationStrategy.MIDDLE_POINT) {
            return true;
        }
        return hasText(hostDepartureName)
                && hasText(hostDepartureAddress)
                && hostDepartureLatitude != null
                && hostDepartureLongitude != null
                && hostTransportationMode != null;
    }

    @AssertTrue(message = "마감 시간은 10분 단위로 입력해야 합니다.")
    @Schema(hidden = true)
    public boolean isValidDeadlineUnit() {
        return deadlineMinutes % 10 == 0;
    }

    public CreateMeetingCommand toCommand() {
        return new CreateMeetingCommand(
                name,
                description,
                maxParticipants,
                planningType,
                resolveScheduleMode(),
                null,
                scheduleCandidateDates != null ? scheduleCandidateDates : List.of(),
                availableStartTime,
                availableEndTime,
                resolvePlaceMode(),
                placeRecommendationStrategy,
                null,
                null,
                hostDepartureName,
                hostDepartureAddress,
                hostDepartureLatitude,
                hostDepartureLongitude,
                hostTransportationMode,
                deadlineMinutes
        );
    }

    private ScheduleMode resolveScheduleMode() {
        return requiresSchedule() ? ScheduleMode.VOTE : ScheduleMode.NONE;
    }

    private PlaceMode resolvePlaceMode() {
        return requiresPlace() ? PlaceMode.RECOMMEND : PlaceMode.NONE;
    }

    private boolean requiresSchedule() {
        return planningType == PlanningType.SCHEDULE_ONLY || planningType == PlanningType.SCHEDULE_AND_PLACE;
    }

    private boolean requiresPlace() {
        return planningType == PlanningType.PLACE_ONLY || planningType == PlanningType.SCHEDULE_AND_PLACE;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isHourUnit(LocalTime time) {
        return time.getMinute() == 0 && time.getSecond() == 0 && time.getNano() == 0;
    }
}
