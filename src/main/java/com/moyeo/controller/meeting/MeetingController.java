package com.moyeo.controller.meeting;

import com.moyeo.global.security.CurrentMember;
import com.moyeo.service.member.AuthenticatedMember;
import com.moyeo.service.meeting.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/meetings")
@Tag(name = "Meeting", description = "모임 생성, 초대 코드 조회, 게스트 참여 API")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Hidden
    @Operation(
            summary = "모임 생성",
            description = """
                    로그인한 사용자가 방장이 되어 모임을 생성하고 초대 코드를 발급받습니다.<br>
                    클라이언트는 FAB에서 선택한 생성 플로우를 완료한 뒤 마지막 링크 생성 시 한 번에 요청합니다.
                    <ul>
                      <li>SCHEDULE_ONLY: SCR-06 기본 정보 -> SCR-07 일정 정하기 -> SCR-09 마감 시간 -> SCR-10 완료. 일정 조율(VOTE)만 사용합니다.</li>
                      <li>PLACE_ONLY: SCR-06 기본 정보 -> SCR-08 장소 정하기 -> SCR-09 마감 시간 -> SCR-10 완료. 장소 추천 방식으로 MIDDLE_POINT 또는 RANDOM을 선택합니다.</li>
                      <li>SCHEDULE_AND_PLACE: SCR-06 기본 정보 -> SCR-07 일정 정하기 -> SCR-08 장소 정하기 -> SCR-09 마감 시간 -> SCR-10 완료. 일정 조율(VOTE)과 장소 추천 방식(RECOMMEND)을 함께 저장합니다.</li>
                    </ul>
                    scheduleMode와 placeMode는 서버가 planningType 기준으로 파생합니다.
                    placeRecommendationStrategy는 1차 MVP에서 생성 후 변경하지 않으며, 추후 전환 기능을 검토할 수 있습니다.
                    확정 일정/확정 장소 직접 입력 플로우는 이번 MVP 생성 범위에서 제외하며, 추후 회의에서 재검토합니다.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "SCHEDULE_AND_PLACE",
                                            description = "일정 조율(VOTE)과 장소 추천 방식(RECOMMEND)을 함께 저장하는 생성 요청입니다. 생성 성공 응답의 inviteCode를 복사해 초대 코드 조회와 회원/게스트 참여 API의 {inviteCode}에 넣으세요. 아래 후보 날짜와 시간 범위는 참여 API의 예시와 연결됩니다. 장소 추천 방식은 MIDDLE_POINT 또는 RANDOM 중 선택합니다. 1차 MVP에서는 생성 후 변경하지 않으며, 생성 시점에는 추천 결과나 확정 장소를 만들지 않습니다. MIDDLE_POINT를 선택하면 방장 출발지 이름, 주소, 이동수단을 저장합니다. 좌표 제공 API 승인 전까지 위도와 경도는 함께 생략할 수 있습니다.",
                                            value = """
                                                    {
                                                      "name": "토요일 저녁 모임",
                                                      "description": "오랜만에 같이 저녁 먹어요.",
                                                      "maxParticipants": 6,
                                                      "planningType": "SCHEDULE_AND_PLACE",
                                                      "scheduleCandidateDates": [
                                                        "2026-07-10",
                                                        "2026-07-11",
                                                        "2026-07-12",
                                                        "2026-07-13",
                                                        "2026-07-14"
                                                      ],
                                                      "availableStartTime": "17:00",
                                                      "availableEndTime": "23:00",
                                                      "placeRecommendationStrategy": "MIDDLE_POINT",
                                                      "hostDepartureName": "회사",
                                                      "hostDepartureAddress": "서울 강남구 테헤란로 123",
                                                      "hostDepartureLatitude": 37.498095,
                                                      "hostDepartureLongitude": 127.027610,
                                                      "hostTransportationMode": "PUBLIC_TRANSIT",
                                                      "deadlineMinutes": 1440
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "SCHEDULE_ONLY",
                                            description = "일정만 정하는 생성 요청입니다. 현재는 후보 날짜와 공통 시간대를 받는 일정 조율(VOTE) 방식만 지원하며, 장소 입력이나 장소 추천 값은 받지 않습니다.",
                                            value = """
                                                    {
                                                      "name": "저녁 일정 정하기",
                                                      "description": "먼저 가능한 날짜를 정해요.",
                                                      "maxParticipants": 4,
                                                      "planningType": "SCHEDULE_ONLY",
                                                      "scheduleCandidateDates": [
                                                        "2026-07-10",
                                                        "2026-07-11",
                                                        "2026-07-12",
                                                        "2026-07-13",
                                                        "2026-07-14"
                                                      ],
                                                      "availableStartTime": "17:00",
                                                      "availableEndTime": "23:00",
                                                      "deadlineMinutes": 180
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "PLACE_ONLY",
                                            description = "장소만 정하는 생성 요청입니다. 현재는 장소 추천 방식(RECOMMEND)으로 MIDDLE_POINT 또는 RANDOM 중 하나를 선택합니다. 1차 MVP에서는 생성 후 변경하지 않으며, 생성 시점에는 추천 결과나 확정 장소를 만들지 않습니다. 일정 후보와 시간대는 받지 않습니다.",
                                            value = """
                                                    {
                                                      "name": "카페 장소 정하기",
                                                      "description": "먼저 갈 장소를 정해요.",
                                                      "maxParticipants": 8,
                                                      "planningType": "PLACE_ONLY",
                                                      "placeRecommendationStrategy": "RANDOM",
                                                      "deadlineMinutes": 720
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "모임 생성 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "COMMON_VALIDATION_FAILED",
                              "status": 400
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token 없음, 만료 또는 유효하지 않음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "AUTHENTICATION_REQUIRED",
                              "status": 401
                            }
                            """))
            )
    })
    public CreateMeetingResponse createMeeting(
            @Parameter(hidden = true) @CurrentMember AuthenticatedMember member,
            @Valid @RequestBody CreateMeetingRequest request
    ) {
        return CreateMeetingResponse.from(meetingService.createMeeting(member, request.toCommand()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "커버 사진을 포함한 모임 생성",
            description = """
                    커버 사진은 선택 사항입니다. 사진이 없어도 이 multipart API로 모임을 생성할 수 있습니다.
                    이때 `coverImage` 파트만 생략합니다. `request`는 문자열이 아니라
                    `application/json` 타입의 Blob 파트여야 합니다.

                    `FormData` 요청에서 `Content-Type` 헤더는 직접 설정하지 마세요. 브라우저 또는 curl이
                    multipart boundary를 자동으로 추가합니다.

                    ```javascript
                    const formData = new FormData();
                    formData.append(
                      "request",
                      new Blob([JSON.stringify(requestBody)], { type: "application/json" })
                    );
                    if (coverImageFile) {
                      formData.append("coverImage", coverImageFile);
                    }

                    await fetch("/api/meetings", {
                      method: "POST",
                      headers: { Authorization: `Bearer ${accessToken}` },
                      body: formData
                    });
                    ```
                    """
            , requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    examples = {
                                            @ExampleObject(
                                                    name = "SCHEDULE_AND_PLACE",
                                                    description = """
                                                            **허용 `planningType`**
                                                            - `SCHEDULE_ONLY`
                                                            - `PLACE_ONLY`
                                                            - `SCHEDULE_AND_PLACE` ← 이 예시

                                                            **필수 입력**
                                                            - 일정: `scheduleCandidateDates`, `availableStartTime`, `availableEndTime`
                                                            - 장소 전략: `placeRecommendationStrategy` (`MIDDLE_POINT` 또는 `RANDOM`)
                                                            - `MIDDLE_POINT` 선택 시: `hostDepartureName`, `hostDepartureAddress`, `hostTransportationMode` (`PUBLIC_TRANSIT` 또는 `CAR`)

                                                            `scheduleMode`와 `placeMode`는 보내지 않습니다. 서버가 각각 `VOTE`, `RECOMMEND`로 설정합니다.
                                                            """,
                                                    value = """
                                                    {
                                                      "name": "토요일 저녁 모임",
                                                      "description": "오랜만에 같이 저녁 먹어요.",
                                                      "maxParticipants": 6,
                                                      "planningType": "SCHEDULE_AND_PLACE",
                                                      "scheduleCandidateDates": ["2026-07-10", "2026-07-11"],
                                                      "availableStartTime": "17:00",
                                                      "availableEndTime": "23:00",
                                                      "placeRecommendationStrategy": "MIDDLE_POINT",
                                                      "hostDepartureName": "회사",
                                                      "hostDepartureAddress": "서울 강남구 테헤란로 123",
                                                      "hostTransportationMode": "PUBLIC_TRANSIT",
                                                      "deadlineMinutes": 1440
                                                    }
                                                    """
                                            ),
                                            @ExampleObject(
                                                    name = "SCHEDULE_ONLY",
                                                    description = """
                                                            **허용 `planningType`**
                                                            - `SCHEDULE_ONLY` ← 이 예시
                                                            - `PLACE_ONLY`
                                                            - `SCHEDULE_AND_PLACE`

                                                            **필수 입력**
                                                            - `scheduleCandidateDates`
                                                            - `availableStartTime`
                                                            - `availableEndTime`

                                                            `placeRecommendationStrategy`와 출발지 필드는 보내지 않습니다.
                                                            `scheduleMode`와 `placeMode`는 서버가 각각 `VOTE`, `NONE`으로 설정합니다.
                                                            """,
                                                    value = """
                                                    {
                                                      "name": "주말 일정 정하기",
                                                      "description": "가능한 날짜를 정해요.",
                                                      "maxParticipants": 4,
                                                      "planningType": "SCHEDULE_ONLY",
                                                      "scheduleCandidateDates": ["2026-07-10", "2026-07-11"],
                                                      "availableStartTime": "17:00",
                                                      "availableEndTime": "23:00",
                                                      "deadlineMinutes": 180
                                                    }
                                                    """
                                            ),
                                            @ExampleObject(
                                                    name = "PLACE_ONLY",
                                                    description = """
                                                            **허용 `planningType`**
                                                            - `SCHEDULE_ONLY`
                                                            - `PLACE_ONLY` ← 이 예시
                                                            - `SCHEDULE_AND_PLACE`

                                                            **필수 입력**
                                                            - `placeRecommendationStrategy`: `MIDDLE_POINT` 또는 `RANDOM`
                                                            - `MIDDLE_POINT` 선택 시: `hostDepartureName`, `hostDepartureAddress`, `hostTransportationMode` (`PUBLIC_TRANSIT` 또는 `CAR`)

                                                            일정 필드는 보내지 않습니다.
                                                            `scheduleMode`와 `placeMode`는 서버가 각각 `NONE`, `RECOMMEND`로 설정합니다.
                                                            """,
                                                    value = """
                                                    {
                                                      "name": "카페 장소 정하기",
                                                      "description": "만날 장소를 정해요.",
                                                      "maxParticipants": 8,
                                                      "planningType": "PLACE_ONLY",
                                                      "placeRecommendationStrategy": "RANDOM",
                                                      "deadlineMinutes": 720
                                                    }
                                                    """
                                            )
                                    }
                            ),
                            @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            encoding = @Encoding(name = "request", contentType = MediaType.APPLICATION_JSON_VALUE),
                            examples = {
                                    @ExampleObject(
                                            name = "SCHEDULE_AND_PLACE",
                                            description = """
                                                    **허용 `planningType`**
                                                    - `SCHEDULE_ONLY`
                                                    - `PLACE_ONLY`
                                                    - `SCHEDULE_AND_PLACE` ← 이 예시

                                                    **필수 `request` JSON 입력**
                                                    - 일정: `scheduleCandidateDates`, `availableStartTime`, `availableEndTime`
                                                    - 장소 전략: `placeRecommendationStrategy` (`MIDDLE_POINT` 또는 `RANDOM`)
                                                    - `MIDDLE_POINT` 선택 시: `hostDepartureName`, `hostDepartureAddress`, `hostTransportationMode` (`PUBLIC_TRANSIT` 또는 `CAR`)

                                                    `scheduleMode`와 `placeMode`는 보내지 않습니다. 서버가 각각 `VOTE`, `RECOMMEND`로 설정합니다.
                                                    `coverImage`는 선택 파일 파트이며, 사진이 없으면 생략합니다.
                                                    """,
                                            value = """
                                                    {
                                                      "request": {
                                                        "name": "토요일 저녁 모임",
                                                        "description": "오랜만에 같이 저녁 먹어요.",
                                                        "maxParticipants": 6,
                                                        "planningType": "SCHEDULE_AND_PLACE",
                                                        "scheduleCandidateDates": ["2026-07-10", "2026-07-11"],
                                                        "availableStartTime": "17:00",
                                                        "availableEndTime": "23:00",
                                                        "placeRecommendationStrategy": "MIDDLE_POINT",
                                                        "hostDepartureName": "회사",
                                                        "hostDepartureAddress": "서울 강남구 테헤란로 123",
                                                        "hostTransportationMode": "PUBLIC_TRANSIT",
                                                        "deadlineMinutes": 1440
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "SCHEDULE_ONLY",
                                            description = """
                                                    **허용 `planningType`**
                                                    - `SCHEDULE_ONLY` ← 이 예시
                                                    - `PLACE_ONLY`
                                                    - `SCHEDULE_AND_PLACE`

                                                    **필수 `request` JSON 입력**
                                                    - `scheduleCandidateDates`
                                                    - `availableStartTime`
                                                    - `availableEndTime`

                                                    `placeRecommendationStrategy`와 출발지 필드는 보내지 않습니다.
                                                    `scheduleMode`와 `placeMode`는 서버가 각각 `VOTE`, `NONE`으로 설정합니다.
                                                    `coverImage`는 선택 파일 파트이며, 사진이 없으면 생략합니다.
                                                    """,
                                            value = """
                                                    {
                                                      "request": {
                                                        "name": "주말 일정 정하기",
                                                        "description": "가능한 날짜를 정해요.",
                                                        "maxParticipants": 4,
                                                        "planningType": "SCHEDULE_ONLY",
                                                        "scheduleCandidateDates": ["2026-07-10", "2026-07-11"],
                                                        "availableStartTime": "17:00",
                                                        "availableEndTime": "23:00",
                                                        "deadlineMinutes": 180
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "PLACE_ONLY",
                                            description = """
                                                    **허용 `planningType`**
                                                    - `SCHEDULE_ONLY`
                                                    - `PLACE_ONLY` ← 이 예시
                                                    - `SCHEDULE_AND_PLACE`

                                                    **필수 `request` JSON 입력**
                                                    - `placeRecommendationStrategy`: `MIDDLE_POINT` 또는 `RANDOM`
                                                    - `MIDDLE_POINT` 선택 시: `hostDepartureName`, `hostDepartureAddress`, `hostTransportationMode` (`PUBLIC_TRANSIT` 또는 `CAR`)

                                                    일정 필드는 보내지 않습니다.
                                                    `scheduleMode`와 `placeMode`는 서버가 각각 `NONE`, `RECOMMEND`로 설정합니다.
                                                    `coverImage`는 선택 파일 파트이며, 사진이 없으면 생략합니다.
                                                    """,
                                            value = """
                                                    {
                                                      "request": {
                                                        "name": "카페 장소 정하기",
                                                        "description": "만날 장소를 정해요.",
                                                        "maxParticipants": 8,
                                                        "planningType": "PLACE_ONLY",
                                                        "placeRecommendationStrategy": "RANDOM",
                                                        "deadlineMinutes": 720
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    )
                    }
            )
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "모임 생성 성공"),
            @ApiResponse(responseCode = "400", description = "request JSON 검증 실패"),
            @ApiResponse(responseCode = "413", description = "커버 파일 크기 초과"),
            @ApiResponse(responseCode = "415", description = "JPEG 또는 PNG가 아닌 커버 파일"),
            @ApiResponse(responseCode = "503", description = "커버 이미지 저장소 일시 이용 불가")
    })
    public CreateMeetingResponse createMeetingWithCover(
            @Parameter(hidden = true) @CurrentMember AuthenticatedMember member,
            @Parameter(
                    description = "필수 JSON 파트. FormData 문자열이 아닌 application/json Blob으로 전송합니다.",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CreateMeetingRequest.class))
            )
            @Valid @RequestPart("request") CreateMeetingRequest request,
            @Parameter(description = "선택 JPEG 또는 PNG 파일. 사진이 없으면 이 파트를 생략합니다.")
            @RequestPart(value = "coverImage", required = false) MultipartFile coverImage
    ) {
        return CreateMeetingResponse.from(meetingService.createMeeting(member, request.toCommand(), coverImage));
    }

    @PutMapping(value = "/{meetingId}/cover-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "모임 커버 사진 교체", description = "임시 MVP 정책상 방장만 커버 사진을 교체할 수 있습니다. 성공 응답의 coverImageUrl은 새로 저장된 이미지의 상대 조회 경로입니다. 프론트는 기존 <img src>를 이 값에 API 서버 주소를 붙인 URL로 교체합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "커버 사진 교체 성공"),
            @ApiResponse(responseCode = "403", description = "방장 권한 없음"),
            @ApiResponse(responseCode = "503", description = "커버 이미지 저장소 일시 이용 불가")
    })
    public MeetingCoverResponse replaceCoverImage(
            @PathVariable Long meetingId,
            @Parameter(hidden = true) @CurrentMember AuthenticatedMember member,
            @RequestPart("coverImage") MultipartFile coverImage
    ) {
        return MeetingCoverResponse.from(meetingService.replaceCoverImage(meetingId, member, coverImage));
    }

    @DeleteMapping("/{meetingId}/cover-image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "모임 커버 사진 삭제", description = "임시 MVP 정책상 방장만 모임 커버 사진을 삭제할 수 있습니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "커버 사진 삭제 성공"),
            @ApiResponse(responseCode = "403", description = "방장 권한 없음"),
            @ApiResponse(responseCode = "404", description = "삭제할 커버 사진 없음")
    })
    public void deleteCoverImage(
            @PathVariable Long meetingId,
            @Parameter(hidden = true) @CurrentMember AuthenticatedMember member
    ) {
        meetingService.deleteCoverImage(meetingId, member);
    }

    @GetMapping("/invitations/{inviteCode}/cover-image")
    @Operation(summary = "초대 링크 모임 커버 사진 조회", description = "초대 조회·모임 조회 응답의 coverImageUrl을 그대로 사용해 <img src>에 넣는 이미지 바이너리 API입니다. 상대 경로이므로 프론트 API 서버 주소를 앞에 붙입니다. v 쿼리는 브라우저 캐시 갱신 전용이며 변경하거나 제거하지 않습니다. 응답은 1년 immutable 캐시 정책을 사용합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "커버 사진 조회 성공"),
            @ApiResponse(responseCode = "404", description = "모임 또는 커버 사진 없음"),
            @ApiResponse(responseCode = "503", description = "커버 이미지 저장소 일시 이용 불가")
    })
    public ResponseEntity<byte[]> getCoverImage(
            @PathVariable String inviteCode,
            @Parameter(description = "브라우저 캐시를 구분하는 응답 전용 버전값", example = "15v9zq") @RequestParam(required = false) String v
    ) {
        var cover = meetingService.getCoverImage(inviteCode);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(cover.contentType()))
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(365)).cachePublic().immutable())
                .body(cover.content());
    }

    @GetMapping("/invitations/{inviteCode}")
    @Operation(
            summary = "초대 코드로 모임 조회",
            description = """
                    초대 링크 진입 화면에서 사용할 모임 정보를 조회합니다.<br>
                    참여 가능 여부와 마감/정원 초과 안내 문구를 함께 반환합니다.
                    모임장 프로필 이미지는 아직 사용자 프로필 정책이 확정되지 않아 응답에 포함하지 않았고, 프로필 정책 확정 후 협의하여 확장할 예정입니다.
                    진행상황 확인 버튼은 추후 VIEW-01 모임 현황 API와 연결됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "초대 코드 모임 조회 성공"),
            @ApiResponse(
                    responseCode = "404",
                    description = "초대 코드에 해당하는 모임 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "MEETING_INVITATION_NOT_FOUND",
                              "status": 404
                            }
                            """))
            )
    })
    public MeetingInvitationResponse getInvitation(@PathVariable String inviteCode) {
        return MeetingInvitationResponse.from(meetingService.getInvitation(inviteCode));
    }

    @GetMapping("/invitations/{inviteCode}/view")
    @Operation(
            summary = "모임 현황 조회",
            description = """
                    확정 전 모임 상세 화면에서 표시할 기본 현황을 조회합니다.<br>
                    로그인 없이 열람할 수 있으며, 참여 인원과 마감까지 남은 시간, 응답 완료 비율, 참여자별 응답 상태를 반환합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "모임 현황 조회 성공"),
            @ApiResponse(
                    responseCode = "404",
                    description = "초대 코드에 해당하는 모임 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "MEETING_INVITATION_NOT_FOUND",
                              "status": 404
                            }
                            """))
            )
    })
    public MeetingViewResponse getMeetingView(@PathVariable String inviteCode) {
        return MeetingViewResponse.from(meetingService.getMeetingView(inviteCode));
    }

    @GetMapping("/invitations/{inviteCode}/view/schedules")
    @Operation(
            summary = "일정 조율 현황 조회",
            description = """
                    확정 전 모임 상세 화면의 일정 조율 현황을 조회합니다.<br>
                    저장된 참여자 가능 시간 슬롯을 집계해 최대 5개의 후보를 반환합니다.
                    sort는 LONGEST_MEETING 또는 EARLIEST_DATE를 사용할 수 있으며, 생략하면 LONGEST_MEETING으로 정렬합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "일정 조율 현황 조회 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "지원하지 않는 정렬 방식",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "COMMON_INVALID_REQUEST",
                              "status": 400
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "초대 코드에 해당하는 모임 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "MEETING_INVITATION_NOT_FOUND",
                              "status": 404
                            }
                            """))
            )
    })
    public ScheduleViewResponse getScheduleView(
            @PathVariable String inviteCode,
            @RequestParam(defaultValue = "LONGEST_MEETING") String sort
    ) {
        return ScheduleViewResponse.from(meetingService.getScheduleView(inviteCode, sort));
    }

    @GetMapping("/invitations/{inviteCode}/view/places")
    @Operation(
            summary = "장소 조율 현황 조회",
            description = """
                    확정 전 모임 상세 화면의 장소 조율 현황을 조회합니다.<br>
                    현재는 외부 이동시간 API를 호출하지 않고, 참여자 출발지 좌표와 임시 상권 카탈로그를 이용한 단순 직선거리 기반 미리보기를 반환합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "장소 조율 현황 조회 성공"),
            @ApiResponse(
                    responseCode = "404",
                    description = "초대 코드에 해당하는 모임 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "MEETING_INVITATION_NOT_FOUND",
                              "status": 404
                            }
                            """))
            )
    })
    public PlaceViewResponse getPlaceView(@PathVariable String inviteCode) {
        return PlaceViewResponse.from(meetingService.getPlaceView(inviteCode));
    }

    @PostMapping("/invitations/{inviteCode}/guests")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "게스트 모임 참여",
            description = """
                    모임 생성 성공 응답의 inviteCode를 경로 변수에 넣어, 해당 모임에 게스트 참여자를 생성합니다.<br>
                    닉네임, 비밀번호와 모임 유형에 맞는 일정 또는 출발지 정보를 한 번에 저장합니다.
                    일정 조율 모임은 scheduleAvailabilities를, 장소 조율 모임은 departure를 필수로 입력해야 합니다.
                    게스트 재입장/수정 인증은 정책 확정 후 함께 보완할 예정입니다.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "SCHEDULE_AND_PLACE_1",
                                            description = "모임 생성의 SCHEDULE_AND_PLACE 예시와 연결됩니다. 5개 후보일과 17:00~23:00 범위 안에서 여러 날짜·시간대를 선택한 첫 번째 게스트 참여 예시입니다.",
                                            value = """
                                            {
                                              "nickname": "민지 친구 1",
                                              "password": "moyeo2026!",
                                              "scheduleAvailabilities": [
                                                {
                                                  "candidateDate": "2026-07-10",
                                                  "startTime": "18:00",
                                                  "endTime": "20:00"
                                                },
                                                {
                                                  "candidateDate": "2026-07-11",
                                                  "startTime": "20:00",
                                                  "endTime": "23:00"
                                                },
                                                {
                                                  "candidateDate": "2026-07-12",
                                                  "startTime": "17:00",
                                                  "endTime": "19:00"
                                                }
                                              ],
                                              "departure": {
                                                "name": "회사",
                                                "address": "서울 강남구 테헤란로 123",
                                                "latitude": 37.498095,
                                                "longitude": 127.027610,
                                                "transportationMode": "PUBLIC_TRANSIT"
                                              }
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "SCHEDULE_AND_PLACE_2",
                                            description = "같은 모임에 사용할 두 번째 게스트 참여 예시입니다. 첫 번째 예시와 겹치는 시간과 겹치지 않는 시간을 함께 넣어 추천 결과를 비교할 수 있습니다.",
                                            value = """
                                            {
                                              "nickname": "민지 친구 2",
                                              "password": "moyeo2026!",
                                              "scheduleAvailabilities": [
                                                {
                                                  "candidateDate": "2026-07-10",
                                                  "startTime": "19:00",
                                                  "endTime": "22:00"
                                                },
                                                {
                                                  "candidateDate": "2026-07-12",
                                                  "startTime": "18:00",
                                                  "endTime": "23:00"
                                                },
                                                {
                                                  "candidateDate": "2026-07-13",
                                                  "startTime": "17:00",
                                                  "endTime": "20:00"
                                                }
                                              ],
                                              "departure": {
                                                "name": "집",
                                                "address": "서울 마포구 월드컵북로 1",
                                                "latitude": 37.566500,
                                                "longitude": 126.978000,
                                                "transportationMode": "CAR"
                                              }
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "SCHEDULE_ONLY",
                                            description = "모임 생성의 SCHEDULE_ONLY 예시와 연결됩니다. 일정만 입력하고 departure는 포함하지 않습니다.",
                                            value = """
                                            {
                                              "nickname": "민지 친구",
                                              "password": "moyeo2026!",
                                              "scheduleAvailabilities": [
                                                {
                                                  "candidateDate": "2026-07-10",
                                                  "startTime": "18:00",
                                                  "endTime": "20:00"
                                                },
                                                {
                                                  "candidateDate": "2026-07-13",
                                                  "startTime": "20:00",
                                                  "endTime": "22:00"
                                                }
                                              ]
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "PLACE_ONLY",
                                            description = "모임 생성의 PLACE_ONLY 예시와 연결됩니다. 출발지만 입력하고 scheduleAvailabilities는 포함하지 않습니다.",
                                            value = """
                                            {
                                              "nickname": "민지 친구",
                                              "password": "moyeo2026!",
                                              "departure": {
                                                "name": "회사",
                                                "address": "서울 강남구 테헤란로 123",
                                                "latitude": 37.498095,
                                                "longitude": 127.027610,
                                                "transportationMode": "PUBLIC_TRANSIT"
                                              }
                                            }
                                            """
                                    )
                            }
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "게스트 참여 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "COMMON_VALIDATION_FAILED",
                              "status": 400
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "초대 코드에 해당하는 모임 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "MEETING_INVITATION_NOT_FOUND",
                              "status": 404
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "닉네임 중복, 모임 인원 초과 또는 참여 마감",
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "모임 참여 마감",
                                    value = """
                                            {
                                              "code": "MEETING_PARTICIPATION_CLOSED",
                                              "status": 409
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "모임 인원 초과",
                                    value = """
                                            {
                                              "code": "MEETING_PARTICIPANT_LIMIT_EXCEEDED",
                                              "status": 409
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "모임 안 닉네임 중복",
                                    value = """
                                            {
                                              "code": "DUPLICATE_MEETING_PARTICIPANT_NICKNAME",
                                              "status": 409
                                            }
                                            """
                            )
                    })
            )
    })
    public ParticipantJoinResponse joinGuest(
            @Parameter(
                    description = "모임 생성 성공 응답의 inviteCode 값입니다.",
                    example = "ABCD234567"
            )
            @PathVariable String inviteCode,
            @Valid @RequestBody GuestJoinRequest request
    ) {
        return ParticipantJoinResponse.from(
                meetingService.joinGuest(inviteCode, request.nickname(), request.password(), request.toParticipationCommand())
        );
    }

    @PostMapping("/invitations/{inviteCode}/members")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "로그인 회원 모임 참여",
            description = """
                    모임 생성 성공 응답의 inviteCode를 경로 변수에 넣어 현재 로그인한 서비스 사용자를 모임 참여자로 생성합니다.<br>
                    회원 기본 닉네임과 다른 모임 안 표시 닉네임을 입력할 수 있습니다.
                    일정 조율 모임은 scheduleAvailabilities를, 장소 조율 모임은 departure를 함께 입력해야 합니다.
                    로그인 회원은 Bearer Access Token으로 식별하므로 참여 비밀번호를 입력하지 않습니다.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "SCHEDULE_AND_PLACE_1",
                                            description = "모임 생성의 SCHEDULE_AND_PLACE 예시와 연결됩니다. Authorize에서 Bearer Access Token을 설정한 뒤 5개 후보일과 17:00~23:00 범위 안에서 여러 날짜·시간대를 선택합니다.",
                                            value = """
                                            {
                                              "nickname": "민지 1",
                                              "scheduleAvailabilities": [
                                                {
                                                  "candidateDate": "2026-07-10",
                                                  "startTime": "19:00",
                                                  "endTime": "22:00"
                                                },
                                                {
                                                  "candidateDate": "2026-07-11",
                                                  "startTime": "17:00",
                                                  "endTime": "20:00"
                                                },
                                                {
                                                  "candidateDate": "2026-07-14",
                                                  "startTime": "20:00",
                                                  "endTime": "23:00"
                                                }
                                              ],
                                              "departure": {
                                                "name": "집",
                                                "address": "서울 마포구 월드컵북로 1",
                                                "latitude": 37.566500,
                                                "longitude": 126.978000,
                                                "transportationMode": "CAR"
                                              }
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "SCHEDULE_AND_PLACE_2",
                                            description = "같은 모임에 사용할 두 번째 로그인 회원 참여 예시입니다. 다른 참여 예시와 일부 시간만 겹치도록 구성되어 추천 후보 순서를 확인할 수 있습니다.",
                                            value = """
                                            {
                                              "nickname": "민지 2",
                                              "scheduleAvailabilities": [
                                                {
                                                  "candidateDate": "2026-07-10",
                                                  "startTime": "18:00",
                                                  "endTime": "21:00"
                                                },
                                                {
                                                  "candidateDate": "2026-07-12",
                                                  "startTime": "19:00",
                                                  "endTime": "22:00"
                                                },
                                                {
                                                  "candidateDate": "2026-07-13",
                                                  "startTime": "17:00",
                                                  "endTime": "19:00"
                                                }
                                              ],
                                              "departure": {
                                                "name": "학교",
                                                "address": "서울 성동구 왕십리로 222",
                                                "latitude": 37.561000,
                                                "longitude": 127.036500,
                                                "transportationMode": "PUBLIC_TRANSIT"
                                              }
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "SCHEDULE_ONLY",
                                            description = "모임 생성의 SCHEDULE_ONLY 예시와 연결됩니다. 일정만 입력하고 departure는 포함하지 않습니다.",
                                            value = """
                                            {
                                              "nickname": "민지",
                                              "scheduleAvailabilities": [
                                                {
                                                  "candidateDate": "2026-07-10",
                                                  "startTime": "18:00",
                                                  "endTime": "20:00"
                                                },
                                                {
                                                  "candidateDate": "2026-07-14",
                                                  "startTime": "21:00",
                                                  "endTime": "23:00"
                                                }
                                              ]
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "PLACE_ONLY",
                                            description = "모임 생성의 PLACE_ONLY 예시와 연결됩니다. 출발지만 입력하고 scheduleAvailabilities는 포함하지 않습니다.",
                                            value = """
                                            {
                                              "nickname": "민지",
                                              "departure": {
                                                "name": "집",
                                                "address": "서울 마포구 월드컵북로 1",
                                                "latitude": 37.566500,
                                                "longitude": 126.978000,
                                                "transportationMode": "CAR"
                                              }
                                            }
                                            """
                                    )
                            }
                    )
            )
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "로그인 회원 모임 참여 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "COMMON_VALIDATION_FAILED",
                              "status": 400
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token 없음, 만료 또는 유효하지 않음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "AUTHENTICATION_REQUIRED",
                              "status": 401
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "초대 코드에 해당하는 모임 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "MEETING_INVITATION_NOT_FOUND",
                              "status": 404
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "회원 중복 참여, 모임 인원 초과 또는 참여 마감",
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "이미 참여한 회원",
                                    value = """
                                            {
                                              "code": "DUPLICATE_MEETING_PARTICIPANT_MEMBER",
                                              "status": 409
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "모임 인원 초과",
                                    value = """
                                            {
                                              "code": "MEETING_PARTICIPANT_LIMIT_EXCEEDED",
                                              "status": 409
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "모임 참여 마감",
                                    value = """
                                            {
                                              "code": "MEETING_PARTICIPATION_CLOSED",
                                              "status": 409
                                            }
                                            """
                            )
                    })
            )
    })
    public ParticipantJoinResponse joinMember(
            @Parameter(hidden = true) @CurrentMember AuthenticatedMember member,
            @Parameter(
                    description = "모임 생성 성공 응답의 inviteCode 값입니다.",
                    example = "ABCD234567"
            )
            @PathVariable String inviteCode,
            @Valid @RequestBody MemberJoinRequest request
    ) {
        return ParticipantJoinResponse.from(
                meetingService.joinMember(
                        inviteCode,
                        member,
                        request.nickname(),
                        request.toParticipationCommand()
                )
        );
    }

}
