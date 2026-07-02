package com.moyeo.controller.room;

import com.moyeo.global.security.CurrentMember;
import com.moyeo.service.member.AuthenticatedMember;
import com.moyeo.service.room.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Room", description = "모임 생성, 초대 코드 조회, 게스트 참여 API")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "모임 생성",
            description = """
                    로그인한 사용자가 방장이 되어 모임을 생성하고 초대 코드를 발급받습니다.<br>
                    클라이언트는 FAB에서 선택한 생성 플로우를 완료한 뒤 마지막 링크 생성 시 한 번에 요청합니다.
                    <ul>
                      <li>SCHEDULE_ONLY: SCR-06 기본 정보 -> SCR-07 일정 정하기 -> SCR-09 마감 시간 -> SCR-10 완료. 일정 조율(VOTE)만 사용합니다.</li>
                      <li>PLACE_ONLY: SCR-06 기본 정보 -> SCR-08 장소 정하기 -> SCR-09 마감 시간 -> SCR-10 완료. 장소를 나중에 정할 방식(RECOMMEND)을 저장하며 MIDDLE_POINT 또는 RANDOM을 선택합니다.</li>
                      <li>SCHEDULE_AND_PLACE: SCR-06 기본 정보 -> SCR-07 일정 정하기 -> SCR-08 장소 정하기 -> SCR-09 마감 시간 -> SCR-10 완료. 일정 조율(VOTE)과 장소를 나중에 정할 방식(RECOMMEND)을 함께 저장합니다.</li>
                    </ul>
                    scheduleMode와 placeMode는 서버가 planningType 기준으로 파생합니다.
                    확정 일정/확정 장소 직접 입력 플로우는 이번 MVP 생성 범위에서 제외하며, 추후 회의에서 재검토합니다.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "SCHEDULE_AND_PLACE",
                                            description = "일정 조율(VOTE)과 장소를 나중에 정할 방식(RECOMMEND)을 함께 저장하는 생성 요청입니다. 장소 방식은 MIDDLE_POINT 또는 RANDOM 중 선택하며, 생성 시점에는 추천 결과나 확정 장소를 만들지 않습니다. MIDDLE_POINT를 선택하면 방장 출발지 주소를 방장 참여자 정보에 저장합니다.",
                                            value = """
                                                    {
                                                      "name": "토요일 저녁 모임",
                                                      "description": "오랜만에 같이 저녁 먹어요.",
                                                      "maxParticipants": 6,
                                                      "planningType": "SCHEDULE_AND_PLACE",
                                                      "scheduleCandidateDates": [
                                                        "2026-07-04",
                                                        "2026-07-05"
                                                      ],
                                                      "availableStartTime": "18:00",
                                                      "availableEndTime": "22:00",
                                                      "placeRecommendationStrategy": "MIDDLE_POINT",
                                                      "hostDepartureAddress": "서울 강남구 테헤란로 123",
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
                                                        "2026-07-04",
                                                        "2026-07-05"
                                                      ],
                                                      "availableStartTime": "18:00",
                                                      "availableEndTime": "22:00",
                                                      "deadlineMinutes": 180
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "PLACE_ONLY",
                                            description = "장소만 정하는 생성 요청입니다. 현재는 장소를 나중에 정할 방식(RECOMMEND)만 저장하며, MIDDLE_POINT 또는 RANDOM 중 하나를 선택합니다. 생성 시점에는 추천 결과나 확정 장소를 만들지 않습니다. 일정 후보와 시간대는 받지 않습니다.",
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
    public CreateRoomResponse createRoom(
            @Parameter(hidden = true) @CurrentMember AuthenticatedMember member,
            @Valid @RequestBody CreateRoomRequest request
    ) {
        return CreateRoomResponse.from(roomService.createRoom(member, request.toCommand()));
    }

    @GetMapping("/invitations/{inviteCode}")
    @Operation(
            summary = "초대 코드로 모임 조회",
            description = "초대 링크 진입 화면에서 사용할 모임 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "초대 코드 모임 조회 성공"),
            @ApiResponse(
                    responseCode = "404",
                    description = "초대 코드에 해당하는 모임 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "ROOM_INVITATION_NOT_FOUND",
                              "status": 404
                            }
                            """))
            )
    })
    public RoomInvitationResponse getInvitation(@PathVariable String inviteCode) {
        return RoomInvitationResponse.from(roomService.getInvitation(inviteCode));
    }

    @PostMapping("/invitations/{inviteCode}/guests")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "게스트 모임 참여",
            description = "초대 코드에 해당하는 모임에 게스트 참여자를 생성합니다."
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
                              "code": "ROOM_INVITATION_NOT_FOUND",
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
                                              "code": "ROOM_PARTICIPATION_CLOSED",
                                              "status": 409
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "모임 인원 초과",
                                    value = """
                                            {
                                              "code": "ROOM_PARTICIPANT_LIMIT_EXCEEDED",
                                              "status": 409
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "모임 안 닉네임 중복",
                                    value = """
                                            {
                                              "code": "DUPLICATE_ROOM_PARTICIPANT_NICKNAME",
                                              "status": 409
                                            }
                                            """
                            )
                    })
            )
    })
    public GuestJoinResponse joinGuest(
            @PathVariable String inviteCode,
            @Valid @RequestBody GuestJoinRequest request
    ) {
        return GuestJoinResponse.from(roomService.joinGuest(inviteCode, request.nickname(), request.password()));
    }
}
