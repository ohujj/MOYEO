package com.moyeo.service.room;

import com.moyeo.domain.room.Room;

public record RoomInvitationResult(
        Long roomId,
        String name,
        String description,
        Integer maxParticipants,
        long participantCount,
        String hostNickname
) {

    public static RoomInvitationResult from(Room room, long participantCount) {
        return new RoomInvitationResult(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.getMaxParticipants(),
                participantCount,
                room.getHostUser().getNickname()
        );
    }
}
