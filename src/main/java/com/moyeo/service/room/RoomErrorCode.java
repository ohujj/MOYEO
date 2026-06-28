package com.moyeo.service.room;

import com.moyeo.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

import java.net.URI;

public enum RoomErrorCode implements ErrorCode {

    ROOM_INVITATION_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "ROOM_INVITATION_NOT_FOUND",
            "room-invitation-not-found",
            "초대 링크 없음",
            "초대 코드에 해당하는 모임을 찾을 수 없습니다."
    ),
    DUPLICATE_ROOM_PARTICIPANT_NICKNAME(
            HttpStatus.CONFLICT,
            "DUPLICATE_ROOM_PARTICIPANT_NICKNAME",
            "duplicate-room-participant-nickname",
            "참여자 닉네임 중복",
            "이미 해당 모임에서 사용 중인 닉네임입니다."
    ),
    ROOM_PARTICIPANT_LIMIT_EXCEEDED(
            HttpStatus.CONFLICT,
            "ROOM_PARTICIPANT_LIMIT_EXCEEDED",
            "room-participant-limit-exceeded",
            "모임 인원 초과",
            "모임 최대 참여 인원을 초과했습니다."
    );

    private static final String TYPE_PREFIX = "urn:moyeo:problem:";

    private final HttpStatus status;
    private final String code;
    private final URI type;
    private final String title;
    private final String detail;

    RoomErrorCode(HttpStatus status, String code, String type, String title, String detail) {
        this.status = status;
        this.code = code;
        this.type = URI.create(TYPE_PREFIX + type);
        this.title = title;
        this.detail = detail;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public URI type() {
        return type;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public String detail() {
        return detail;
    }
}
