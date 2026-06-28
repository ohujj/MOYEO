package com.moyeo.controller.room;

import com.moyeo.global.security.CurrentMember;
import com.moyeo.service.member.AuthenticatedMember;
import com.moyeo.service.room.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
            description = "로그인한 사용자가 방장이 되어 모임을 생성하고 초대 코드를 발급받습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "모임 생성 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청값 검증 실패",
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
            @CurrentMember AuthenticatedMember member,
            @Valid @RequestBody CreateRoomRequest request
    ) {
        return CreateRoomResponse.from(roomService.createRoom(
                member,
                request.name(),
                request.description(),
                request.maxParticipants()
        ));
    }

    @GetMapping("/invitations/{inviteCode}")
    @Operation(
            summary = "초대 코드로 모임 조회",
            description = "초대 링크 진입 화면에서 사용할 모임 기본 정보를 조회합니다."
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
            description = "초대 코드로 들어온 게스트가 닉네임과 비밀번호를 입력해 모임에 참여합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "게스트 참여 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청값 검증 실패",
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
                    description = "닉네임 중복 또는 모임 인원 초과",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "DUPLICATE_ROOM_PARTICIPANT_NICKNAME",
                              "status": 409
                            }
                            """))
            )
    })
    public GuestJoinResponse joinGuest(
            @PathVariable String inviteCode,
            @Valid @RequestBody GuestJoinRequest request
    ) {
        return GuestJoinResponse.from(roomService.joinGuest(inviteCode, request.nickname(), request.password()));
    }
}
