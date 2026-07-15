package com.moyeo.service.meeting;

import com.moyeo.domain.member.User;
import com.moyeo.domain.meeting.ParticipantType;
import com.moyeo.domain.meeting.PlaceMode;
import com.moyeo.domain.meeting.PlaceRecommendationStrategy;
import com.moyeo.domain.meeting.Meeting;
import com.moyeo.domain.meeting.MeetingParticipant;
import com.moyeo.domain.meeting.MeetingParticipantScheduleAvailability;
import com.moyeo.domain.meeting.MeetingScheduleCandidate;
import com.moyeo.domain.meeting.ScheduleMode;
import com.moyeo.global.error.CommonErrorCode;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.repository.member.UserRepository;
import com.moyeo.repository.meeting.MeetingParticipantRepository;
import com.moyeo.repository.meeting.MeetingParticipantScheduleAvailabilityRepository;
import com.moyeo.repository.meeting.MeetingRepository;
import com.moyeo.repository.meeting.MeetingScheduleCandidateRepository;
import com.moyeo.service.member.AuthenticatedMember;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingParticipantScheduleAvailabilityRepository meetingParticipantScheduleAvailabilityRepository;
    private final MeetingScheduleCandidateRepository meetingScheduleCandidateRepository;
    private final UserRepository userRepository;
    private final CommercialAreaCatalog commercialAreaCatalog;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final PasswordEncoder passwordEncoder;

    public MeetingService(
            MeetingRepository meetingRepository,
            MeetingParticipantRepository meetingParticipantRepository,
            MeetingParticipantScheduleAvailabilityRepository meetingParticipantScheduleAvailabilityRepository,
            MeetingScheduleCandidateRepository meetingScheduleCandidateRepository,
            UserRepository userRepository,
            CommercialAreaCatalog commercialAreaCatalog,
            InviteCodeGenerator inviteCodeGenerator,
            PasswordEncoder passwordEncoder
    ) {
        this.meetingRepository = meetingRepository;
        this.meetingParticipantRepository = meetingParticipantRepository;
        this.meetingParticipantScheduleAvailabilityRepository = meetingParticipantScheduleAvailabilityRepository;
        this.meetingScheduleCandidateRepository = meetingScheduleCandidateRepository;
        this.userRepository = userRepository;
        this.commercialAreaCatalog = commercialAreaCatalog;
        this.inviteCodeGenerator = inviteCodeGenerator;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public MeetingCreateResult createMeeting(
            AuthenticatedMember hostMember,
            CreateMeetingCommand command
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
        Meeting savedMeeting = meetingRepository.saveAndFlush(meeting);
        saveScheduleCandidates(savedMeeting, command);

        MeetingParticipant hostParticipant = meetingParticipantRepository.saveAndFlush(
                MeetingParticipant.host(
                        savedMeeting,
                        hostUser,
                        normalizeOptional(command.hostDepartureName()),
                        normalizeOptional(command.hostDepartureAddress()),
                        command.hostDepartureLatitude(),
                        command.hostDepartureLongitude(),
                        command.hostTransportationMode()
                )
        );

        List<MeetingScheduleCandidate> scheduleCandidates = meetingScheduleCandidateRepository
                .findAllByMeetingIdOrderByCandidateDateAsc(savedMeeting.getId());
        return MeetingCreateResult.from(savedMeeting, hostParticipant, scheduleCandidates);
    }

    public MeetingInvitationResult getInvitation(String inviteCode) {
        Meeting meeting = findMeetingByInviteCode(inviteCode);
        long participantCount = meetingParticipantRepository.countByMeetingId(meeting.getId());
        List<MeetingScheduleCandidate> scheduleCandidates = meetingScheduleCandidateRepository
                .findAllByMeetingIdOrderByCandidateDateAsc(meeting.getId());
        return MeetingInvitationResult.from(meeting, participantCount, scheduleCandidates);
    }

    public MeetingViewResult getMeetingView(String inviteCode) {
        Meeting meeting = findMeetingByInviteCode(inviteCode);
        List<MeetingParticipant> participants = meetingParticipantRepository.findAllByMeetingIdOrderByIdAsc(meeting.getId());
        Set<Long> scheduleRespondedParticipantIds = meetingParticipantScheduleAvailabilityRepository
                .findAllByParticipantMeetingId(meeting.getId())
                .stream()
                .map(availability -> availability.getParticipant().getId())
                .collect(Collectors.toSet());

        boolean requiresSchedule = meeting.getScheduleMode() == ScheduleMode.VOTE;
        boolean requiresPlace = meeting.getPlaceMode() == PlaceMode.RECOMMEND;

        List<MeetingViewResult.ParticipantStatus> participantStatuses = participants.stream()
                .map(participant -> {
                    boolean scheduleResponded = scheduleRespondedParticipantIds.contains(participant.getId());
                    boolean placeResponded = hasDeparture(participant);
                    boolean responseCompleted = (!requiresSchedule || scheduleResponded)
                            && (!requiresPlace || placeResponded);
                    return new MeetingViewResult.ParticipantStatus(
                            participant.getId(),
                            participant.getNickname(),
                            participant.getParticipantType().name(),
                            scheduleResponded,
                            placeResponded,
                            responseCompleted
                    );
                })
                .toList();
        long respondedParticipantCount = participantStatuses.stream()
                .filter(MeetingViewResult.ParticipantStatus::responseCompleted)
                .count();

        return new MeetingViewResult(
                meeting.getId(),
                meeting.getName(),
                meeting.getDescription(),
                meeting.getPlanningType().name(),
                meeting.getScheduleMode().name(),
                meeting.getPlaceMode().name(),
                meeting.getPlaceRecommendationStrategy() != null ? meeting.getPlaceRecommendationStrategy().name() : null,
                meeting.getMaxParticipants(),
                participants.size(),
                meeting.getDeadlineAt(),
                remainingMinutes(meeting.getDeadlineAt()),
                respondedParticipantCount,
                responseRate(respondedParticipantCount, participants.size()),
                participantStatuses
        );
    }

    public ScheduleViewResult getScheduleView(String inviteCode, String sort) {
        Meeting meeting = findMeetingByInviteCode(inviteCode);
        long participantCount = meetingParticipantRepository.countByMeetingId(meeting.getId());
        String resolvedSort = resolveScheduleSort(sort);

        List<MeetingParticipantScheduleAvailability> availabilities = meetingParticipantScheduleAvailabilityRepository
                .findAllByParticipantMeetingId(meeting.getId());
        Map<ScheduleSlot, Set<Long>> participantsBySlot = availabilities.stream()
                .flatMap(availability -> expandHourlyScheduleSlots(availability).stream())
                .collect(Collectors.groupingBy(
                        ParticipantScheduleSlot::slot,
                        Collectors.mapping(ParticipantScheduleSlot::participantId, Collectors.toSet())
                ));

        Comparator<ScheduleViewResult.Candidate> comparator = scheduleCandidateComparator(resolvedSort);
        List<ScheduleViewResult.Candidate> candidates = mergeConsecutiveScheduleCandidates(participantsBySlot, participantCount)
                .stream()
                .sorted(comparator)
                .limit(5)
                .toList();

        long respondedParticipantCount = availabilities.stream()
                .map(availability -> availability.getParticipant().getId())
                .distinct()
                .count();

        return new ScheduleViewResult(
                meeting.getId(),
                resolvedSort,
                participantCount,
                respondedParticipantCount,
                candidates,
                candidates.isEmpty() ? "겹치는 시간이 없어요." : null
        );
    }

    public PlaceViewResult getPlaceView(String inviteCode) {
        Meeting meeting = findMeetingByInviteCode(inviteCode);
        List<MeetingParticipant> participants = meetingParticipantRepository.findAllByMeetingIdOrderByIdAsc(meeting.getId());
        List<MeetingParticipant> departureParticipants = participants.stream()
                .filter(this::hasDeparture)
                .toList();
        List<PlaceViewResult.ParticipantDepartureStatus> participantStatuses = participants.stream()
                .map(participant -> new PlaceViewResult.ParticipantDepartureStatus(
                        participant.getId(),
                        participant.getNickname(),
                        participant.getParticipantType().name(),
                        hasDeparture(participant),
                        participant.getDepartureName(),
                        participant.getDepartureAddress(),
                        participant.getTransportationMode() != null ? participant.getTransportationMode().name() : null
                ))
                .toList();

        String strategy = meeting.getPlaceRecommendationStrategy() != null
                ? meeting.getPlaceRecommendationStrategy().name()
                : null;
        if (meeting.getPlaceMode() != PlaceMode.RECOMMEND || meeting.getPlaceRecommendationStrategy() == null) {
            return emptyPlaceView(meeting, participants.size(), departureParticipants.size(), participantStatuses, strategy);
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
                    departureParticipants.size(),
                    participantStatuses,
                    recommendations,
                    recommendations.isEmpty() ? "추천할 장소가 없어요." : null
            );
        }

        if (departureParticipants.isEmpty()) {
            return emptyPlaceView(meeting, participants.size(), 0, participantStatuses, strategy);
        }

        PlaceViewResult.Coordinate center = averageCoordinate(departureParticipants);
        List<PlaceViewResult.Recommendation> recommendations = commercialAreaCatalog.findAll()
                .stream()
                .map(area -> scoreArea(area, departureParticipants))
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
                departureParticipants.size(),
                participantStatuses,
                recommendations,
                recommendations.isEmpty() ? "추천할 장소가 없어요." : null
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
        boolean requiresSchedule = meeting.getScheduleMode() == ScheduleMode.VOTE;
        boolean requiresPlace = meeting.getPlaceMode() == PlaceMode.RECOMMEND;
        validateParticipationInput(command, requiresSchedule, requiresPlace);

        int scheduleAvailabilityCount = saveScheduleAvailabilities(meeting, participant, command);
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
        return participant.getDepartureLatitude() != null
                && participant.getDepartureLongitude() != null
                && participant.getDepartureAddress() != null;
    }

    private long remainingMinutes(LocalDateTime deadlineAt) {
        return Math.max(0, ChronoUnit.MINUTES.between(LocalDateTime.now(), deadlineAt));
    }

    private double responseRate(long respondedParticipantCount, long participantCount) {
        if (participantCount == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf((double) respondedParticipantCount / participantCount)
                .setScale(4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String resolveScheduleSort(String sort) {
        if ("LONGEST_MEETING".equals(sort) || "EARLIEST_DATE".equals(sort)) {
            return sort;
        }
        throw new MoyeoException(CommonErrorCode.INVALID_REQUEST);
    }

    private List<ScheduleViewResult.Candidate> mergeConsecutiveScheduleCandidates(
            Map<ScheduleSlot, Set<Long>> participantsBySlot,
            long participantCount
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
            candidates.add(toScheduleCandidate(currentSlot, currentParticipantIds, participantCount));
            currentSlot = next.slot();
            currentParticipantIds = next.participantIds();
        }
        candidates.add(toScheduleCandidate(currentSlot, currentParticipantIds, participantCount));
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
            Set<Long> participantIds,
            long participantCount
    ) {
        return new ScheduleViewResult.Candidate(
                slot.candidateDate(),
                slot.startTime(),
                slot.endTime(),
                participantIds.size(),
                participantCount
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
            long departureRespondedParticipantCount,
            List<PlaceViewResult.ParticipantDepartureStatus> participantStatuses,
            String strategy
    ) {
        return new PlaceViewResult(
                meeting.getId(),
                strategy,
                strategy != null ? "STRAIGHT_LINE_PREVIEW" : null,
                null,
                participantCount,
                departureRespondedParticipantCount,
                participantStatuses,
                List.of(),
                "추천할 장소가 없어요."
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

    private void saveScheduleCandidates(Meeting meeting, CreateMeetingCommand command) {
        if (command.scheduleMode() == ScheduleMode.VOTE) {
            List<MeetingScheduleCandidate> candidates = command.scheduleCandidateDates().stream()
                    .distinct()
                    .sorted()
                    .map(candidateDate -> new MeetingScheduleCandidate(meeting, candidateDate))
                    .toList();
            meetingScheduleCandidateRepository.saveAll(candidates);
        }
    }

    private void validateParticipationInput(
            SaveParticipationCommand command,
            boolean requiresSchedule,
            boolean requiresPlace
    ) {
        boolean hasScheduleAvailabilities = command.scheduleAvailabilities() != null
                && !command.scheduleAvailabilities().isEmpty();
        boolean hasDeparture = command.departure() != null;

        if (requiresSchedule != hasScheduleAvailabilities || requiresPlace != hasDeparture) {
            throw new MoyeoException(MeetingErrorCode.INVALID_MEETING_PARTICIPATION_INPUT);
        }
    }

    private int saveScheduleAvailabilities(
            Meeting meeting,
            MeetingParticipant participant,
            SaveParticipationCommand command
    ) {
        meetingParticipantScheduleAvailabilityRepository.deleteAllByParticipantId(participant.getId());
        meetingParticipantScheduleAvailabilityRepository.flush();

        if (meeting.getScheduleMode() != ScheduleMode.VOTE) {
            return 0;
        }

        Map<LocalDate, MeetingScheduleCandidate> candidatesByDate = meetingScheduleCandidateRepository
                .findAllByMeetingIdOrderByCandidateDateAsc(meeting.getId())
                .stream()
                .collect(Collectors.toMap(MeetingScheduleCandidate::getCandidateDate, Function.identity()));

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
        return command.scheduleMode() == ScheduleMode.VOTE ? command.availableStartTime() : null;
    }

    private LocalTime resolveAvailableEndTime(CreateMeetingCommand command) {
        return command.scheduleMode() == ScheduleMode.VOTE ? command.availableEndTime() : null;
    }

    private PlaceRecommendationStrategy resolvePlaceRecommendationStrategy(CreateMeetingCommand command) {
        return command.placeMode() == PlaceMode.RECOMMEND ? command.placeRecommendationStrategy() : null;
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
