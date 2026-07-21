package com.moyeo.service.meeting;

import com.moyeo.domain.member.User;
import com.moyeo.domain.meeting.ParticipantType;
import com.moyeo.domain.meeting.PlaceMode;
import com.moyeo.domain.meeting.PlaceRecommendationStrategy;
import com.moyeo.domain.meeting.Meeting;
import com.moyeo.domain.meeting.MeetingParticipant;
import com.moyeo.domain.meeting.MeetingParticipantScheduleDateAvailability;
import com.moyeo.domain.meeting.MeetingParticipantScheduleAvailability;
import com.moyeo.domain.meeting.MeetingScheduleCandidate;
import com.moyeo.domain.meeting.ScheduleMode;
import com.moyeo.domain.meeting.ScheduleInputType;
import com.moyeo.global.error.CommonErrorCode;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.repository.member.UserRepository;
import com.moyeo.repository.meeting.MeetingParticipantRepository;
import com.moyeo.repository.meeting.MeetingParticipantScheduleDateAvailabilityRepository;
import com.moyeo.repository.meeting.MeetingParticipantScheduleAvailabilityRepository;
import com.moyeo.repository.meeting.MeetingRepository;
import com.moyeo.repository.meeting.MeetingScheduleCandidateRepository;
import com.moyeo.service.member.AuthenticatedMember;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingParticipantScheduleDateAvailabilityRepository meetingParticipantScheduleDateAvailabilityRepository;
    private final MeetingParticipantScheduleAvailabilityRepository meetingParticipantScheduleAvailabilityRepository;
    private final MeetingScheduleCandidateRepository meetingScheduleCandidateRepository;
    private final UserRepository userRepository;
    private final CommercialAreaCatalog commercialAreaCatalog;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final PasswordEncoder passwordEncoder;
    private final MeetingCoverStorage meetingCoverStorage;
    private final MeetingCoverProcessor meetingCoverProcessor;

    public MeetingService(
            MeetingRepository meetingRepository,
            MeetingParticipantRepository meetingParticipantRepository,
            MeetingParticipantScheduleDateAvailabilityRepository meetingParticipantScheduleDateAvailabilityRepository,
            MeetingParticipantScheduleAvailabilityRepository meetingParticipantScheduleAvailabilityRepository,
            MeetingScheduleCandidateRepository meetingScheduleCandidateRepository,
            UserRepository userRepository,
            CommercialAreaCatalog commercialAreaCatalog,
            InviteCodeGenerator inviteCodeGenerator,
            PasswordEncoder passwordEncoder,
            MeetingCoverStorage meetingCoverStorage,
            MeetingCoverProcessor meetingCoverProcessor
    ) {
        this.meetingRepository = meetingRepository;
        this.meetingParticipantRepository = meetingParticipantRepository;
        this.meetingParticipantScheduleDateAvailabilityRepository = meetingParticipantScheduleDateAvailabilityRepository;
        this.meetingParticipantScheduleAvailabilityRepository = meetingParticipantScheduleAvailabilityRepository;
        this.meetingScheduleCandidateRepository = meetingScheduleCandidateRepository;
        this.userRepository = userRepository;
        this.commercialAreaCatalog = commercialAreaCatalog;
        this.inviteCodeGenerator = inviteCodeGenerator;
        this.passwordEncoder = passwordEncoder;
        this.meetingCoverStorage = meetingCoverStorage;
        this.meetingCoverProcessor = meetingCoverProcessor;
    }

    @Transactional
    public MeetingCreateResult createMeeting(
            AuthenticatedMember hostMember,
            CreateMeetingCommand command,
            List<LocalDate> scheduleCandidateDates,
            SaveParticipationCommand participationCommand
    ) {
        return createMeeting(hostMember, command, scheduleCandidateDates, participationCommand, null);
    }

    @Transactional
    public MeetingCreateResult createMeeting(
            AuthenticatedMember hostMember,
            CreateMeetingCommand command,
            List<LocalDate> scheduleCandidateDates,
            SaveParticipationCommand participationCommand,
            MultipartFile coverImage
    ) {
        User hostUser = userRepository.findById(hostMember.userId())
                .orElseThrow(() -> new MoyeoException(CommonErrorCode.INVALID_REQUEST));

        Meeting meeting = new Meeting(
                hostUser,
                normalizeRequired(command.name()),
                normalizeOptional(command.description()),
                command.maxParticipants(),
                command.planningType(),
                command.scheduleMode(),
                command.scheduleInputType(),
                resolveFixedScheduleAt(command),
                resolveAvailableStartTime(command),
                resolveAvailableEndTime(command),
                command.placeMode(),
                resolvePlaceRecommendationStrategy(command.placeMode()),
                resolveFixedPlaceName(command),
                resolveFixedPlaceAddress(command),
                LocalDateTime.now().plusMinutes(command.deadlineMinutes()),
                inviteCodeGenerator.generate()
        );
        Meeting savedMeeting = meetingRepository.saveAndFlush(meeting);
        MeetingParticipant hostParticipant = meetingParticipantRepository.saveAndFlush(
                MeetingParticipant.host(savedMeeting, hostUser)
        );
        saveHostScheduleCandidates(savedMeeting, scheduleCandidateDates);
        SaveParticipationCommand resolvedCommand = resolveHostCreationParticipationCommand(
                savedMeeting,
                scheduleCandidateDates,
                participationCommand
        );
        saveParticipation(savedMeeting, hostParticipant, resolvedCommand);

        if (coverImage != null && !coverImage.isEmpty()) {
            saveCoverImage(savedMeeting, coverImage);
        }
        return MeetingCreateResult.from(savedMeeting);
    }

    @Transactional
    public MeetingCoverResult replaceCoverImage(Long meetingId, AuthenticatedMember member, MultipartFile coverImage) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MoyeoException(CommonErrorCode.INVALID_REQUEST));
        validateCoverModificationAuthority(meeting, member);
        String previousKey = meeting.getCoverImageKey();
        saveCoverImage(meeting, coverImage);
        deleteAfterCommit(previousKey);
        return new MeetingCoverResult(MeetingCoverUrl.from(meeting));
    }

    @Transactional
    public void deleteCoverImage(Long meetingId, AuthenticatedMember member) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MoyeoException(CommonErrorCode.INVALID_REQUEST));
        validateCoverModificationAuthority(meeting, member);
        String previousKey = meeting.getCoverImageKey();
        if (previousKey == null) {
            throw new MoyeoException(MeetingCoverErrorCode.MEETING_COVER_IMAGE_NOT_FOUND);
        }
        meeting.removeCoverImage();
        deleteAfterCommit(previousKey);
    }

    public MeetingCoverStorage.CoverObject getCoverImage(String inviteCode) {
        Meeting meeting = findMeetingByInviteCode(inviteCode);
        if (meeting.getCoverImageKey() == null) {
            throw new MoyeoException(MeetingCoverErrorCode.MEETING_COVER_IMAGE_NOT_FOUND);
        }
        return meetingCoverStorage.get(meeting.getCoverImageKey());
    }

    private void saveCoverImage(Meeting meeting, MultipartFile coverImage) {
        byte[] resized = meetingCoverProcessor.resizeToJpeg(coverImage);
        String objectKey = "meeting-covers/" + UUID.randomUUID() + ".jpg";
        meetingCoverStorage.put(objectKey, resized);
        meeting.changeCoverImageKey(objectKey);
        deleteUploadedObjectOnRollback(objectKey);
    }

    private void validateCoverModificationAuthority(Meeting meeting, AuthenticatedMember member) {
        if (!meeting.getHostUser().getId().equals(member.userId())) {
            throw new MoyeoException(MeetingCoverErrorCode.MEETING_COVER_IMAGE_MODIFICATION_FORBIDDEN);
        }
    }

    private void deleteUploadedObjectOnRollback(String objectKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deleteQuietly(objectKey);
                }
            }
        });
    }

    private void deleteAfterCommit(String objectKey) {
        if (objectKey == null) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(objectKey);
            }
        });
    }

    private void deleteQuietly(String objectKey) {
        try {
            meetingCoverStorage.delete(objectKey);
        } catch (RuntimeException ignored) {
            // 이전 이미지 정리 실패는 확정된 DB 변경을 되돌리지 않는다.
        }
    }

    public MeetingInvitationResult getInvitation(String inviteCode) {
        Meeting meeting = findMeetingByInviteCode(inviteCode);
        long participantCount = meetingParticipantRepository.countByMeetingId(meeting.getId());
        List<MeetingScheduleCandidate> scheduleCandidates = meetingScheduleCandidateRepository
                .findAllByMeetingIdOrderByCandidateDateAsc(meeting.getId());
        return MeetingInvitationResult.from(meeting, participantCount, scheduleCandidates);
    }

    public Long validateInvitationExists(String inviteCode) {
        return findMeetingByInviteCode(inviteCode).getId();
    }

    public MeetingViewResult getMeetingView(String inviteCode) {
        Meeting meeting = findMeetingByInviteCode(inviteCode);
        List<MeetingParticipant> participants = meetingParticipantRepository.findAllByMeetingIdOrderByIdAsc(meeting.getId());
        List<MeetingViewResult.Participant> participantResults = participants.stream()
                .map(participant -> new MeetingViewResult.Participant(
                        participant.getId(),
                        participant.getNickname(),
                        participant.getParticipantType().name()
                ))
                .toList();

        return new MeetingViewResult(
                meeting.getId(),
                meeting.getName(),
                meeting.getDescription(),
                MeetingCoverUrl.from(meeting),
                meeting.getPlanningType().name(),
                meeting.getScheduleMode().name(),
                meeting.getScheduleInputType().name(),
                meeting.getPlaceMode().name(),
                meeting.getPlaceRecommendationStrategy() != null ? meeting.getPlaceRecommendationStrategy().name() : null,
                meeting.getMaxParticipants(),
                participants.size(),
                meeting.getDeadlineAt(),
                remainingMinutes(meeting.getDeadlineAt()),
                participantResults
        );
    }

    public ScheduleViewResult getScheduleView(String inviteCode, String sort) {
        Meeting meeting = findMeetingByInviteCode(inviteCode);
        long participantCount = meetingParticipantRepository.countByMeetingId(meeting.getId());
        String resolvedSort = resolveScheduleSort(sort);

        if (meeting.getScheduleInputType() == ScheduleInputType.DATE_ONLY) {
            return getDateOnlyScheduleView(meeting, participantCount, resolvedSort);
        }

        List<MeetingParticipantScheduleAvailability> availabilities = meetingParticipantScheduleAvailabilityRepository
                .findAllByParticipantMeetingId(meeting.getId());
        Map<ScheduleSlot, Set<Long>> participantsBySlot = availabilities.stream()
                .flatMap(availability -> expandHourlyScheduleSlots(availability).stream())
                .collect(Collectors.groupingBy(
                        ParticipantScheduleSlot::slot,
                        Collectors.mapping(ParticipantScheduleSlot::participantId, Collectors.toSet())
                ));

        Comparator<ScheduleViewResult.Candidate> comparator = scheduleCandidateComparator(resolvedSort);
        List<ScheduleViewResult.Candidate> candidates = mergeConsecutiveScheduleCandidates(participantsBySlot)
                .stream()
                .sorted(comparator)
                .limit(3)
                .toList();

        return new ScheduleViewResult(
                meeting.getId(),
                meeting.getScheduleInputType().name(),
                resolvedSort,
                participantCount,
                candidates
        );
    }

    private ScheduleViewResult getDateOnlyScheduleView(
            Meeting meeting,
            long participantCount,
            String resolvedSort
    ) {
        List<MeetingParticipantScheduleDateAvailability> availabilities =
                meetingParticipantScheduleDateAvailabilityRepository.findAllByParticipantMeetingId(meeting.getId());
        Map<LocalDate, Set<Long>> participantsByDate = availabilities.stream()
                .collect(Collectors.groupingBy(
                        availability -> availability.getScheduleCandidate().getCandidateDate(),
                        Collectors.mapping(
                                availability -> availability.getParticipant().getId(),
                                Collectors.toSet()
                        )
                ));

        Comparator<Map.Entry<LocalDate, Set<Long>>> comparator = "EARLIEST_DATE".equals(resolvedSort)
                ? Map.Entry.comparingByKey()
                : Comparator.<Map.Entry<LocalDate, Set<Long>>>comparingInt(entry -> entry.getValue().size())
                        .reversed()
                        .thenComparing(Map.Entry::getKey);
        List<ScheduleViewResult.Candidate> candidates = participantsByDate.entrySet().stream()
                .sorted(comparator)
                .limit(3)
                .map(entry -> new ScheduleViewResult.Candidate(
                        entry.getKey(),
                        null,
                        null,
                        entry.getValue().size()
                ))
                .toList();

        return new ScheduleViewResult(
                meeting.getId(),
                meeting.getScheduleInputType().name(),
                resolvedSort,
                participantCount,
                candidates
        );
    }

    public PlaceViewResult getPlaceView(String inviteCode) {
        Meeting meeting = findMeetingByInviteCode(inviteCode);
        List<MeetingParticipant> participants = meetingParticipantRepository.findAllByMeetingIdOrderByIdAsc(meeting.getId());
        List<MeetingParticipant> departureParticipants = participants.stream()
                .filter(this::hasDeparture)
                .toList();
        List<MeetingParticipant> coordinateParticipants = departureParticipants.stream()
                .filter(this::hasCoordinates)
                .toList();
        List<PlaceViewResult.ParticipantDeparture> participantResults = participants.stream()
                .map(participant -> new PlaceViewResult.ParticipantDeparture(
                        participant.getId(),
                        participant.getNickname(),
                        participant.getParticipantType().name(),
                        participant.getDepartureName(),
                        participant.getDepartureAddress(),
                        participant.getTransportationMode() != null ? participant.getTransportationMode().name() : null
                ))
                .toList();

        String strategy = meeting.getPlaceRecommendationStrategy() != null
                ? meeting.getPlaceRecommendationStrategy().name()
                : null;
        if (meeting.getPlaceMode() != PlaceMode.RECOMMEND || meeting.getPlaceRecommendationStrategy() == null) {
            return emptyPlaceView(meeting, participants.size(), participantResults, strategy);
        }

        if (meeting.getPlaceRecommendationStrategy() == PlaceRecommendationStrategy.RANDOM) {
            List<CommercialArea> randomAreas = new ArrayList<>(commercialAreaCatalog.findAll());
            Collections.shuffle(randomAreas);
            List<PlaceViewResult.Recommendation> recommendations = randomAreas
                    .stream()
                    .limit(5)
                    .map(area -> recommendation(area, 0, null))
                    .toList();
            recommendations = rankRecommendations(recommendations);
            return new PlaceViewResult(
                    meeting.getId(),
                    strategy,
                    "RANDOM_CATALOG_PREVIEW",
                    null,
                    participants.size(),
                    participantResults,
                    recommendations
            );
        }

        if (coordinateParticipants.isEmpty()) {
            return new PlaceViewResult(
                    meeting.getId(), strategy, "COORDINATES_PENDING", null, participants.size(),
                    participantResults, List.of()
            );
        }

        PlaceViewResult.Coordinate center = averageCoordinate(coordinateParticipants);
        List<PlaceViewResult.Recommendation> recommendations = commercialAreaCatalog.findAll()
                .stream()
                .map(area -> scoreArea(area, coordinateParticipants))
                .sorted(Comparator.comparingLong(ScoredCommercialArea::score)
                        .thenComparing(scoredArea -> scoredArea.area().areaName()))
                .limit(5)
                .map(scoredArea -> recommendation(
                        scoredArea.area(),
                        0,
                        scoredArea.averageStraightDistanceMeters()
                ))
                .toList();

        recommendations = rankRecommendations(recommendations);
        return new PlaceViewResult(
                meeting.getId(),
                strategy,
                "STRAIGHT_LINE_PREVIEW",
                center,
                participants.size(),
                participantResults,
                recommendations
        );
    }

    @Transactional
    public ParticipantJoinResult joinGuest(
            String inviteCode,
            String nickname,
            String rawPassword,
            SaveParticipationCommand participationCommand
    ) {
        String normalizedNickname = normalizeRequired(nickname);
        String passwordHash = passwordEncoder.encode(rawPassword);

        Meeting meeting = prepareGuestJoinableMeeting(inviteCode, normalizedNickname);

        try {
            MeetingParticipant participant = meetingParticipantRepository.saveAndFlush(
                    MeetingParticipant.guest(meeting, normalizedNickname, passwordHash)
            );
            saveParticipation(meeting, participant, participationCommand);
            return ParticipantJoinResult.from(meeting, participant);
        } catch (DataIntegrityViolationException exception) {
            throw new MoyeoException(MeetingErrorCode.DUPLICATE_MEETING_PARTICIPANT_NICKNAME);
        }
    }

    @Transactional
    public ParticipantJoinResult joinMember(
            String inviteCode,
            AuthenticatedMember member,
            String nickname,
            SaveParticipationCommand participationCommand
    ) {
        User user = userRepository.findById(member.userId())
                .orElseThrow(() -> new MoyeoException(CommonErrorCode.INVALID_REQUEST));
        String normalizedNickname = normalizeRequired(nickname);

        Meeting meeting = prepareMemberJoinableMeeting(inviteCode);
        if (meetingParticipantRepository.existsByMeetingIdAndUserId(meeting.getId(), user.getId())) {
            throw new MoyeoException(MeetingErrorCode.DUPLICATE_MEETING_PARTICIPANT_MEMBER);
        }

        try {
            MeetingParticipant participant = meetingParticipantRepository.saveAndFlush(
                    MeetingParticipant.member(meeting, user, normalizedNickname)
            );
            saveParticipation(meeting, participant, participationCommand);
            return ParticipantJoinResult.from(meeting, participant);
        } catch (DataIntegrityViolationException exception) {
            if (meetingParticipantRepository.existsByMeetingIdAndUserId(meeting.getId(), user.getId())) {
                throw new MoyeoException(MeetingErrorCode.DUPLICATE_MEETING_PARTICIPANT_MEMBER);
            }
            throw exception;
        }
    }

    @Transactional
    public SaveParticipationResult saveParticipation(
            String inviteCode,
            Long participantId,
            SaveParticipationCommand command
    ) {
        Meeting meeting = findMeetingByInviteCode(inviteCode);
        MeetingParticipant participant = meetingParticipantRepository.findByIdAndMeetingId(participantId, meeting.getId())
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_PARTICIPANT_NOT_FOUND));

        validateJoinOpen(meeting);
        return saveParticipation(meeting, participant, command);
    }

    private SaveParticipationResult saveParticipation(
            Meeting meeting,
            MeetingParticipant participant,
            SaveParticipationCommand command
    ) {
        boolean requiresPlace = meeting.getPlaceMode() == PlaceMode.RECOMMEND;
        validateParticipationInput(meeting, command, requiresPlace);

        int scheduleAvailabilityCount = saveScheduleResponse(meeting, participant, command);
        boolean hasDeparture = false;

        if (requiresPlace) {
            SaveParticipationCommand.Departure departure = command.departure();
            participant.updateDeparture(
                    normalizeRequired(departure.name()),
                    normalizeRequired(departure.address()),
                    departure.latitude(),
                    departure.longitude(),
                    departure.transportationMode()
            );
            hasDeparture = true;
        }

        return new SaveParticipationResult(meeting.getId(), participant.getId(), scheduleAvailabilityCount, hasDeparture);
    }

    private Meeting findMeetingByInviteCode(String inviteCode) {
        return meetingRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_INVITATION_NOT_FOUND));
    }

    private Meeting findMeetingByInviteCodeForUpdate(String inviteCode) {
        return meetingRepository.findByInviteCodeForUpdate(inviteCode)
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_INVITATION_NOT_FOUND));
    }

    private Meeting prepareGuestJoinableMeeting(String inviteCode, String normalizedNickname) {
        Meeting meeting = findMeetingByInviteCodeForUpdate(inviteCode);
        validateJoinOpen(meeting);
        validateParticipantLimit(meeting);
        validateGuestNicknameAvailable(meeting, normalizedNickname);
        return meeting;
    }

    private Meeting prepareMemberJoinableMeeting(String inviteCode) {
        Meeting meeting = findMeetingByInviteCodeForUpdate(inviteCode);
        validateJoinOpen(meeting);
        validateParticipantLimit(meeting);
        return meeting;
    }

    private void validateJoinOpen(Meeting meeting) {
        if (!meeting.getDeadlineAt().isAfter(LocalDateTime.now())) {
            throw new MoyeoException(MeetingErrorCode.MEETING_PARTICIPATION_CLOSED);
        }
    }

    private void validateParticipantLimit(Meeting meeting) {
        if (meetingParticipantRepository.countByMeetingId(meeting.getId()) >= meeting.getMaxParticipants()) {
            throw new MoyeoException(MeetingErrorCode.MEETING_PARTICIPANT_LIMIT_EXCEEDED);
        }
    }

    private void validateGuestNicknameAvailable(Meeting meeting, String normalizedNickname) {
        if (meetingParticipantRepository.existsByMeetingAndNicknameAndParticipantType(
                meeting,
                normalizedNickname,
                ParticipantType.GUEST
        )) {
            throw new MoyeoException(MeetingErrorCode.DUPLICATE_MEETING_PARTICIPANT_NICKNAME);
        }
    }

    private boolean hasDeparture(MeetingParticipant participant) {
        return participant.getDepartureName() != null
                && participant.getDepartureAddress() != null
                && participant.getTransportationMode() != null;
    }

    private boolean hasCoordinates(MeetingParticipant participant) {
        return participant.getDepartureLatitude() != null && participant.getDepartureLongitude() != null;
    }

    private long remainingMinutes(LocalDateTime deadlineAt) {
        return Math.max(0, ChronoUnit.MINUTES.between(LocalDateTime.now(), deadlineAt));
    }

    private String resolveScheduleSort(String sort) {
        if ("LONGEST_MEETING".equals(sort) || "EARLIEST_DATE".equals(sort)) {
            return sort;
        }
        throw new MoyeoException(CommonErrorCode.INVALID_REQUEST);
    }

    private List<ScheduleViewResult.Candidate> mergeConsecutiveScheduleCandidates(
            Map<ScheduleSlot, Set<Long>> participantsBySlot
    ) {
        List<ScheduleSlotAvailability> slots = participantsBySlot.entrySet().stream()
                .map(entry -> new ScheduleSlotAvailability(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing((ScheduleSlotAvailability availability) -> availability.slot().candidateDate())
                        .thenComparing(availability -> availability.slot().startTime())
                        .thenComparing(availability -> availability.slot().endTime()))
                .toList();
        if (slots.isEmpty()) {
            return List.of();
        }

        List<ScheduleViewResult.Candidate> candidates = new ArrayList<>();
        ScheduleSlot currentSlot = slots.getFirst().slot();
        Set<Long> currentParticipantIds = slots.getFirst().participantIds();

        for (int index = 1; index < slots.size(); index++) {
            ScheduleSlotAvailability next = slots.get(index);
            if (canMergeScheduleSlots(currentSlot, currentParticipantIds, next)) {
                currentSlot = new ScheduleSlot(
                        currentSlot.candidateDate(),
                        currentSlot.startTime(),
                        next.slot().endTime()
                );
                continue;
            }
            candidates.add(toScheduleCandidate(currentSlot, currentParticipantIds));
            currentSlot = next.slot();
            currentParticipantIds = next.participantIds();
        }
        candidates.add(toScheduleCandidate(currentSlot, currentParticipantIds));
        return candidates;
    }

    private boolean canMergeScheduleSlots(
            ScheduleSlot currentSlot,
            Set<Long> currentParticipantIds,
            ScheduleSlotAvailability next
    ) {
        return currentSlot.candidateDate().equals(next.slot().candidateDate())
                && currentSlot.endTime().equals(next.slot().startTime())
                && currentParticipantIds.equals(next.participantIds());
    }

    private List<ParticipantScheduleSlot> expandHourlyScheduleSlots(
            MeetingParticipantScheduleAvailability availability
    ) {
        List<ParticipantScheduleSlot> slots = new ArrayList<>();
        for (LocalTime startTime = availability.getStartTime(); startTime.isBefore(availability.getEndTime()); startTime = startTime.plusHours(1)) {
            slots.add(new ParticipantScheduleSlot(
                    new ScheduleSlot(
                            availability.getScheduleCandidate().getCandidateDate(),
                            startTime,
                            startTime.plusHours(1)
                    ),
                    availability.getParticipant().getId()
            ));
        }
        return slots;
    }

    private ScheduleViewResult.Candidate toScheduleCandidate(
            ScheduleSlot slot,
            Set<Long> participantIds
    ) {
        return new ScheduleViewResult.Candidate(
                slot.candidateDate(),
                slot.startTime(),
                slot.endTime(),
                participantIds.size()
        );
    }

    private Comparator<ScheduleViewResult.Candidate> scheduleCandidateComparator(String sort) {
        if ("EARLIEST_DATE".equals(sort)) {
            return Comparator.comparing(ScheduleViewResult.Candidate::candidateDate)
                    .thenComparing(ScheduleViewResult.Candidate::startTime)
                    .thenComparing(ScheduleViewResult.Candidate::endTime)
                    .thenComparing(ScheduleViewResult.Candidate::availableParticipantCount, Comparator.reverseOrder());
        }
        return Comparator.comparing(ScheduleViewResult.Candidate::availableParticipantCount, Comparator.reverseOrder())
                .thenComparing(candidate -> ChronoUnit.MINUTES.between(candidate.startTime(), candidate.endTime()), Comparator.reverseOrder())
                .thenComparing(ScheduleViewResult.Candidate::candidateDate)
                .thenComparing(ScheduleViewResult.Candidate::startTime);
    }

    private PlaceViewResult emptyPlaceView(
            Meeting meeting,
            long participantCount,
            List<PlaceViewResult.ParticipantDeparture> participantResults,
            String strategy
    ) {
        return new PlaceViewResult(
                meeting.getId(),
                strategy,
                strategy != null ? "STRAIGHT_LINE_PREVIEW" : null,
                null,
                participantCount,
                participantResults,
                List.of()
        );
    }

    private PlaceViewResult.Coordinate averageCoordinate(List<MeetingParticipant> participants) {
        BigDecimal latitude = participants.stream()
                .map(MeetingParticipant::getDepartureLatitude)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(participants.size()), 7, RoundingMode.HALF_UP);
        BigDecimal longitude = participants.stream()
                .map(MeetingParticipant::getDepartureLongitude)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(participants.size()), 7, RoundingMode.HALF_UP);
        return new PlaceViewResult.Coordinate(latitude, longitude);
    }

    private ScoredCommercialArea scoreArea(CommercialArea area, List<MeetingParticipant> participants) {
        List<Long> distances = participants.stream()
                .map(participant -> Math.round(distanceMeters(
                        participant.getDepartureLatitude().doubleValue(),
                        participant.getDepartureLongitude().doubleValue(),
                        area.latitude().doubleValue(),
                        area.longitude().doubleValue()
                )))
                .toList();
        long averageDistanceMeters = Math.round(distances.stream().mapToLong(Long::longValue).average().orElse(0));
        long maxDistanceMeters = distances.stream().mapToLong(Long::longValue).max().orElse(0);
        return new ScoredCommercialArea(area, averageDistanceMeters, maxDistanceMeters, averageDistanceMeters + maxDistanceMeters);
    }

    private List<PlaceViewResult.Recommendation> rankRecommendations(List<PlaceViewResult.Recommendation> recommendations) {
        List<PlaceViewResult.Recommendation> ranked = new ArrayList<>();
        for (int index = 0; index < recommendations.size(); index++) {
            PlaceViewResult.Recommendation recommendation = recommendations.get(index);
            ranked.add(recommendation(
                    new CommercialArea(
                            recommendation.areaCode(),
                            recommendation.areaName(),
                            recommendation.categoryName(),
                            recommendation.latitude(),
                            recommendation.longitude(),
                            recommendation.guName(),
                            recommendation.dongName()
                    ),
                    index + 1,
                    recommendation.averageStraightDistanceMeters()
            ));
        }
        return ranked;
    }

    private PlaceViewResult.Recommendation recommendation(
            CommercialArea area,
            int rank,
            Long averageStraightDistanceMeters
    ) {
        return new PlaceViewResult.Recommendation(
                rank,
                area.areaCode(),
                area.areaName(),
                area.categoryName(),
                area.latitude(),
                area.longitude(),
                area.guName(),
                area.dongName(),
                averageStraightDistanceMeters
        );
    }

    private double distanceMeters(double latitude1, double longitude1, double latitude2, double longitude2) {
        double earthRadiusMeters = 6_371_000;
        double lat1 = Math.toRadians(latitude1);
        double lat2 = Math.toRadians(latitude2);
        double deltaLat = Math.toRadians(latitude2 - latitude1);
        double deltaLon = Math.toRadians(longitude2 - longitude1);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusMeters * c;
    }

    private void saveHostScheduleCandidates(Meeting meeting, List<LocalDate> scheduleCandidateDates) {
        boolean requiresSchedule = meeting.getScheduleMode() == ScheduleMode.VOTE;
        boolean hasScheduleCandidateDates = scheduleCandidateDates != null && !scheduleCandidateDates.isEmpty();
        if (requiresSchedule != hasScheduleCandidateDates) {
            throw new MoyeoException(MeetingErrorCode.INVALID_MEETING_PARTICIPATION_INPUT);
        }
        if (!requiresSchedule) {
            return;
        }

        List<MeetingScheduleCandidate> candidates = scheduleCandidateDates.stream()
                .distinct()
                .sorted()
                .map(candidateDate -> new MeetingScheduleCandidate(meeting, candidateDate))
                .toList();
        meetingScheduleCandidateRepository.saveAllAndFlush(candidates);
    }

    private SaveParticipationCommand resolveHostCreationParticipationCommand(
            Meeting meeting,
            List<LocalDate> scheduleCandidateDates,
            SaveParticipationCommand command
    ) {
        if (meeting.getScheduleInputType() != ScheduleInputType.DATE_ONLY) {
            return command;
        }
        if (!command.scheduleAvailableDates().isEmpty() || !command.scheduleAvailabilities().isEmpty()) {
            throw new MoyeoException(MeetingErrorCode.INVALID_MEETING_PARTICIPATION_INPUT);
        }
        return new SaveParticipationCommand(
                scheduleCandidateDates != null ? scheduleCandidateDates : List.of(),
                List.of(),
                command.departure()
        );
    }

    private void validateParticipationInput(
            Meeting meeting,
            SaveParticipationCommand command,
            boolean requiresPlace
    ) {
        boolean hasAvailableDates = command.scheduleAvailableDates() != null
                && !command.scheduleAvailableDates().isEmpty();
        boolean hasScheduleAvailabilities = command.scheduleAvailabilities() != null
                && !command.scheduleAvailabilities().isEmpty();
        boolean hasDeparture = command.departure() != null;

        boolean validScheduleInput = switch (meeting.getScheduleInputType()) {
            case DATE_ONLY -> hasAvailableDates && !hasScheduleAvailabilities;
            case DATE_AND_TIME -> !hasAvailableDates && hasScheduleAvailabilities;
            case NONE -> !hasAvailableDates && !hasScheduleAvailabilities;
        };
        if (!validScheduleInput || requiresPlace != hasDeparture) {
            throw new MoyeoException(MeetingErrorCode.INVALID_MEETING_PARTICIPATION_INPUT);
        }
    }

    private int saveScheduleResponse(
            Meeting meeting,
            MeetingParticipant participant,
            SaveParticipationCommand command
    ) {
        meetingParticipantScheduleDateAvailabilityRepository.deleteAllByParticipantId(participant.getId());
        meetingParticipantScheduleDateAvailabilityRepository.flush();
        meetingParticipantScheduleAvailabilityRepository.deleteAllByParticipantId(participant.getId());
        meetingParticipantScheduleAvailabilityRepository.flush();

        Map<LocalDate, MeetingScheduleCandidate> candidatesByDate = meetingScheduleCandidateRepository
                .findAllByMeetingIdOrderByCandidateDateAsc(meeting.getId())
                .stream()
                .collect(Collectors.toMap(MeetingScheduleCandidate::getCandidateDate, Function.identity()));

        if (meeting.getScheduleInputType() == ScheduleInputType.DATE_ONLY) {
            return saveScheduleDateAvailabilities(participant, command, candidatesByDate);
        }
        if (meeting.getScheduleInputType() == ScheduleInputType.NONE) {
            return 0;
        }

        LinkedHashSet<ScheduleSlot> slots = new LinkedHashSet<>();
        for (SaveParticipationCommand.ScheduleAvailability availability : command.scheduleAvailabilities()) {
            validateScheduleAvailability(meeting, candidatesByDate, availability);
            slots.add(new ScheduleSlot(
                    availability.candidateDate(),
                    availability.startTime(),
                    availability.endTime()
            ));
        }

        List<MeetingParticipantScheduleAvailability> entities = slots.stream()
                .map(slot -> new MeetingParticipantScheduleAvailability(
                        participant,
                        candidatesByDate.get(slot.candidateDate()),
                        slot.startTime(),
                        slot.endTime()
                ))
                .toList();
        meetingParticipantScheduleAvailabilityRepository.saveAll(entities);
        return entities.size();
    }

    private int saveScheduleDateAvailabilities(
            MeetingParticipant participant,
            SaveParticipationCommand command,
            Map<LocalDate, MeetingScheduleCandidate> candidatesByDate
    ) {
        LinkedHashSet<LocalDate> availableDates = new LinkedHashSet<>(command.scheduleAvailableDates());
        if (!candidatesByDate.keySet().containsAll(availableDates)) {
            throw new MoyeoException(MeetingErrorCode.INVALID_MEETING_PARTICIPATION_INPUT);
        }
        List<MeetingParticipantScheduleDateAvailability> entities = availableDates.stream()
                .map(candidateDate -> new MeetingParticipantScheduleDateAvailability(
                        participant,
                        candidatesByDate.get(candidateDate)
                ))
                .toList();
        meetingParticipantScheduleDateAvailabilityRepository.saveAll(entities);
        return entities.size();
    }

    private void validateScheduleAvailability(
            Meeting meeting,
            Map<LocalDate, MeetingScheduleCandidate> candidatesByDate,
            SaveParticipationCommand.ScheduleAvailability availability
    ) {
        if (!candidatesByDate.containsKey(availability.candidateDate())
                || availability.startTime() == null
                || availability.endTime() == null
                || !availability.startTime().isBefore(availability.endTime())
                || !isHourUnit(availability.startTime())
                || !isHourUnit(availability.endTime())
                || availability.startTime().isBefore(meeting.getAvailableStartTime())
                || availability.endTime().isAfter(meeting.getAvailableEndTime())) {
            throw new MoyeoException(MeetingErrorCode.INVALID_MEETING_PARTICIPATION_INPUT);
        }
    }

    private LocalDateTime resolveFixedScheduleAt(CreateMeetingCommand command) {
        return command.scheduleMode() == ScheduleMode.FIXED ? command.fixedScheduleAt() : null;
    }

    private LocalTime resolveAvailableStartTime(CreateMeetingCommand command) {
        return command.scheduleInputType() == ScheduleInputType.DATE_AND_TIME ? command.availableStartTime() : null;
    }

    private LocalTime resolveAvailableEndTime(CreateMeetingCommand command) {
        return command.scheduleInputType() == ScheduleInputType.DATE_AND_TIME ? command.availableEndTime() : null;
    }

    private PlaceRecommendationStrategy resolvePlaceRecommendationStrategy(PlaceMode placeMode) {
        return placeMode == PlaceMode.RECOMMEND ? PlaceRecommendationStrategy.MIDDLE_POINT : null;
    }

    private String resolveFixedPlaceName(CreateMeetingCommand command) {
        return command.placeMode() == PlaceMode.FIXED ? normalizeOptional(command.fixedPlaceName()) : null;
    }

    private String resolveFixedPlaceAddress(CreateMeetingCommand command) {
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

    private boolean isHourUnit(LocalTime time) {
        return time.getMinute() == 0 && time.getSecond() == 0 && time.getNano() == 0;
    }

    private record ScheduleSlot(
            LocalDate candidateDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
    }

    private record ScheduleSlotAvailability(
            ScheduleSlot slot,
            Set<Long> participantIds
    ) {
    }

    private record ParticipantScheduleSlot(
            ScheduleSlot slot,
            Long participantId
    ) {
    }

    private record ScoredCommercialArea(
            CommercialArea area,
            long averageStraightDistanceMeters,
            long maxStraightDistanceMeters,
            long score
    ) {
    }
}
