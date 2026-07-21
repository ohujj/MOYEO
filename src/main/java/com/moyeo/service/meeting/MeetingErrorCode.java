package com.moyeo.service.meeting;

import com.moyeo.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

import java.net.URI;

public enum MeetingErrorCode implements ErrorCode {

    MEETING_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "MEETING_NOT_FOUND",
            "meeting-not-found",
            "모임 없음",
            "해당 모임을 찾을 수 없습니다."
    ),
    MEETING_INVITATION_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "MEETING_INVITATION_NOT_FOUND",
            "meeting-invitation-not-found",
            "초대 링크 없음",
            "초대 코드에 해당하는 모임을 찾을 수 없습니다."
    ),
    DUPLICATE_MEETING_PARTICIPANT_NICKNAME(
            HttpStatus.CONFLICT,
            "DUPLICATE_MEETING_PARTICIPANT_NICKNAME",
            "duplicate-meeting-participant-nickname",
            "참여자 닉네임 중복",
            "이미 해당 모임에서 사용 중인 닉네임입니다."
    ),
    DUPLICATE_MEETING_PARTICIPANT_MEMBER(
            HttpStatus.CONFLICT,
            "DUPLICATE_MEETING_PARTICIPANT_MEMBER",
            "duplicate-meeting-participant-member",
            "모임 참여 회원 중복",
            "이미 해당 모임에 참여 중인 회원입니다."
    ),
    MEETING_PARTICIPANT_LIMIT_EXCEEDED(
            HttpStatus.CONFLICT,
            "MEETING_PARTICIPANT_LIMIT_EXCEEDED",
            "meeting-participant-limit-exceeded",
            "모임 인원 초과",
            "모임 최대 참여 인원을 초과했습니다."
    ),
    MEETING_PARTICIPATION_CLOSED(
            HttpStatus.CONFLICT,
            "MEETING_PARTICIPATION_CLOSED",
            "meeting-participation-closed",
            "모임 참여 마감",
            "모임 참여 마감 시간이 지났습니다."
    ),
    MEETING_PARTICIPANT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "MEETING_PARTICIPANT_NOT_FOUND",
            "meeting-participant-not-found",
            "모임 참여자 없음",
            "해당 모임의 참여자를 찾을 수 없습니다."
    ),
    INVALID_MEETING_PARTICIPATION_INPUT(
            HttpStatus.BAD_REQUEST,
            "INVALID_MEETING_PARTICIPATION_INPUT",
            "invalid-meeting-participation-input",
            "모임 참여 입력 오류",
            "모임 참여 입력 값이 모임 설정과 맞지 않습니다."
    );

    private static final String TYPE_PREFIX = "urn:moyeo:problem:";

    private final HttpStatus status;
    private final String code;
    private final URI type;
    private final String title;
    private final String detail;

    MeetingErrorCode(HttpStatus status, String code, String type, String title, String detail) {
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
