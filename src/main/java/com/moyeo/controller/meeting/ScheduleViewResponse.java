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

        @Schema(description = "적용된 정렬 방식", example = "LONGEST_MEETING", allowableValues = {"LONGEST_MEETING", "EARLIEST_DATE"})
        String sort,

        @Schema(description = "현재 참여 인원. 방장을 포함합니다.", example = "4")
        long participantCount,

        @Schema(description = "일정 가능 시간을 1개 이상 입력한 인원 수", example = "3")
        long respondedParticipantCount,

        @Schema(description = "일정 후보 목록. 최대 5개를 반환합니다.")
        List<CandidateResponse> candidates,

        @Schema(description = "후보가 없을 때 표시할 문구. 후보가 있으면 null입니다.", example = "겹치는 시간이 없어요.")
        String emptyMessage
) {

    public static ScheduleViewResponse from(ScheduleViewResult result) {
        return new ScheduleViewResponse(
                result.meetingId(),
                result.sort(),
                result.participantCount(),
                result.respondedParticipantCount(),
                result.candidates().stream().map(CandidateResponse::from).toList(),
                result.emptyMessage()
        );
    }

    @Schema(description = "일정 후보")
    public record CandidateResponse(
            @Schema(description = "후보 날짜", example = "2026-07-12")
            LocalDate candidateDate,

            @Schema(description = "시작 시간", example = "18:00")
            LocalTime startTime,

            @Schema(description = "종료 시간", example = "19:00")
            LocalTime endTime,

            @Schema(description = "해당 시간에 참여 가능한 인원 수", example = "3")
            long availableParticipantCount,

            @Schema(description = "전체 참여 인원 수", example = "4")
            long totalParticipantCount
    ) {

        private static CandidateResponse from(ScheduleViewResult.Candidate candidate) {
            return new CandidateResponse(
                    candidate.candidateDate(),
                    candidate.startTime(),
                    candidate.endTime(),
                    candidate.availableParticipantCount(),
                    candidate.totalParticipantCount()
            );
        }
    }
}
