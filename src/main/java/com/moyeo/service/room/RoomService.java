package com.moyeo.service.room;

import com.moyeo.domain.member.User;
import com.moyeo.domain.room.PlaceMode;
import com.moyeo.domain.room.PlaceRecommendationStrategy;
import com.moyeo.domain.room.Room;
import com.moyeo.domain.room.RoomParticipant;
import com.moyeo.domain.room.RoomScheduleCandidate;
import com.moyeo.domain.room.ScheduleMode;
import com.moyeo.global.error.CommonErrorCode;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.repository.member.UserRepository;
import com.moyeo.repository.room.RoomParticipantRepository;
import com.moyeo.repository.room.RoomRepository;
import com.moyeo.repository.room.RoomScheduleCandidateRepository;
import com.moyeo.service.member.AuthenticatedMember;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final RoomScheduleCandidateRepository roomScheduleCandidateRepository;
    private final UserRepository userRepository;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final PasswordEncoder passwordEncoder;

    public RoomService(
            RoomRepository roomRepository,
            RoomParticipantRepository roomParticipantRepository,
            RoomScheduleCandidateRepository roomScheduleCandidateRepository,
            UserRepository userRepository,
            InviteCodeGenerator inviteCodeGenerator,
            PasswordEncoder passwordEncoder
    ) {
        this.roomRepository = roomRepository;
        this.roomParticipantRepository = roomParticipantRepository;
        this.roomScheduleCandidateRepository = roomScheduleCandidateRepository;
        this.userRepository = userRepository;
        this.inviteCodeGenerator = inviteCodeGenerator;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RoomCreateResult createRoom(
            AuthenticatedMember hostMember,
            CreateRoomCommand command
    ) {
        User hostUser = userRepository.findById(hostMember.userId())
                .orElseThrow(() -> new MoyeoException(CommonErrorCode.INVALID_REQUEST));

        Room room = new Room(
                hostUser,
                normalizeRequired(command.name()),
                normalizeOptional(command.description()),
                command.maxParticipants(),
                command.planningType(),
                command.scheduleMode(),
                resolveFixedScheduleAt(command),
                resolveAvailableStartTime(command),
                resolveAvailableEndTime(command),
                command.placeMode(),
                resolvePlaceRecommendationStrategy(command),
                resolveFixedPlaceName(command),
                resolveFixedPlaceAddress(command),
                LocalDateTime.now().plusMinutes(command.deadlineMinutes()),
                inviteCodeGenerator.generate()
        );
        Room savedRoom = roomRepository.saveAndFlush(room);
        saveScheduleCandidates(savedRoom, command);

        RoomParticipant hostParticipant = roomParticipantRepository.saveAndFlush(
                RoomParticipant.host(savedRoom, hostUser, normalizeOptional(command.hostDepartureAddress()))
        );

        List<RoomScheduleCandidate> scheduleCandidates = roomScheduleCandidateRepository
                .findAllByRoomIdOrderByCandidateDateAsc(savedRoom.getId());
        return RoomCreateResult.from(savedRoom, hostParticipant, scheduleCandidates);
    }

    public RoomInvitationResult getInvitation(String inviteCode) {
        Room room = findRoomByInviteCode(inviteCode);
        long participantCount = roomParticipantRepository.countByRoomId(room.getId());
        List<RoomScheduleCandidate> scheduleCandidates = roomScheduleCandidateRepository
                .findAllByRoomIdOrderByCandidateDateAsc(room.getId());
        return RoomInvitationResult.from(room, participantCount, scheduleCandidates);
    }

    @Transactional
    public GuestJoinResult joinGuest(String inviteCode, String nickname, String rawPassword) {
        String normalizedNickname = normalizeRequired(nickname);
        String passwordHash = passwordEncoder.encode(rawPassword);

        Room room = findRoomByInviteCodeForUpdate(inviteCode);

        if (!room.getDeadlineAt().isAfter(LocalDateTime.now())) {
            throw new MoyeoException(RoomErrorCode.ROOM_PARTICIPATION_CLOSED);
        }

        if (roomParticipantRepository.countByRoomId(room.getId()) >= room.getMaxParticipants()) {
            throw new MoyeoException(RoomErrorCode.ROOM_PARTICIPANT_LIMIT_EXCEEDED);
        }

        if (roomParticipantRepository.existsByRoomAndNickname(room, normalizedNickname)) {
            throw new MoyeoException(RoomErrorCode.DUPLICATE_ROOM_PARTICIPANT_NICKNAME);
        }

        try {
            RoomParticipant participant = roomParticipantRepository.saveAndFlush(
                    RoomParticipant.guest(room, normalizedNickname, passwordHash)
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

    private Room findRoomByInviteCodeForUpdate(String inviteCode) {
        return roomRepository.findByInviteCodeForUpdate(inviteCode)
                .orElseThrow(() -> new MoyeoException(RoomErrorCode.ROOM_INVITATION_NOT_FOUND));
    }

    private void saveScheduleCandidates(Room room, CreateRoomCommand command) {
        if (command.scheduleMode() == ScheduleMode.VOTE) {
            List<RoomScheduleCandidate> candidates = command.scheduleCandidateDates().stream()
                    .distinct()
                    .sorted()
                    .map(candidateDate -> new RoomScheduleCandidate(room, candidateDate))
                    .toList();
            roomScheduleCandidateRepository.saveAll(candidates);
        }
    }

    private LocalDateTime resolveFixedScheduleAt(CreateRoomCommand command) {
        return command.scheduleMode() == ScheduleMode.FIXED ? command.fixedScheduleAt() : null;
    }

    private LocalTime resolveAvailableStartTime(CreateRoomCommand command) {
        return command.scheduleMode() == ScheduleMode.VOTE ? command.availableStartTime() : null;
    }

    private LocalTime resolveAvailableEndTime(CreateRoomCommand command) {
        return command.scheduleMode() == ScheduleMode.VOTE ? command.availableEndTime() : null;
    }

    private PlaceRecommendationStrategy resolvePlaceRecommendationStrategy(CreateRoomCommand command) {
        return command.placeMode() == PlaceMode.RECOMMEND ? command.placeRecommendationStrategy() : null;
    }

    private String resolveFixedPlaceName(CreateRoomCommand command) {
        return command.placeMode() == PlaceMode.FIXED ? normalizeOptional(command.fixedPlaceName()) : null;
    }

    private String resolveFixedPlaceAddress(CreateRoomCommand command) {
        return command.placeMode() == PlaceMode.FIXED ? normalizeOptional(command.fixedPlaceAddress()) : null;
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
