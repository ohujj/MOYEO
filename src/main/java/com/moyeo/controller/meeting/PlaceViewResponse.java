package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.PlaceViewResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "장소 조율 현황 조회 응답")
public record PlaceViewResponse(
        @Schema(description = "모임 ID", example = "1")
        Long meetingId,

        @Schema(description = "장소 추천 방식입니다. 장소 조율 모임이 아니면 null입니다.", example = "MIDDLE_POINT", allowableValues = {"MIDDLE_POINT", "RANDOM"})
        String placeRecommendationStrategy,

        @Schema(description = "추천 산출 방식입니다. MIDDLE_POINT는 STRAIGHT_LINE_PREVIEW, RANDOM은 RANDOM_CATALOG_PREVIEW를 반환하며 장소 조율 모임이 아니면 null입니다.", example = "STRAIGHT_LINE_PREVIEW", allowableValues = {"STRAIGHT_LINE_PREVIEW", "RANDOM_CATALOG_PREVIEW"})
        String recommendationBasis,

        @Schema(description = "참여자 출발지 좌표의 단순 평균 지점입니다. MIDDLE_POINT에서 출발지가 하나 이상 있을 때만 반환하고, RANDOM 또는 장소 조율 모임이 아니면 null입니다.")
        CoordinateResponse center,

        @Schema(description = "현재 참여 인원. 방장을 포함합니다.", example = "4")
        long participantCount,

        @Schema(description = "출발지를 입력한 인원 수", example = "3")
        long departureRespondedParticipantCount,

        @Schema(description = "참여자별 출발지 입력 상태")
        List<ParticipantDepartureStatusResponse> participants,

        @Schema(description = "추천 상권 목록. 최대 5개를 반환합니다.")
        List<RecommendationResponse> recommendations,

        @Schema(description = "추천 목록이 없을 때 표시할 문구. 추천 목록이 있으면 null입니다.", example = "추천할 장소가 없어요.")
        String emptyMessage
) {

    public static PlaceViewResponse from(PlaceViewResult result) {
        return new PlaceViewResponse(
                result.meetingId(),
                result.placeRecommendationStrategy(),
                result.recommendationBasis(),
                result.center() != null ? CoordinateResponse.from(result.center()) : null,
                result.participantCount(),
                result.departureRespondedParticipantCount(),
                result.participants().stream().map(ParticipantDepartureStatusResponse::from).toList(),
                result.recommendations().stream().map(RecommendationResponse::from).toList(),
                result.emptyMessage()
        );
    }

    @Schema(description = "좌표")
    public record CoordinateResponse(
            @Schema(description = "위도", example = "37.5344715")
            BigDecimal latitude,

            @Schema(description = "경도", example = "126.9726696")
            BigDecimal longitude
    ) {

        private static CoordinateResponse from(PlaceViewResult.Coordinate coordinate) {
            return new CoordinateResponse(coordinate.latitude(), coordinate.longitude());
        }
    }

    @Schema(description = "참여자 출발지 입력 상태")
    public record ParticipantDepartureStatusResponse(
            @Schema(description = "모임 참여자 ID", example = "1")
            Long participantId,

            @Schema(description = "모임 안에서 표시할 닉네임", example = "moyeo1")
            String nickname,

            @Schema(description = "참여자 유형", example = "HOST", allowableValues = {"HOST", "MEMBER", "GUEST"})
            String participantType,

            @Schema(description = "출발지 입력 여부", example = "true")
            boolean departureResponded,

            @Schema(description = "출발지 라벨. 입력하지 않았으면 null입니다.", example = "회사")
            String departureName,

            @Schema(description = "출발지 주소. 입력하지 않았으면 null입니다.", example = "서울 강남구 테헤란로 123")
            String departureAddress,

            @Schema(description = "이동 수단입니다. 입력하지 않았으면 null입니다.", example = "PUBLIC_TRANSIT", allowableValues = {"PUBLIC_TRANSIT", "CAR"})
            String transportationMode
    ) {

        private static ParticipantDepartureStatusResponse from(PlaceViewResult.ParticipantDepartureStatus status) {
            return new ParticipantDepartureStatusResponse(
                    status.participantId(),
                    status.nickname(),
                    status.participantType(),
                    status.departureResponded(),
                    status.departureName(),
                    status.departureAddress(),
                    status.transportationMode()
            );
        }
    }

    @Schema(description = "추천 상권")
    public record RecommendationResponse(
            @Schema(description = "추천 순위", example = "1")
            int rank,

            @Schema(description = "상권 코드", example = "1001491")
            String areaCode,

            @Schema(description = "상권명", example = "삼각지역")
            String areaName,

            @Schema(description = "상권 분류명", example = "관광특구")
            String categoryName,

            @Schema(description = "상권 중심 위도", example = "37.5344715")
            BigDecimal latitude,

            @Schema(description = "상권 중심 경도", example = "126.9726696")
            BigDecimal longitude,

            @Schema(description = "자치구명", example = "용산구")
            String guName,

            @Schema(description = "행정동명", example = "한강로동")
            String dongName,

            @Schema(description = "참여자 출발지에서 상권까지의 평균 직선거리 미터. 랜덤 추천이면 null입니다.", example = "3200")
            Long averageStraightDistanceMeters
    ) {

        private static RecommendationResponse from(PlaceViewResult.Recommendation recommendation) {
            return new RecommendationResponse(
                    recommendation.rank(),
                    recommendation.areaCode(),
                    recommendation.areaName(),
                    recommendation.categoryName(),
                    recommendation.latitude(),
                    recommendation.longitude(),
                    recommendation.guName(),
                    recommendation.dongName(),
                    recommendation.averageStraightDistanceMeters()
            );
        }
    }
}
