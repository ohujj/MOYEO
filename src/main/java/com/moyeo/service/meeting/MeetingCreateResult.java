package com.moyeo.service.meeting;

import com.moyeo.domain.meeting.Meeting;

public record MeetingCreateResult(
        Long meetingId,
        String inviteCode,
        String invitePath
) {

    public static MeetingCreateResult from(Meeting meeting) {
        return new MeetingCreateResult(
                meeting.getId(),
                meeting.getInviteCode(),
                "/meetings/invitations/" + meeting.getInviteCode()
        );
    }
}
