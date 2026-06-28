package com.moyeo.service.room;

import com.moyeo.domain.room.Room;
import com.moyeo.domain.room.RoomParticipant;

public record GuestJoinResult(
        Long roomId,
        Long participantId,
        String nickname,
        String participantType
) {

    public static GuestJoinResult from(Room room, RoomParticipant participant) {
        return new GuestJoinResult(
                room.getId(),
                participant.getId(),
                participant.getNickname(),
                participant.getParticipantType().name()
        );
    }
}
