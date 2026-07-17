package com.moyeo.controller.meeting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyeo.repository.meeting.MeetingParticipantRepository;
import com.moyeo.repository.meeting.MeetingParticipantScheduleAvailabilityRepository;
import com.moyeo.service.meeting.MeetingService;
import com.moyeo.service.meeting.SaveParticipationCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class MeetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MeetingParticipantRepository meetingParticipantRepository;

    @Autowired
    private MeetingParticipantScheduleAvailabilityRepository meetingParticipantScheduleAvailabilityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MeetingService meetingService;

    @Test
    void createMeetingReturnsInviteCodeAndHostParticipant() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost1", "host1");

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultCreateMeetingRequest(6))))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.meetingId").isNumber())
                .andExpect(jsonPath("$.name").value("weekend-meeting"))
                .andExpect(jsonPath("$.description").value("dinner together"))
                .andExpect(jsonPath("$.maxParticipants").value(6))
                .andExpect(jsonPath("$.planningType").value("SCHEDULE_AND_PLACE"))
                .andExpect(jsonPath("$.scheduleMode").value("VOTE"))
                .andExpect(jsonPath("$.scheduleCandidateDates[0]").value("2026-07-01"))
                .andExpect(jsonPath("$.availableStartTime").value("09:00:00"))
                .andExpect(jsonPath("$.availableEndTime").value("18:00:00"))
                .andExpect(jsonPath("$.placeMode").value("RECOMMEND"))
                .andExpect(jsonPath("$.placeRecommendationStrategy").value("MIDDLE_POINT"))
                .andExpect(jsonPath("$.deadlineAt").isString())
                .andExpect(jsonPath("$.inviteCode").isString())
                .andExpect(jsonPath("$.invitePath").isString())
                .andExpect(jsonPath("$.hostDepartureName").value("company"))
                .andExpect(jsonPath("$.hostDepartureAddress").value("Seoul Gangnam"))
                .andExpect(jsonPath("$.hostDepartureLatitude").value(37.498095))
                .andExpect(jsonPath("$.hostDepartureLongitude").value(127.027610))
                .andExpect(jsonPath("$.hostTransportationMode").value("PUBLIC_TRANSIT"))
                .andExpect(jsonPath("$.hostParticipantId").isNumber());
    }

    @Test
    void createScheduleMeetingSavesHostAvailabilityForEveryCandidateDate() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost35", "host35");

        String response = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultCreateMeetingRequest(6))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long hostParticipantId = objectMapper.readTree(response).get("hostParticipantId").asLong();
        assertThat(meetingParticipantScheduleAvailabilityRepository.countByParticipantId(hostParticipantId)).isEqualTo(2);
    }

    @Test
    void createMeetingRequiresAccessToken() throws Exception {
        mockMvc.perform(post("/api/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "weekend-meeting",
                                "maxParticipants", 6
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void createMeetingValidatesRequest() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost2", "host2");

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCreateMeetingRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void createMeetingRejectsMaxParticipantsBelowMinimum() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-min-participants", "host-min-participants");

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultCreateMeetingRequest(1))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void createMeetingRejectsMaxParticipantsAboveMaximum() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-max-participants", "host-max-participants");

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultCreateMeetingRequest(21))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void createMeetingRejectsNonHourUnitScheduleTime() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost10", "host10");

        CreateMeetingRequest request = new CreateMeetingRequest(
                "weekend",
                "dinner",
                6,
                com.moyeo.domain.meeting.PlanningType.SCHEDULE_ONLY,
                List.of(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)),
                LocalTime.of(18, 30),
                LocalTime.of(22, 0),
                null,
                null,
                null,
                null,
                null,
                null,
                1440
        );

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void createMeetingRejectsNonTenMinuteDeadline() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost11", "host11");

        CreateMeetingRequest request = new CreateMeetingRequest(
                "weekend",
                "dinner",
                6,
                com.moyeo.domain.meeting.PlanningType.SCHEDULE_AND_PLACE,
                List.of(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)),
                LocalTime.of(18, 0),
                LocalTime.of(22, 0),
                com.moyeo.domain.meeting.PlaceRecommendationStrategy.MIDDLE_POINT,
                "company",
                "Seoul Gangnam",
                BigDecimal.valueOf(37.498095),
                BigDecimal.valueOf(127.027610),
                com.moyeo.domain.meeting.TransportationMode.PUBLIC_TRANSIT,
                15
        );

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void createMeetingRejectsMiddlePointWithoutHostDepartureSnapshot() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost17", "host17");

        CreateMeetingRequest request = new CreateMeetingRequest(
                "weekend",
                "dinner",
                6,
                com.moyeo.domain.meeting.PlanningType.PLACE_ONLY,
                null,
                null,
                null,
                com.moyeo.domain.meeting.PlaceRecommendationStrategy.MIDDLE_POINT,
                null,
                "Seoul Gangnam",
                BigDecimal.valueOf(37.498095),
                BigDecimal.valueOf(127.027610),
                com.moyeo.domain.meeting.TransportationMode.PUBLIC_TRANSIT,
                1440
        );

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void createMeetingSupportsPlaceOnlyPlanning() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost13", "host13");

        CreateMeetingRequest request = new CreateMeetingRequest(
                "weekend",
                "dinner",
                6,
                com.moyeo.domain.meeting.PlanningType.PLACE_ONLY,
                null,
                null,
                null,
                com.moyeo.domain.meeting.PlaceRecommendationStrategy.RANDOM,
                null,
                null,
                null,
                null,
                null,
                1440
        );

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planningType").value("PLACE_ONLY"))
                .andExpect(jsonPath("$.scheduleMode").value("NONE"))
                .andExpect(jsonPath("$.scheduleCandidateDates").isEmpty())
                .andExpect(jsonPath("$.availableStartTime").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.availableEndTime").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.placeMode").value("RECOMMEND"))
                .andExpect(jsonPath("$.placeRecommendationStrategy").value("RANDOM"));
    }

    @Test
    void createMeetingRemovesDuplicatedScheduleCandidateDatesAndSortsThem() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost14", "host14");

        CreateMeetingRequest request = new CreateMeetingRequest(
                "weekend",
                "dinner",
                6,
                com.moyeo.domain.meeting.PlanningType.SCHEDULE_ONLY,
                List.of(LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1)),
                LocalTime.of(18, 0),
                LocalTime.of(22, 0),
                null,
                null,
                null,
                null,
                null,
                null,
                1440
        );

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scheduleCandidateDates.length()").value(2))
                .andExpect(jsonPath("$.scheduleCandidateDates[0]").value("2026-07-01"))
                .andExpect(jsonPath("$.scheduleCandidateDates[1]").value("2026-07-02"));
    }

    @Test
    void getInvitationReturnsMeetingInfo() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost3", "host3", 6);

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}", inviteCode))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.meetingId").isNumber())
                .andExpect(jsonPath("$.name").value("weekend-meeting"))
                .andExpect(jsonPath("$.maxParticipants").value(6))
                .andExpect(jsonPath("$.planningType").value("SCHEDULE_AND_PLACE"))
                .andExpect(jsonPath("$.scheduleMode").value("VOTE"))
                .andExpect(jsonPath("$.scheduleCandidateDates[0]").value("2026-07-01"))
                .andExpect(jsonPath("$.placeMode").value("RECOMMEND"))
                .andExpect(jsonPath("$.placeRecommendationStrategy").value("MIDDLE_POINT"))
                .andExpect(jsonPath("$.deadlineAt").isString())
                .andExpect(jsonPath("$.participantCount").value(1))
                .andExpect(jsonPath("$.hostNickname").value("host3"))
                .andExpect(jsonPath("$.participationStatus.canJoin").value(true))
                .andExpect(jsonPath("$.participationStatus.reason").value("AVAILABLE"))
                .andExpect(jsonPath("$.participationStatus.message").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void getInvitationRejectsUnknownInviteCode() throws Exception {
        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}", "UNKNOWN123"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MEETING_INVITATION_NOT_FOUND"));
    }

    @Test
    void getInvitationReturnsParticipantLimitExceededStatus() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost15", "host15", 2);
        joinGuest(inviteCode, "guest-limit");

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(2))
                .andExpect(jsonPath("$.participationStatus.canJoin").value(false))
                .andExpect(jsonPath("$.participationStatus.reason").value("PARTICIPANT_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.participationStatus.message").value("\uBAA8\uC778 \uC778\uC6D0\uC774 \uBAA8\uB450 \uCC3C\uC5B4\uC694. \uC544\uC27D\uC9C0\uB9CC \uD604\uC7AC\uB294 \uB354 \uC774\uC0C1 \uCC38\uC5EC\uD560 \uC218 \uC5C6\uC5B4\uC694."));
    }

    @Test
    void getInvitationReturnsDeadlinePassedStatusBeforeParticipantLimitStatus() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost16", "host16", 2);
        joinGuest(inviteCode, "guest-deadline-status");
        jdbcTemplate.update("update meetings set deadline_at = dateadd('second', -1, current_timestamp) where invite_code = ?", inviteCode);

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(2))
                .andExpect(jsonPath("$.participationStatus.canJoin").value(false))
                .andExpect(jsonPath("$.participationStatus.reason").value("DEADLINE_PASSED"))
                .andExpect(jsonPath("$.participationStatus.message").value("\uAE30\uD55C\uC774 \uC9C0\uB09C \uBAA8\uC784\uC774\uC5D0\uC694. \uC544\uC27D\uC9C0\uB9CC \uD604\uC7AC\uB294 \uB354 \uC774\uC0C1 \uCC38\uC5EC\uD560 \uC218 \uC5C6\uC5B4\uC694."));
    }

    @Test
    void joinGuestCreatesGuestParticipant() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost4", "host4", 6);

        String response = mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultGuestJoinRequest("guest", "guestpass123"))))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.meetingId").isNumber())
                .andExpect(jsonPath("$.participantId").isNumber())
                .andExpect(jsonPath("$.nickname").value("guest"))
                .andExpect(jsonPath("$.participantType").value("GUEST"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long participantId = objectMapper.readTree(response).get("participantId").asLong();
        var participant = meetingParticipantRepository.findById(participantId).orElseThrow();
        assertThat(participant.getPasswordHash()).isNotEqualTo("guestpass123");
        assertThat(passwordEncoder.matches("guestpass123", participant.getPasswordHash())).isTrue();
        assertThat(participant.getDepartureName()).isEqualTo("company");
        assertThat(meetingParticipantScheduleAvailabilityRepository.countByParticipantId(participantId)).isEqualTo(1);
    }

    @Test
    void joinGuestRejectsInvalidParticipationAndRollsBackParticipantCreation() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost-invalid", "host-invalid", 6);

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", "invalid-guest",
                                "password", "guestpass123",
                                "scheduleAvailabilities", List.of(Map.of(
                                        "candidateDate", "2026-07-01",
                                        "startTime", "08:00",
                                        "endTime", "09:00"
                                )),
                                "departure", Map.of(
                                        "name", "company",
                                        "address", "Seoul Gangnam",
                                        "latitude", 37.498095,
                                        "longitude", 127.027610,
                                        "transportationMode", "PUBLIC_TRANSIT"
                                )
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MEETING_PARTICIPATION_INPUT"));

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from meeting_participants where meeting_id = (select id from meetings where invite_code = ?)",
                Long.class,
                inviteCode
        )).isEqualTo(1L);
    }

    @Test
    void joinGuestRejectsDuplicatedNicknameInSameMeeting() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost5", "host5", 6);
        joinGuest(inviteCode, "duplicated-guest");

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultGuestJoinRequest("duplicated-guest", "guestpass123"))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("DUPLICATE_MEETING_PARTICIPANT_NICKNAME"));
    }

    @Test
    void joinGuestAllowsMultipleGuestsWithNullUserId() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost9", "host9", 6);

        joinGuest(inviteCode, "guest-null-user-1");
        joinGuest(inviteCode, "guest-null-user-2");

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(3));
    }

    @Test
    void joinGuestAllowsHostNicknameInSameMeeting() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost6", "host-nickname", 6);

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultGuestJoinRequest("host-nickname", "guestpass123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nickname").value("host-nickname"))
                .andExpect(jsonPath("$.participantType").value("GUEST"));
    }

    @Test
    void joinGuestRejectsExceededParticipantLimit() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost7", "host7", 2);
        joinGuest(inviteCode, "guest-limit-existing");

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultGuestJoinRequest("guest-limit-new", "guestpass123"))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MEETING_PARTICIPANT_LIMIT_EXCEEDED"));
    }

    @Test
    void joinGuestRejectsClosedMeetingParticipation() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost12", "host12", 6);
        jdbcTemplate.update("update meetings set deadline_at = dateadd('second', -1, current_timestamp) where invite_code = ?", inviteCode);

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultGuestJoinRequest("guest-deadline", "guestpass123"))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MEETING_PARTICIPATION_CLOSED"));
    }

    @Test
    void joinGuestValidatesRequest() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost8", "host8", 6);

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", "",
                                "password", "short"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void joinMemberCreatesMemberParticipant() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost22", "host22", 6);
        String memberToken = signupAndGetAccessToken("memberjoin1", "default-member");

        String response = mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/members", inviteCode)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultMemberJoinRequest("meeting-member"))))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.participantId").isNumber())
                .andExpect(jsonPath("$.nickname").value("meeting-member"))
                .andExpect(jsonPath("$.participantType").value("MEMBER"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long participantId = objectMapper.readTree(response).get("participantId").asLong();
        var participant = meetingParticipantRepository.findById(participantId).orElseThrow();
        assertThat(participant.getPasswordHash()).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from meeting_participants where id = ? and user_id is not null and participant_type = 'MEMBER'",
                Long.class,
                participantId
        )).isEqualTo(1L);
    }

    @Test
    void joinMemberRequiresAccessToken() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost23", "host23", 6);

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/members", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultMemberJoinRequest("meeting-member"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void joinMemberRejectsSameMemberInSameMeeting() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost24", "host24", 6);
        String memberToken = signupAndGetAccessToken("memberjoin2", "member2");
        joinMember(inviteCode, memberToken, "meeting-member1");

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/members", inviteCode)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultMemberJoinRequest("meeting-member2"))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("DUPLICATE_MEETING_PARTICIPANT_MEMBER"));
    }

    @Test
    void joinMemberAllowsGuestNicknameInSameMeeting() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost25", "host25", 6);
        String memberToken = signupAndGetAccessToken("memberjoin3", "member3");
        joinGuest(inviteCode, "duplicated-name");

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/members", inviteCode)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultMemberJoinRequest("duplicated-name"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nickname").value("duplicated-name"))
                .andExpect(jsonPath("$.participantType").value("MEMBER"));
    }

    @Test
    void joinMemberRejectsHostUserInSameMeeting() throws Exception {
        String hostToken = signupAndGetAccessToken("meetinghost26", "host26");
        String inviteCode = createMeetingAndGetInviteCode(hostToken, defaultCreateMeetingRequest(6));

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/members", inviteCode)
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultMemberJoinRequest("host-as-member"))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("DUPLICATE_MEETING_PARTICIPANT_MEMBER"));
    }

    @Test
    @Disabled("참여 생성과 상세 정보 저장은 POST 참여 API로 통합됨")
    void saveParticipationStoresScheduleAvailabilitiesAndDeparture() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost18", "host18", 6);
        Long participantId = joinGuestAndGetParticipantId(inviteCode, "guest-participation");

        mockMvc.perform(put("/api/meetings/invitations/{inviteCode}/participants/{participantId}/participation", inviteCode, participantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "scheduleAvailabilities", List.of(
                                        Map.of(
                                                "candidateDate", "2026-07-01",
                                                "startTime", "09:00",
                                                "endTime", "10:00"
                                        ),
                                        Map.of(
                                                "candidateDate", "2026-07-01",
                                                "startTime", "10:00",
                                                "endTime", "11:00"
                                        )
                                ),
                                "departure", Map.of(
                                        "name", "company",
                                        "address", "Seoul Gangnam",
                                        "latitude", 37.498095,
                                        "longitude", 127.027610,
                                        "transportationMode", "PUBLIC_TRANSIT"
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantId").value(participantId))
                .andExpect(jsonPath("$.scheduleAvailabilityCount").value(2))
                .andExpect(jsonPath("$.hasDeparture").value(true));

        var participant = meetingParticipantRepository.findById(participantId).orElseThrow();
        assertThat(participant.getDepartureName()).isEqualTo("company");
        assertThat(participant.getDepartureAddress()).isEqualTo("Seoul Gangnam");
        assertThat(participant.getTransportationMode()).isEqualTo(com.moyeo.domain.meeting.TransportationMode.PUBLIC_TRANSIT);
        assertThat(meetingParticipantScheduleAvailabilityRepository.countByParticipantId(participantId)).isEqualTo(2);
    }

    @Test
    @Disabled("참여 생성과 상세 정보 저장은 POST 참여 API로 통합됨")
    void saveParticipationReplacesPreviousScheduleAvailabilities() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost19", "host19", 6);
        Long participantId = joinGuestAndGetParticipantId(inviteCode, "guest-replace");

        saveDefaultParticipation(inviteCode, participantId, "09:00", "10:00");
        saveDefaultParticipation(inviteCode, participantId, "11:00", "12:00");

        assertThat(meetingParticipantScheduleAvailabilityRepository.countByParticipantId(participantId)).isEqualTo(1);
    }

    @Test
    @Disabled("참여 생성과 상세 정보 저장은 POST 참여 API로 통합됨")
    void saveParticipationRejectsOutOfRangeScheduleAvailability() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost20", "host20", 6);
        Long participantId = joinGuestAndGetParticipantId(inviteCode, "guest-invalid-time");

        mockMvc.perform(put("/api/meetings/invitations/{inviteCode}/participants/{participantId}/participation", inviteCode, participantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "scheduleAvailabilities", List.of(Map.of(
                                        "candidateDate", "2026-07-01",
                                        "startTime", "08:00",
                                        "endTime", "09:00"
                                )),
                                "departure", Map.of(
                                        "name", "company",
                                        "address", "Seoul Gangnam",
                                        "latitude", 37.498095,
                                        "longitude", 127.027610,
                                        "transportationMode", "PUBLIC_TRANSIT"
                                )
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_MEETING_PARTICIPATION_INPUT"));
    }

    @Test
    @Disabled("참여 생성과 상세 정보 저장은 POST 참여 API로 통합됨")
    void saveParticipationRejectsDepartureForScheduleOnlyMeeting() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode(
                "meetinghost21",
                "host21",
                new CreateMeetingRequest(
                        "schedule",
                        "schedule only",
                        6,
                        com.moyeo.domain.meeting.PlanningType.SCHEDULE_ONLY,
                        List.of(LocalDate.of(2026, 7, 1)),
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        1440
                )
        );
        Long participantId = joinGuestAndGetParticipantId(inviteCode, "guest-schedule-only");

        mockMvc.perform(put("/api/meetings/invitations/{inviteCode}/participants/{participantId}/participation", inviteCode, participantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "scheduleAvailabilities", List.of(Map.of(
                                        "candidateDate", "2026-07-01",
                                        "startTime", "09:00",
                                        "endTime", "10:00"
                                )),
                                "departure", Map.of(
                                        "name", "company",
                                        "address", "Seoul Gangnam",
                                        "latitude", 37.498095,
                                        "longitude", 127.027610,
                                        "transportationMode", "PUBLIC_TRANSIT"
                                )
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_MEETING_PARTICIPATION_INPUT"));
    }

    @Test
    void getMeetingViewReturnsResponseProgressAndParticipantStatuses() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost29", "host29", 6);
        Long participantId = joinGuestAndGetParticipantId(inviteCode, "guest-view");
        saveDefaultParticipation(inviteCode, participantId, "09:00", "10:00");

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view", inviteCode))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("weekend-meeting"))
                .andExpect(jsonPath("$.maxParticipants").value(6))
                .andExpect(jsonPath("$.participantCount").value(2))
                .andExpect(jsonPath("$.respondedParticipantCount").value(2))
                .andExpect(jsonPath("$.responseRate").value(1.0))
                .andExpect(jsonPath("$.participants[0].participantType").value("HOST"))
                .andExpect(jsonPath("$.participants[0].scheduleResponded").value(true))
                .andExpect(jsonPath("$.participants[0].responseCompleted").value(true))
                .andExpect(jsonPath("$.participants[1].nickname").value("guest-view"))
                .andExpect(jsonPath("$.participants[1].responseCompleted").value(true));
    }

    @Test
    void getScheduleViewReturnsTopAvailabilitySlots() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost30", "host30", 6);
        Long firstParticipantId = joinGuestAndGetParticipantId(inviteCode, "guest-schedule-view-1");
        Long secondParticipantId = joinGuestAndGetParticipantId(inviteCode, "guest-schedule-view-2");
        saveDefaultParticipation(inviteCode, firstParticipantId, "09:00", "10:00");
        saveDefaultParticipation(inviteCode, secondParticipantId, "09:00", "10:00");

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/schedules", inviteCode)
                        .param("sort", "LONGEST_MEETING"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sort").value("LONGEST_MEETING"))
                .andExpect(jsonPath("$.participantCount").value(3))
                .andExpect(jsonPath("$.respondedParticipantCount").value(3))
                .andExpect(jsonPath("$.candidates[0].candidateDate").value("2026-07-01"))
                .andExpect(jsonPath("$.candidates[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$.candidates[0].availableParticipantCount").value(3))
                .andExpect(jsonPath("$.emptyMessage").doesNotExist());
    }

    @Test
    void getScheduleViewMergesConsecutiveSlotsForSameParticipants() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost32", "host32", 6);
        Long firstParticipantId = joinGuestAndGetParticipantId(inviteCode, "guest-schedule-merge-1");
        Long secondParticipantId = joinGuestAndGetParticipantId(inviteCode, "guest-schedule-merge-2");
        saveDefaultParticipation(inviteCode, firstParticipantId, "09:00", "11:00");
        saveDefaultParticipation(inviteCode, secondParticipantId, List.of(
                scheduleAvailability("09:00", "10:00"),
                scheduleAvailability("10:00", "11:00")
        ));

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/schedules", inviteCode)
                        .param("sort", "LONGEST_MEETING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$.candidates[0].endTime").value("11:00:00"))
                .andExpect(jsonPath("$.candidates[0].availableParticipantCount").value(3));
    }

    @Test
    void getScheduleViewRejectsUnsupportedSort() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost33", "host33", 6);

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/schedules", inviteCode)
                        .param("sort", "LATEST_DATE"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"));
    }

    @Test
    void getPlaceViewReturnsStraightLineCommercialAreaPreview() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost31", "host31", 6);
        Long participantId = joinGuestAndGetParticipantId(inviteCode, "guest-place-view");
        saveDefaultParticipation(inviteCode, participantId, "09:00", "10:00");

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/places", inviteCode))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.placeRecommendationStrategy").value("MIDDLE_POINT"))
                .andExpect(jsonPath("$.recommendationBasis").value("STRAIGHT_LINE_PREVIEW"))
                .andExpect(jsonPath("$.center.latitude").value(37.498095))
                .andExpect(jsonPath("$.participantCount").value(2))
                .andExpect(jsonPath("$.departureRespondedParticipantCount").value(2))
                .andExpect(jsonPath("$.participants[0].departureResponded").value(true))
                .andExpect(jsonPath("$.recommendations[0].rank").value(1))
                .andExpect(jsonPath("$.recommendations[0].areaName").isString())
                .andExpect(jsonPath("$.recommendations[0].averageStraightDistanceMeters").isNumber());
    }

    @Test
    void getPlaceViewReturnsRandomCatalogPreviewForRandomStrategy() throws Exception {
        CreateMeetingRequest request = new CreateMeetingRequest(
                "random-meeting",
                null,
                6,
                com.moyeo.domain.meeting.PlanningType.PLACE_ONLY,
                List.of(),
                null,
                null,
                com.moyeo.domain.meeting.PlaceRecommendationStrategy.RANDOM,
                null,
                null,
                null,
                null,
                null,
                1440
        );
        String inviteCode = createMeetingAndGetInviteCode("meetinghost34", "host34", request);

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/places", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placeRecommendationStrategy").value("RANDOM"))
                .andExpect(jsonPath("$.recommendationBasis").value("RANDOM_CATALOG_PREVIEW"))
                .andExpect(jsonPath("$.center").doesNotExist())
                .andExpect(jsonPath("$.recommendations.length()").value(5))
                .andExpect(jsonPath("$.recommendations[0].rank").value(1))
                .andExpect(jsonPath("$.recommendations[0].averageStraightDistanceMeters").doesNotExist());
    }

    private String signupAndGetAccessToken(String loginId, String nickname) throws Exception {
        String response = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginId", loginId,
                                "password", "password123!",
                                "nickname", nickname
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String createMeetingAndGetInviteCode(String loginId, String nickname, int maxParticipants) throws Exception {
        return createMeetingAndGetInviteCode(loginId, nickname, defaultCreateMeetingRequest(maxParticipants));
    }

    private String createMeetingAndGetInviteCode(String loginId, String nickname, CreateMeetingRequest request) throws Exception {
        String accessToken = signupAndGetAccessToken(loginId, nickname);
        return createMeetingAndGetInviteCode(accessToken, request);
    }

    private String createMeetingAndGetInviteCode(String accessToken, CreateMeetingRequest request) throws Exception {
        String response = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);
        return jsonNode.get("inviteCode").asText();
    }

    private CreateMeetingRequest defaultCreateMeetingRequest(int maxParticipants) {
        return new CreateMeetingRequest(
                "weekend-meeting",
                "dinner together",
                maxParticipants,
                com.moyeo.domain.meeting.PlanningType.SCHEDULE_AND_PLACE,
                List.of(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)),
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                com.moyeo.domain.meeting.PlaceRecommendationStrategy.MIDDLE_POINT,
                "company",
                "Seoul Gangnam",
                BigDecimal.valueOf(37.498095),
                BigDecimal.valueOf(127.027610),
                com.moyeo.domain.meeting.TransportationMode.PUBLIC_TRANSIT,
                1440
        );
    }

    private CreateMeetingRequest invalidCreateMeetingRequest() {
        return new CreateMeetingRequest(
                "",
                "x".repeat(101),
                1,
                com.moyeo.domain.meeting.PlanningType.SCHEDULE_AND_PLACE,
                List.of(),
                LocalTime.of(18, 0),
                LocalTime.of(9, 0),
                null,
                null,
                null,
                null,
                null,
                null,
                0
        );
    }

    private void joinGuest(String inviteCode, String nickname) throws Exception {
        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultGuestJoinRequest(nickname, "guestpass123"))))
                .andExpect(status().isCreated());
    }

    private void joinMember(String inviteCode, String accessToken, String nickname) throws Exception {
        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/members", inviteCode)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultMemberJoinRequest(nickname))))
                .andExpect(status().isCreated());
    }

    private Long joinGuestAndGetParticipantId(String inviteCode, String nickname) throws Exception {
        String response = mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultGuestJoinRequest(nickname, "guestpass123"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("participantId").asLong();
    }

    private void saveDefaultParticipation(String inviteCode, Long participantId, String startTime, String endTime) throws Exception {
        saveDefaultParticipation(inviteCode, participantId, List.of(scheduleAvailability(startTime, endTime)));
    }

    private void saveDefaultParticipation(
            String inviteCode,
            Long participantId,
            List<Map<String, String>> scheduleAvailabilities
    ) throws Exception {
        meetingService.saveParticipation(
                inviteCode,
                participantId,
                new SaveParticipationCommand(
                        scheduleAvailabilities.stream()
                                .map(slot -> new SaveParticipationCommand.ScheduleAvailability(
                                        LocalDate.parse(slot.get("candidateDate")),
                                        LocalTime.parse(slot.get("startTime")),
                                        LocalTime.parse(slot.get("endTime"))
                                ))
                                .toList(),
                        new SaveParticipationCommand.Departure(
                                "company",
                                "Seoul Gangnam",
                                BigDecimal.valueOf(37.498095),
                                BigDecimal.valueOf(127.027610),
                                com.moyeo.domain.meeting.TransportationMode.PUBLIC_TRANSIT
                        )
                )
        );
    }

    private Map<String, Object> defaultGuestJoinRequest(String nickname, String password) {
        return defaultJoinRequest(nickname, password);
    }

    private Map<String, Object> defaultMemberJoinRequest(String nickname) {
        return Map.of(
                "nickname", nickname,
                "scheduleAvailabilities", List.of(scheduleAvailability("09:00", "10:00")),
                "departure", Map.of(
                        "name", "company",
                        "address", "Seoul Gangnam",
                        "latitude", 37.498095,
                        "longitude", 127.027610,
                        "transportationMode", "PUBLIC_TRANSIT"
                )
        );
    }

    private Map<String, Object> defaultJoinRequest(String nickname, String password) {
        return Map.of(
                "nickname", nickname,
                "password", password,
                "scheduleAvailabilities", List.of(scheduleAvailability("09:00", "10:00")),
                "departure", Map.of(
                        "name", "company",
                        "address", "Seoul Gangnam",
                        "latitude", 37.498095,
                        "longitude", 127.027610,
                        "transportationMode", "PUBLIC_TRANSIT"
                )
        );
    }

    private Map<String, String> scheduleAvailability(String startTime, String endTime) {
        return Map.of(
                "candidateDate", "2026-07-01",
                "startTime", startTime,
                "endTime", endTime
        );
    }
}
