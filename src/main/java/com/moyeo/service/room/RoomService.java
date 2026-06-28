package com.moyeo.service.room;

import com.moyeo.domain.member.User;
import com.moyeo.domain.room.Room;
import com.moyeo.domain.room.RoomParticipant;
import com.moyeo.global.error.CommonErrorCode;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.repository.member.UserRepository;
import com.moyeo.repository.room.RoomParticipantRepository;
import com.moyeo.repository.room.RoomRepository;
import com.moyeo.service.member.AuthenticatedMember;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final UserRepository userRepository;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final PasswordEncoder passwordEncoder;

    public RoomService(
            RoomRepository roomRepository,
            RoomParticipantRepository roomParticipantRepository,
            UserRepository userRepository,
            InviteCodeGenerator inviteCodeGenerator,
            PasswordEncoder passwordEncoder
    ) {
        this.roomRepository = roomRepository;
        this.roomParticipantRepository = roomParticipantRepository;
        this.userRepository = userRepository;
        this.inviteCodeGenerator = inviteCodeGenerator;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RoomCreateResult createRoom(
            AuthenticatedMember hostMember,
            String name,
            String description,
            int maxParticipants
    ) {
        User hostUser = userRepository.findById(hostMember.userId())
                .orElseThrow(() -> new MoyeoException(CommonErrorCode.INVALID_REQUEST));

        Room room = new Room(
                hostUser,
                normalizeRequired(name),
                normalizeOptional(description),
                maxParticipants,
                inviteCodeGenerator.generate()
        );
        Room savedRoom = roomRepository.saveAndFlush(room);

        RoomParticipant hostParticipant = roomParticipantRepository.saveAndFlush(
                RoomParticipant.host(savedRoom, hostUser)
        );

        return RoomCreateResult.from(savedRoom, hostParticipant);
    }

    public RoomInvitationResult getInvitation(String inviteCode) {
        Room room = findRoomByInviteCode(inviteCode);
        long participantCount = roomParticipantRepository.countByRoomId(room.getId());
        return RoomInvitationResult.from(room, participantCount);
    }

    @Transactional
    public GuestJoinResult joinGuest(String inviteCode, String nickname, String rawPassword) {
        Room room = findRoomByInviteCode(inviteCode);
        String normalizedNickname = normalizeRequired(nickname);

        if (roomParticipantRepository.countByRoomId(room.getId()) >= room.getMaxParticipants()) {
            throw new MoyeoException(RoomErrorCode.ROOM_PARTICIPANT_LIMIT_EXCEEDED);
        }

        if (roomParticipantRepository.existsByRoomAndNickname(room, normalizedNickname)) {
            throw new MoyeoException(RoomErrorCode.DUPLICATE_ROOM_PARTICIPANT_NICKNAME);
        }

        try {
            RoomParticipant participant = roomParticipantRepository.saveAndFlush(
                    RoomParticipant.guest(room, normalizedNickname, passwordEncoder.encode(rawPassword))
            );
            return GuestJoinResult.from(room, participant);
        } catch (DataIntegrityViolationException exception) {
            throw new MoyeoException(RoomErrorCode.DUPLICATE_ROOM_PARTICIPANT_NICKNAME);
        }
    }

    private Room findRoomByInviteCode(String inviteCode) {
        return roomRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new MoyeoException(RoomErrorCode.ROOM_INVITATION_NOT_FOUND));
    }

    private String normalizeRequired(String value) {
        return value.strip();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
