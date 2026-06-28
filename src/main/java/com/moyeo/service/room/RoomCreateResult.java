package com.moyeo.service.room;

import com.moyeo.domain.room.Room;
import com.moyeo.domain.room.RoomParticipant;

public record RoomCreateResult(
        Long roomId,
        String name,
        String description,
        Integer maxParticipants,
        String inviteCode,
        String invitePath,
        Long hostParticipantId
) {

    public static RoomCreateResult from(Room room, RoomParticipant hostParticipant) {
        return new RoomCreateResult(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.getMaxParticipants(),
                room.getInviteCode(),
                "/rooms/invitations/" + room.getInviteCode(),
                hostParticipant.getId()
        );
    }
}
