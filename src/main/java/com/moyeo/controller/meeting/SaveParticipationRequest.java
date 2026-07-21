package com.moyeo.controller.meeting;

import com.moyeo.domain.meeting.TransportationMode;
import com.moyeo.service.meeting.SaveParticipationCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = """
        INV-02 모임 참여 정보 저장 요청입니다.
        일정 조율 모임은 가능한 일정 슬롯을, 장소 조율 모임은 출발지와 이동수단을 저장합니다.
        이전에 저장한 일정 슬롯은 요청 값으로 전체 교체됩니다.
        주소 검색, GPS 현재 위치 찾기, 저장된 출발지 목록은 정책과 화면 협의가 더 필요한 영역이라 이 요청에는 포함하지 않고,
        클라이언트에서 선택이 완료된 출발지 스냅샷만 전달합니다.
        """)
public record SaveParticipationRequest(
        @Schema(description = "참여자의 일정 응답입니다. 일정 입력 유형에 맞는 필드 하나만 사용합니다.")
        @Valid ScheduleResponseRequest scheduleResponse,

        @Schema(description = "참여자 출발지와 이동수단입니다. 장소 조율 모임에서 필수입니다. 현재 위치 찾기나 저장된 출발지 선택은 협의 후 확장 예정입니다.")
        @Valid
        DepartureRequest departure
) {

    public SaveParticipationCommand toCommand() {
        return toCommand(scheduleResponse, departure);
    }

    public static SaveParticipationCommand toCommand(
            ScheduleResponseRequest scheduleResponse,
            DepartureRequest departure
    ) {
        return new SaveParticipationCommand(
                scheduleResponse != null && scheduleResponse.availableDates() != null
                        ? scheduleResponse.availableDates()
                        : List.of(),
                scheduleResponse != null && scheduleResponse.availableTimeRanges() != null
                        ? scheduleResponse.availableTimeRanges().stream().map(ScheduleAvailabilityRequest::toCommand).toList()
                        : List.of(),
                departure != null ? departure.toCommand() : null
        );
    }

    @Schema(description = "일정 참여 응답")
    public record ScheduleResponseRequest(
            @Schema(description = "날짜만 조율할 때 선택한 가능한 날짜 목록입니다.", example = "[\"2026-07-10\", \"2026-07-11\"]")
            List<@NotNull LocalDate> availableDates,

            @Schema(description = "날짜와 시간을 조율할 때 선택한 가능한 시간 범위 목록입니다.")
            List<@Valid ScheduleAvailabilityRequest> availableTimeRanges
    ) {
    }

    @Schema(description = "가능한 일정 슬롯")
    public record ScheduleAvailabilityRequest(
            @Schema(description = "모임장이 설정한 일정 후보 날짜 중 하나입니다.", example = "2026-07-10")
            @NotNull
            LocalDate candidateDate,

            @Schema(description = "가능 시간 시작입니다. 1시간 단위로 입력합니다.", example = "18:00")
            @NotNull
            LocalTime startTime,

            @Schema(description = "가능 시간 종료입니다. 1시간 단위로 입력합니다.", example = "19:00")
            @NotNull
            LocalTime endTime
    ) {

        private SaveParticipationCommand.ScheduleAvailability toCommand() {
            return new SaveParticipationCommand.ScheduleAvailability(candidateDate, startTime, endTime);
        }
    }

    @Schema(description = "참여자 출발지와 이동수단")
    public record DepartureRequest(
            @Schema(
                    description = "출발지 표시 이름입니다. 선택 입력(nullable)이며, 생략하면 조회 응답에서는 출발지 주소를 표시합니다.",
                    example = "회사",
                    maxLength = 30,
                    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                    nullable = true
            )
            @Size(max = 30)
            String name,

            @Schema(description = "출발지 주소입니다.", example = "서울 강남구 테헤란로 123", maxLength = 255)
            @NotBlank
            @Size(max = 255)
            String address,

            @Schema(description = "출발지 위도입니다. 검색 후보를 선택한 경우 응답 좌표를 경도와 함께 보냅니다.", example = "37.498095")
            @DecimalMin("-90.0")
            @DecimalMax("90.0")
            BigDecimal latitude,

            @Schema(description = "출발지 경도입니다. 검색 후보를 선택한 경우 응답 좌표를 위도와 함께 보냅니다.", example = "127.027610")
            @DecimalMin("-180.0")
            @DecimalMax("180.0")
            BigDecimal longitude,

            @Schema(
                    description = """
                            이동수단입니다.
                            <ul>
                              <li>PUBLIC_TRANSIT: 대중교통</li>
                              <li>CAR: 자동차</li>
                            </ul>
                            """,
                    example = "PUBLIC_TRANSIT",
                    allowableValues = {"PUBLIC_TRANSIT", "CAR"}
            )
            @NotNull
            TransportationMode transportationMode
    ) {

        @jakarta.validation.constraints.AssertTrue(message = "Latitude and longitude must be sent together.")
        @Schema(hidden = true)
        public boolean isValidCoordinatePair() {
            return (latitude == null) == (longitude == null);
        }

        private SaveParticipationCommand.Departure toCommand() {
            return new SaveParticipationCommand.Departure(name, address, latitude, longitude, transportationMode);
        }
    }
}
