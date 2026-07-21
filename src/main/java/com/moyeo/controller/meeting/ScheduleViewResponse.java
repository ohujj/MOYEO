package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.ScheduleViewResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "일정 조율 현황 조회 응답")
public record ScheduleViewResponse(
        @Schema(description = "모임 ID", example = "1")
        Long meetingId,

        @Schema(description = "일정 참여 입력 유형입니다. 장소만 정하는 모임은 NONE입니다.", example = "DATE_AND_TIME", allowableValues = {"DATE_ONLY", "DATE_AND_TIME", "NONE"})
        String scheduleInputType,

        @Schema(description = "적용된 정렬 방식", example = "LONGEST_MEETING", allowableValues = {"LONGEST_MEETING", "EARLIEST_DATE"})
        String sort,

        @Schema(description = "현재 참여 인원. 방장을 포함합니다.", example = "4")
        long participantCount,

        @Schema(description = "일정 후보 목록. 적용된 정렬 방식에 따라 최대 3개를 반환합니다. 후보가 없으면 빈 배열입니다.")
        List<CandidateResponse> candidates
) {

    public static ScheduleViewResponse from(ScheduleViewResult result) {
        return new ScheduleViewResponse(
                result.meetingId(),
                result.scheduleInputType(),
                result.sort(),
                result.participantCount(),
                result.candidates().stream().map(CandidateResponse::from).toList()
        );
    }

    @Schema(description = "일정 후보")
    public record CandidateResponse(
            @Schema(description = "후보 날짜", example = "2026-07-12")
            LocalDate candidateDate,

            @Schema(description = "시작 시간. DATE_ONLY에서는 null입니다.", example = "18:00")
            LocalTime startTime,

            @Schema(description = "종료 시간. DATE_ONLY에서는 null입니다.", example = "19:00")
            LocalTime endTime,

            @Schema(description = "해당 날짜 또는 시간에 참여 가능한 인원 수", example = "3")
            long availableParticipantCount
    ) {

        private static CandidateResponse from(ScheduleViewResult.Candidate candidate) {
            return new CandidateResponse(
                    candidate.candidateDate(),
                    candidate.startTime(),
                    candidate.endTime(),
                    candidate.availableParticipantCount()
            );
        }
    }
}
