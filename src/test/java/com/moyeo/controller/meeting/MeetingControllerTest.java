package com.moyeo.controller.meeting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyeo.repository.meeting.MeetingParticipantRepository;
import com.moyeo.repository.meeting.MeetingParticipantScheduleAvailabilityRepository;
import com.moyeo.service.meeting.MeetingService;
import com.moyeo.service.meeting.MeetingCoverStorage;
import com.moyeo.service.meeting.SaveParticipationCommand;
import com.moyeo.domain.meeting.ScheduleInputType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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

    @MockitoBean
    private MeetingCoverStorage meetingCoverStorage;

    @Test
    void createMeetingReturnsMeetingAndInvitationInformation() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost1", "host1");

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultCreateMeetingRequest(6))))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.meetingId").isNumber())
                .andExpect(jsonPath("$.*").value(org.hamcrest.Matchers.hasSize(3)))
                .andExpect(jsonPath("$.inviteCode").isString())
                .andExpect(jsonPath("$.invitePath").isString());
    }

    @Test
    void createDateOnlyMeetingDoesNotRequireCommonTimeRange() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-date-only", "host-date-only");

        Long meetingId = createMeetingAndGetMeetingId(accessToken, dateOnlyCreateMeetingRequest());

        assertThat(jdbcTemplate.queryForObject(
                "select schedule_input_type from meetings where id = ?",
                String.class,
                meetingId
        )).isEqualTo("DATE_ONLY");
        assertThat(jdbcTemplate.queryForObject(
                "select available_start_time is null and available_end_time is null from meetings where id = ?",
                Boolean.class,
                meetingId
        )).isTrue();
    }

    @Test
    void createMeetingRejectsScheduleInputTypeAndTimeRangeMismatch() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-input-mismatch", "host-input-mismatch");

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "invalid-date-only",
                                "maxParticipants", 6,
                                "planningType", "SCHEDULE_ONLY",
                                "scheduleInputType", "DATE_ONLY",
                                "availableStartTime", "09:00",
                                "availableEndTime", "18:00",
                                "deadlineMinutes", 1440
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "invalid-place-only",
                                "maxParticipants", 6,
                                "planningType", "PLACE_ONLY",
                                "scheduleInputType", "DATE_ONLY",
                                "deadlineMinutes", 1440
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void createPlaceOnlyMeetingWithoutScheduleInputType() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-place-simple", "host-place-simple");

        String response = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "place-simple",
                                "maxParticipants", 6,
                                "planningType", "PLACE_ONLY",
                                "departure", Map.of(
                                        "name", "company",
                                        "address", "Seoul",
                                        "transportationMode", "PUBLIC_TRANSIT"
                                ),
                                "deadlineMinutes", 1440
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.meetingId").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long meetingId = objectMapper.readTree(response).get("meetingId").asLong();
        assertThat(jdbcTemplate.queryForObject(
                "select schedule_input_type from meetings where id = ?",
                String.class,
                meetingId
        )).isEqualTo("NONE");
        assertThat(jdbcTemplate.queryForObject(
                "select place_recommendation_strategy from meetings where id = ?",
                String.class,
                meetingId
        )).isEqualTo("MIDDLE_POINT");
    }

    @Test
    void createMeetingWithOptionalCoverReturnsVersionedCoverUrl() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-cover", "host-cover");
        MockMultipartFile request = new MockMultipartFile(
                "request",
                "request.json",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(defaultCreateMeetingRequest(6))
        );
        MockMultipartFile coverImage = new MockMultipartFile(
                "coverImage",
                "cover.png",
                MediaType.IMAGE_PNG_VALUE,
                pngImage()
        );

        mockMvc.perform(multipart("/api/meetings")
                        .file(request)
                        .file(coverImage)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.meetingId").isNumber())
                .andExpect(jsonPath("$.coverImageUrl").doesNotExist());

        verify(meetingCoverStorage).put(any(String.class), any(byte[].class));
    }

    @Test
    void createMeetingWithMultipartRequestAndNoCoverSucceeds() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-no-cover", "host-no-cover");
        MockMultipartFile request = new MockMultipartFile(
                "request",
                "request.json",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(defaultCreateMeetingRequest(6))
        );

        mockMvc.perform(multipart("/api/meetings")
                .file(request)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.meetingId").isNumber())
                .andExpect(jsonPath("$.inviteCode").isString())
                .andExpect(jsonPath("$.coverImageUrl").doesNotExist());
    }

    @Test
    void hostCanReplaceAndDeleteCoverImage() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-cover-edit", "host-cover-edit");
        String created = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultCreateMeetingRequest(6))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long meetingId = objectMapper.readTree(created).get("meetingId").asLong();
        MockMultipartFile coverImage = new MockMultipartFile(
                "coverImage", "cover.png", MediaType.IMAGE_PNG_VALUE, pngImage());

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/meetings/{meetingId}/cover-image", meetingId)
                        .file(coverImage)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverImageUrl").value(org.hamcrest.Matchers.containsString("/cover-image?v=")));

        mockMvc.perform(delete("/api/meetings/{meetingId}/cover-image", meetingId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void createScheduleMeetingStoresHostParticipationAtomically() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost35", "host35");

        String response = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultCreateMeetingRequest(6))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long meetingId = objectMapper.readTree(response).get("meetingId").asLong();
        Long hostParticipantId = meetingParticipantRepository.findAllByMeetingIdOrderByIdAsc(meetingId).getFirst().getId();
        assertThat(meetingParticipantScheduleAvailabilityRepository.countByParticipantId(hostParticipantId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from meeting_schedule_candidates where meeting_id = ?",
                Long.class,
                meetingId
        )).isEqualTo(2L);
    }

    @Test
    void createDateOnlyMeetingUsesCandidateDatesAsHostAvailability() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-date-only-complete", "host-date-only-complete");
        String response = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dateOnlyCreateMeetingRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inviteCode").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long meetingId = objectMapper.readTree(response).get("meetingId").asLong();
        Long hostParticipantId = meetingParticipantRepository.findAllByMeetingIdOrderByIdAsc(meetingId).getFirst().getId();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from meeting_participant_schedule_date_availabilities where participant_id = ?",
                Long.class,
                hostParticipantId
        )).isEqualTo(2L);
        assertThat(meetingParticipantScheduleAvailabilityRepository.countByParticipantId(hostParticipantId)).isZero();

        String inviteCode = objectMapper.readTree(response).get("inviteCode").asText();
        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleInputType").value("DATE_ONLY"))
                .andExpect(jsonPath("$.availableStartTime").doesNotExist())
                .andExpect(jsonPath("$.availableEndTime").doesNotExist());
    }

    @Test
    void createMeetingRollsBackWhenHostAvailabilityIsInvalid() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-rollback", "host-rollback");
        Long meetingCount = jdbcTemplate.queryForObject("select count(*) from meetings", Long.class);

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "rollback",
                                "maxParticipants", 6,
                                "planningType", "SCHEDULE_ONLY",
                                "scheduleInputType", "DATE_AND_TIME",
                                "availableStartTime", "09:00",
                                "availableEndTime", "18:00",
                                "scheduleCandidateDates", List.of("2026-07-01"),
                                "scheduleResponse", Map.of(
                                        "availableTimeRanges", List.of(scheduleAvailability("08:00", "09:00"))
                                ),
                                "deadlineMinutes", 1440
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MEETING_PARTICIPATION_INPUT"));

        assertThat(jdbcTemplate.queryForObject("select count(*) from meetings", Long.class)).isEqualTo(meetingCount);
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
    void createMeetingAllowsMoreThanTwentyOneScheduleCandidateDates() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-many-dates", "host-many-dates");
        List<LocalDate> candidateDates = java.util.stream.IntStream.range(0, 22)
                .mapToObj(dayOffset -> LocalDate.of(2026, 7, 1).plusDays(dayOffset))
                .toList();

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "weekend",
                                "maxParticipants", 6,
                                "planningType", "SCHEDULE_ONLY",
                                "scheduleInputType", "DATE_AND_TIME",
                                "availableStartTime", "18:00",
                                "availableEndTime", "22:00",
                                "scheduleCandidateDates", candidateDates,
                                "scheduleResponse", Map.of(
                                        "availableTimeRanges", List.of(scheduleAvailability("18:00", "19:00"))
                                ),
                                "deadlineMinutes", 1440
                        ))))
                .andExpect(status().isCreated());
    }

    @Test
    void createMeetingRejectsNonHourUnitScheduleTime() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost10", "host10");

        CreateMeetingRequest request = createMeetingRequest(
                "weekend",
                "dinner",
                6,
                com.moyeo.domain.meeting.PlanningType.SCHEDULE_ONLY,
                ScheduleInputType.DATE_AND_TIME,
                LocalTime.of(18, 30),
                LocalTime.of(22, 0),
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

        CreateMeetingRequest request = createMeetingRequest(
                "weekend",
                "dinner",
                6,
                com.moyeo.domain.meeting.PlanningType.SCHEDULE_AND_PLACE,
                ScheduleInputType.DATE_AND_TIME,
                LocalTime.of(18, 0),
                LocalTime.of(22, 0),
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
    void createMeetingRejectsMiddlePointWithoutDeparture() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost17", "host17");

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "weekend",
                                "maxParticipants", 6,
                                "planningType", "PLACE_ONLY",
                                "deadlineMinutes", 1440
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_MEETING_PARTICIPATION_INPUT"));
    }

    @Test
    void createMeetingSupportsPlaceOnlyPlanning() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost13", "host13");

        CreateMeetingRequest request = createMeetingRequest(
                "weekend",
                "dinner",
                6,
                com.moyeo.domain.meeting.PlanningType.PLACE_ONLY,
                null,
                null,
                null,
                1440
        );

        String inviteCode = createMeetingAndGetInviteCode(accessToken, request);

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planningType").value("PLACE_ONLY"))
                .andExpect(jsonPath("$.scheduleMode").value("NONE"))
                .andExpect(jsonPath("$.scheduleCandidateDates").isEmpty())
                .andExpect(jsonPath("$.availableStartTime").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.availableEndTime").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.placeMode").value("RECOMMEND"))
                .andExpect(jsonPath("$.placeRecommendationStrategy").value("MIDDLE_POINT"));
    }

    @Test
    void placeViewUsesDepartureAddressWhenDepartureNameIsOmitted() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-departure-address", "host-departure-address");
        String response = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "주소 출발지",
                                "maxParticipants", 6,
                                "planningType", "PLACE_ONLY",
                                "departure", Map.of(
                                        "address", "서울 강남구 테헤란로 123",
                                        "latitude", 37.498095,
                                        "longitude", 127.027610,
                                        "transportationMode", "PUBLIC_TRANSIT"
                                ),
                                "deadlineMinutes", 1440
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String inviteCode = objectMapper.readTree(response).get("inviteCode").asText();

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/places", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendationBasis").value("STRAIGHT_LINE_PREVIEW"))
                .andExpect(jsonPath("$.participants[0].departureName").value("서울 강남구 테헤란로 123"))
                .andExpect(jsonPath("$.participants[0].departureAddress").value("서울 강남구 테헤란로 123"))
                .andExpect(jsonPath("$.recommendations[0].areaName").isString());
    }

    @Test
    void createMeetingRemovesDuplicatedScheduleCandidateDatesAndSortsThem() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost14", "host14");

        String response = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "weekend",
                                "maxParticipants", 6,
                                "planningType", "SCHEDULE_ONLY",
                                "scheduleInputType", "DATE_AND_TIME",
                                "availableStartTime", "18:00",
                                "availableEndTime", "22:00",
                                "scheduleCandidateDates", List.of("2026-07-02", "2026-07-01", "2026-07-01"),
                                "scheduleResponse", Map.of(
                                        "availableTimeRanges", List.of(scheduleAvailability("18:00", "19:00"))
                                ),
                                "deadlineMinutes", 1440
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String inviteCode = objectMapper.readTree(response).get("inviteCode").asText();

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}", inviteCode))
                .andExpect(status().isOk())
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
                                "scheduleResponse", Map.of(
                                        "availableTimeRanges", List.of(Map.of(
                                                "candidateDate", "2026-07-01",
                                                "startTime", "08:00",
                                                "endTime", "09:00"
                                        ))
                                ),
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
                                "scheduleResponse", Map.of(
                                        "availableTimeRanges", List.of(
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
                                "scheduleResponse", Map.of(
                                        "availableTimeRanges", List.of(Map.of(
                                                "candidateDate", "2026-07-01",
                                                "startTime", "08:00",
                                                "endTime", "09:00"
                                        ))
                                ),
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
                createMeetingRequest(
                        "schedule",
                        "schedule only",
                        6,
                        com.moyeo.domain.meeting.PlanningType.SCHEDULE_ONLY,
                        ScheduleInputType.DATE_AND_TIME,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        1440
                )
        );
        Long participantId = joinGuestAndGetParticipantId(inviteCode, "guest-schedule-only");

        mockMvc.perform(put("/api/meetings/invitations/{inviteCode}/participants/{participantId}/participation", inviteCode, participantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "scheduleResponse", Map.of(
                                        "availableTimeRanges", List.of(Map.of(
                                                "candidateDate", "2026-07-01",
                                                "startTime", "09:00",
                                                "endTime", "10:00"
                                        ))
                                ),
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
    void getMeetingViewReturnsParticipantListWithoutRedundantResponseStatus() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost29", "host29", 6);
        Long participantId = joinGuestAndGetParticipantId(inviteCode, "guest-view");
        saveDefaultParticipation(inviteCode, participantId, "09:00", "10:00");

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view", inviteCode))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("weekend-meeting"))
                .andExpect(jsonPath("$.maxParticipants").value(6))
                .andExpect(jsonPath("$.participantCount").value(2))
                .andExpect(jsonPath("$.respondedParticipantCount").doesNotExist())
                .andExpect(jsonPath("$.responseRate").doesNotExist())
                .andExpect(jsonPath("$.participants[0].participantType").value("HOST"))
                .andExpect(jsonPath("$.participants[1].nickname").value("guest-view"))
                .andExpect(jsonPath("$.participants[0].scheduleResponded").doesNotExist())
                .andExpect(jsonPath("$.participants[0].placeResponded").doesNotExist())
                .andExpect(jsonPath("$.participants[0].responseCompleted").doesNotExist());
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
                .andExpect(jsonPath("$.respondedParticipantCount").doesNotExist())
                .andExpect(jsonPath("$.candidates[0].candidateDate").value("2026-07-01"))
                .andExpect(jsonPath("$.candidates[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$.candidates[0].availableParticipantCount").value(3))
                .andExpect(jsonPath("$.candidates[0].totalParticipantCount").doesNotExist())
                .andExpect(jsonPath("$.emptyMessage").doesNotExist());
    }

    @Test
    void swaggerDocumentsAllScheduleViewInputTypes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.components.schemas.ScheduleViewResponse.properties.scheduleInputType.enum",
                        containsInAnyOrder("DATE_ONLY", "DATE_AND_TIME", "NONE")
                ));
    }

    @Test
    void swaggerDocumentsScheduleInputTypeAsOptionalWithoutNoneForCreation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.components.schemas.CreateMeetingRequest.properties.scheduleInputType.enum",
                        containsInAnyOrder("DATE_ONLY", "DATE_AND_TIME")
                ))
                .andExpect(jsonPath(
                        "$.components.schemas.CreateMeetingRequest.required",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("scheduleInputType"))
                ));
    }

    @Test
    void swaggerDocumentsEveryMeetingCreationFlowForJsonAndMultipart() throws Exception {
        List<String> exampleNames = List.of(
                "SCHEDULE_AND_PLACE_DATE_AND_TIME",
                "SCHEDULE_AND_PLACE_DATE_ONLY",
                "SCHEDULE_ONLY_DATE_AND_TIME",
                "SCHEDULE_ONLY_DATE_ONLY",
                "PLACE_ONLY"
        );

        var openApi = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        var requestContent = openApi.path("paths")
                .path("/api/meetings")
                .path("post")
                .path("requestBody")
                .path("content");
        assertThat(openApi.path("paths").path("/api/meetings").path("post").path("description").asText())
                .contains("출발지 이름 공통 안내")
                .contains("departure.name");

        var jsonExamples = requestContent.path(MediaType.APPLICATION_JSON_VALUE).path("examples");
        var multipartExamples = requestContent.path(MediaType.MULTIPART_FORM_DATA_VALUE).path("examples");
        assertThat(jsonExamples.size()).isEqualTo(exampleNames.size());
        assertThat(multipartExamples.size()).isEqualTo(exampleNames.size());
        assertThat(exampleNames).allSatisfy(exampleName -> {
            assertThat(jsonExamples.has(exampleName)).isTrue();
            assertThat(multipartExamples.has(exampleName)).isTrue();
        });
        assertThat(jsonExamples.path("SCHEDULE_AND_PLACE_DATE_AND_TIME").path("value").has("scheduleCandidateDates")).isTrue();
        assertThat(jsonExamples.path("SCHEDULE_AND_PLACE_DATE_AND_TIME").path("value").has("scheduleResponse")).isTrue();
        assertThat(jsonExamples.path("SCHEDULE_AND_PLACE_DATE_AND_TIME").path("value").has("departure")).isTrue();
        assertThat(jsonExamples.path("SCHEDULE_ONLY_DATE_ONLY").path("value").has("scheduleCandidateDates")).isTrue();
        assertThat(jsonExamples.path("PLACE_ONLY").path("value").has("departure")).isTrue();
        assertThat(jsonExamples.path("PLACE_ONLY").path("value").path("departure").has("name")).isFalse();
        assertThat(multipartExamples.path("PLACE_ONLY").path("value").path("request").path("departure").has("name")).isFalse();
        var departureNameSchema = openApi.path("components").path("schemas").path("DepartureRequest")
                .path("properties").path("name");
        assertThat(departureNameSchema.path("type").toString())
                .contains("string")
                .contains("null");
        assertThat(openApi.path("components").path("schemas").path("DepartureRequest").path("required").toString())
                .doesNotContain("name");
    }

    @Test
    void swaggerDocumentsValidationAndParticipationInputErrorsForMeetingFlows() throws Exception {
        var openApi = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        var paths = openApi.path("paths");
        List<String> errorContents = List.of(
                paths.path("/api/meetings").path("post").path("responses").path("400").path("content").toString(),
                paths.path("/api/meetings/invitations/{inviteCode}/guests").path("post").path("responses").path("400").path("content").toString(),
                paths.path("/api/meetings/invitations/{inviteCode}/members").path("post").path("responses").path("400").path("content").toString()
        );

        assertThat(errorContents).allSatisfy(errorContent -> {
            assertThat(errorContent).contains("COMMON_VALIDATION_FAILED");
            assertThat(errorContent).contains("INVALID_MEETING_PARTICIPATION_INPUT");
        });
    }

    @Test
    void swaggerDocumentsSameFiveFlowsForGuestAndMemberJoinAndNoSeparateHostParticipation() throws Exception {
        List<String> exampleNames = List.of(
                "SCHEDULE_AND_PLACE_DATE_AND_TIME",
                "SCHEDULE_AND_PLACE_DATE_ONLY",
                "SCHEDULE_ONLY_DATE_AND_TIME",
                "SCHEDULE_ONLY_DATE_ONLY",
                "PLACE_ONLY"
        );
        var openApi = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        var paths = openApi.path("paths");
        assertThat(paths.has("/api/meetings/{meetingId}/participation")).isFalse();

        var guestExamples = paths.path("/api/meetings/invitations/{inviteCode}/guests")
                .path("post").path("requestBody").path("content")
                .path(MediaType.APPLICATION_JSON_VALUE).path("examples");
        var memberExamples = paths.path("/api/meetings/invitations/{inviteCode}/members")
                .path("post").path("requestBody").path("content")
                .path(MediaType.APPLICATION_JSON_VALUE).path("examples");
        assertThat(paths.path("/api/meetings/invitations/{inviteCode}/guests").path("post").path("description").asText())
                .contains("출발지 이름 공통 안내")
                .contains("departure.name");
        assertThat(paths.path("/api/meetings/invitations/{inviteCode}/members").path("post").path("description").asText())
                .contains("출발지 이름 공통 안내")
                .contains("departure.name");
        assertThat(guestExamples.size()).isEqualTo(exampleNames.size());
        assertThat(memberExamples.size()).isEqualTo(exampleNames.size());
        assertThat(exampleNames).allSatisfy(exampleName -> {
            assertThat(guestExamples.has(exampleName)).isTrue();
            assertThat(memberExamples.has(exampleName)).isTrue();
        });
        assertThat(guestExamples.path("PLACE_ONLY").path("value").path("departure").has("name")).isFalse();
        assertThat(memberExamples.path("PLACE_ONLY").path("value").path("departure").has("name")).isFalse();
    }

    @Test
    void swaggerExamplesCanCreateMeetingThenJoinGuestAndDifferentMember() throws Exception {
        List<String> exampleNames = List.of(
                "SCHEDULE_AND_PLACE_DATE_AND_TIME",
                "SCHEDULE_AND_PLACE_DATE_ONLY",
                "SCHEDULE_ONLY_DATE_AND_TIME",
                "SCHEDULE_ONLY_DATE_ONLY",
                "PLACE_ONLY"
        );
        var openApi = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        var paths = openApi.path("paths");
        var creationExamples = paths.path("/api/meetings").path("post")
                .path("requestBody").path("content")
                .path(MediaType.APPLICATION_JSON_VALUE).path("examples");
        var guestExamples = paths.path("/api/meetings/invitations/{inviteCode}/guests").path("post")
                .path("requestBody").path("content")
                .path(MediaType.APPLICATION_JSON_VALUE).path("examples");
        var memberExamples = paths.path("/api/meetings/invitations/{inviteCode}/members").path("post")
                .path("requestBody").path("content")
                .path(MediaType.APPLICATION_JSON_VALUE).path("examples");

        for (int index = 0; index < exampleNames.size(); index++) {
            String exampleName = exampleNames.get(index);
            String hostToken = signupAndGetAccessToken("swagger-host-" + index, "swagger-host-" + index);
            String memberToken = signupAndGetAccessToken("swagger-member-" + index, "swagger-member-" + index);
            String createResponse = mockMvc.perform(post("/api/meetings")
                            .header("Authorization", "Bearer " + hostToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(creationExamples.path(exampleName).path("value").toString()))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            String inviteCode = objectMapper.readTree(createResponse).path("inviteCode").asText();

            mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(guestExamples.path(exampleName).path("value").toString()))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/members", inviteCode)
                            .header("Authorization", "Bearer " + memberToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(memberExamples.path(exampleName).path("value").toString()))
                    .andExpect(status().isCreated());

            if ("SCHEDULE_AND_PLACE_DATE_AND_TIME".equals(exampleName)) {
                mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/schedules", inviteCode)
                                .param("sort", "LONGEST_MEETING"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.candidates.length()").value(3));
            }
        }
    }

    @Test
    void dateOnlyParticipantSelectsDatesAndScheduleViewAggregatesByDate() throws Exception {
        String hostToken = signupAndGetAccessToken("meetinghost-date-view", "host-date-view");
        String inviteCode = createMeetingAndGetInviteCode(hostToken, dateOnlyCreateMeetingRequest());

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", "date-only-guest",
                                "password", "guestpass123",
                                "scheduleResponse", Map.of(
                                        "availableDates", List.of("2026-07-02")
                                )
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleInputType").value("DATE_ONLY"))
                .andExpect(jsonPath("$.participantCount").value(2))
                .andExpect(jsonPath("$.respondedParticipantCount").doesNotExist());

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/schedules", inviteCode)
                        .param("sort", "LONGEST_MEETING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleInputType").value("DATE_ONLY"))
                .andExpect(jsonPath("$.participantCount").value(2))
                .andExpect(jsonPath("$.respondedParticipantCount").doesNotExist())
                .andExpect(jsonPath("$.candidates[0].candidateDate").value("2026-07-02"))
                .andExpect(jsonPath("$.candidates[0].startTime").doesNotExist())
                .andExpect(jsonPath("$.candidates[0].endTime").doesNotExist())
                .andExpect(jsonPath("$.candidates[0].availableParticipantCount").value(2))
                .andExpect(jsonPath("$.candidates[1].candidateDate").value("2026-07-01"))
                .andExpect(jsonPath("$.candidates[1].availableParticipantCount").value(1));
    }

    @Test
    void dateOnlyScheduleAndPlaceStoresDatesAndDeparturesTogether() throws Exception {
        String hostToken = signupAndGetAccessToken("meetinghost-date-place", "host-date-place");
        CreateMeetingRequest request = new CreateMeetingRequest(
                "date-place",
                "choose dates and place",
                6,
                com.moyeo.domain.meeting.PlanningType.SCHEDULE_AND_PLACE,
                ScheduleInputType.DATE_ONLY,
                null,
                null,
                List.of(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)),
                null,
                new SaveParticipationRequest.DepartureRequest(
                        "host-company",
                        "Seoul Gangnam",
                        BigDecimal.valueOf(37.498095),
                        BigDecimal.valueOf(127.027610),
                        com.moyeo.domain.meeting.TransportationMode.PUBLIC_TRANSIT
                ),
                1440
        );
        String inviteCode = createMeetingAndGetInviteCode(hostToken, request);
        Long meetingId = jdbcTemplate.queryForObject(
                "select id from meetings where invite_code = ?",
                Long.class,
                inviteCode
        );

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", "date-place-guest",
                                "password", "guestpass123",
                                "scheduleResponse", Map.of(
                                        "availableDates", List.of("2026-07-02")
                                ),
                                "departure", Map.of(
                                        "name", "guest-home",
                                        "address", "Seoul Mapo",
                                        "latitude", 37.566500,
                                        "longitude", 126.978000,
                                        "transportationMode", "CAR"
                                )
                        ))))
                .andExpect(status().isCreated());

        assertThat(jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from meeting_participant_schedule_date_availabilities availability
                        join meeting_participants participant on participant.id = availability.participant_id
                        where participant.meeting_id = ?
                        """,
                Long.class,
                meetingId
        )).isEqualTo(3L);
        assertThat(jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from meeting_participant_schedule_availabilities availability
                        join meeting_participants participant on participant.id = availability.participant_id
                        where participant.meeting_id = ?
                        """,
                Long.class,
                meetingId
        )).isZero();
        assertThat(meetingParticipantRepository.findAllByMeetingIdOrderByIdAsc(meetingId))
                .extracting(participant -> participant.getDepartureName())
                .containsExactly("host-company", "guest-home");

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleInputType").value("DATE_ONLY"))
                .andExpect(jsonPath("$.participantCount").value(2))
                .andExpect(jsonPath("$.respondedParticipantCount").doesNotExist())
                .andExpect(jsonPath("$.responseRate").doesNotExist());
    }

    @Test
    void dateOnlyParticipationRejectsDateOutsideHostCandidates() throws Exception {
        String hostToken = signupAndGetAccessToken("meetinghost-date-invalid", "host-date-invalid");
        String inviteCode = createMeetingAndGetInviteCode(hostToken, dateOnlyCreateMeetingRequest());
        Long meetingId = jdbcTemplate.queryForObject(
                "select id from meetings where invite_code = ?",
                Long.class,
                inviteCode
        );

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", "invalid-date-guest",
                                "password", "guestpass123",
                                "scheduleResponse", Map.of(
                                        "availableDates", List.of("2026-07-03")
                                )
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MEETING_PARTICIPATION_INPUT"));

        assertThat(meetingParticipantRepository.countByMeetingId(meetingId)).isEqualTo(1L);
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
                .andExpect(jsonPath("$.departureRespondedParticipantCount").doesNotExist())
                .andExpect(jsonPath("$.participants[0].departureResponded").doesNotExist())
                .andExpect(jsonPath("$.recommendations[0].rank").value(1))
                .andExpect(jsonPath("$.recommendations[0].areaName").isString())
                .andExpect(jsonPath("$.recommendations[0].averageStraightDistanceMeters").isNumber());
    }

    @Test
    void getPlaceViewReturnsCoordinatesPendingWhenNoDepartureCoordinatesExist() throws Exception {
        CreateMeetingRequest request = new CreateMeetingRequest(
                "coord-pending",
                null,
                6,
                com.moyeo.domain.meeting.PlanningType.PLACE_ONLY,
                null,
                null,
                null,
                null,
                null,
                new SaveParticipationRequest.DepartureRequest(
                        "company",
                        "Seoul Gangnam",
                        null,
                        null,
                        com.moyeo.domain.meeting.TransportationMode.PUBLIC_TRANSIT
                ),
                1440
        );
        String accessToken = signupAndGetAccessToken("meetinghost-coordinate-pending", "host-coordinate-pending");
        String inviteCode = createMeetingAndGetInviteCode(accessToken, request);

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/places", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placeRecommendationStrategy").value("MIDDLE_POINT"))
                .andExpect(jsonPath("$.recommendationBasis").value("COORDINATES_PENDING"))
                .andExpect(jsonPath("$.center").doesNotExist())
                .andExpect(jsonPath("$.departureRespondedParticipantCount").doesNotExist())
                .andExpect(jsonPath("$.recommendations").isEmpty());
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
        return objectMapper.readTree(response).get("inviteCode").asText();
    }

    private Long createMeetingAndGetMeetingId(String accessToken, CreateMeetingRequest request) throws Exception {
        String response = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("meetingId").asLong();
    }

    private CreateMeetingRequest createMeetingRequest(
            String name,
            String description,
            int maxParticipants,
            com.moyeo.domain.meeting.PlanningType planningType,
            ScheduleInputType scheduleInputType,
            LocalTime availableStartTime,
            LocalTime availableEndTime,
            int deadlineMinutes
    ) {
        boolean requiresSchedule = planningType == com.moyeo.domain.meeting.PlanningType.SCHEDULE_ONLY
                || planningType == com.moyeo.domain.meeting.PlanningType.SCHEDULE_AND_PLACE;
        boolean requiresPlace = planningType == com.moyeo.domain.meeting.PlanningType.PLACE_ONLY
                || planningType == com.moyeo.domain.meeting.PlanningType.SCHEDULE_AND_PLACE;
        List<LocalDate> candidateDates = requiresSchedule
                ? List.of(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2))
                : null;
        SaveParticipationRequest.ScheduleResponseRequest scheduleResponse =
                scheduleInputType == ScheduleInputType.DATE_AND_TIME
                        && availableStartTime != null
                        && availableEndTime != null
                        && availableStartTime.isBefore(availableEndTime)
                        ? new SaveParticipationRequest.ScheduleResponseRequest(
                                null,
                                List.of(new SaveParticipationRequest.ScheduleAvailabilityRequest(
                                        LocalDate.of(2026, 7, 1),
                                        availableStartTime,
                                        availableStartTime.plusHours(2).isAfter(availableEndTime)
                                                ? availableEndTime
                                                : availableStartTime.plusHours(2)
                                ))
                        )
                        : null;
        SaveParticipationRequest.DepartureRequest departure = requiresPlace
                ? new SaveParticipationRequest.DepartureRequest(
                        "company",
                        "Seoul",
                        BigDecimal.valueOf(37.498095),
                        BigDecimal.valueOf(127.027610),
                        com.moyeo.domain.meeting.TransportationMode.PUBLIC_TRANSIT
                )
                : null;
        return new CreateMeetingRequest(
                name,
                description,
                maxParticipants,
                planningType,
                scheduleInputType,
                availableStartTime,
                availableEndTime,
                candidateDates,
                scheduleResponse,
                departure,
                deadlineMinutes
        );
    }

    private CreateMeetingRequest defaultCreateMeetingRequest(int maxParticipants) {
        return createMeetingRequest(
                "weekend-meeting",
                "dinner together",
                maxParticipants,
                com.moyeo.domain.meeting.PlanningType.SCHEDULE_AND_PLACE,
                ScheduleInputType.DATE_AND_TIME,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                1440
        );
    }

    private CreateMeetingRequest invalidCreateMeetingRequest() {
        return createMeetingRequest(
                "",
                "x".repeat(101),
                1,
                com.moyeo.domain.meeting.PlanningType.SCHEDULE_AND_PLACE,
                ScheduleInputType.DATE_AND_TIME,
                LocalTime.of(18, 0),
                LocalTime.of(9, 0),
                0
        );
    }

    private CreateMeetingRequest dateOnlyCreateMeetingRequest() {
        return createMeetingRequest(
                "date-only",
                "choose available dates",
                6,
                com.moyeo.domain.meeting.PlanningType.SCHEDULE_ONLY,
                ScheduleInputType.DATE_ONLY,
                null,
                null,
                1440
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

    private byte[] pngImage() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0xFF0000);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
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
                        List.of(),
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
                "scheduleResponse", Map.of(
                        "availableTimeRanges", List.of(scheduleAvailability("09:00", "10:00"))
                ),
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
                "scheduleResponse", Map.of(
                        "availableTimeRanges", List.of(scheduleAvailability("09:00", "10:00"))
                ),
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
