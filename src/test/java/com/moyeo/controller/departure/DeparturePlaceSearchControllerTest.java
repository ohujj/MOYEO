package com.moyeo.controller.departure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyeo.controller.meeting.CreateMeetingRequest;
import com.moyeo.controller.meeting.SaveParticipationRequest;
import com.moyeo.departure.DeparturePlaceSearchService;
import com.moyeo.departure.DeparturePlaceType;
import com.moyeo.domain.departure.DeparturePlaceSearchExecutionPath;
import com.moyeo.domain.meeting.PlanningType;
import com.moyeo.domain.meeting.TransportationMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "moyeo.departure-place-search.rest-api-key=")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class DeparturePlaceSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void searchResponseIncludesSelectedCandidateCoordinates() {
        DeparturePlaceSearchResponse response = DeparturePlaceSearchResponse.from(
                new DeparturePlaceSearchService.DeparturePlaceSearchResult(
                        "서울 강남구 테헤란로 152",
                        DeparturePlaceSearchExecutionPath.ADDRESS,
                        List.of(
                        new DeparturePlaceSearchService.DeparturePlaceSearchResult.Place(
                                DeparturePlaceType.ADDRESS,
                                "강남파이낸스센터",
                                "서울 강남구 테헤란로 152",
                                "서울 강남구 테헤란로 152",
                                "서울 강남구 역삼동 737",
                                new BigDecimal("37.500028"),
                                new BigDecimal("127.036502")
                        )
                ))
        );

        assertThat(response.results()).singleElement().satisfies(result -> {
            assertThat(result.latitude()).isEqualByComparingTo("37.500028");
            assertThat(result.longitude()).isEqualByComparingTo("127.036502");
        });
    }

    @Test
    void openApiPublishesOnlyTheUnifiedDepartureSearchEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/departure-places/searches']").exists())
                .andExpect(jsonPath("$['paths']['/api/departure-places/searches']['post']['description']",
                        containsString("Access Token: 선택")))
                .andExpect(jsonPath("$['paths']['/api/departure-places/searches']['post']['description']",
                        containsString("inviteCode: 조건부 필수")))
                .andExpect(jsonPath("$['paths']['/api/departure-places/searches']['post']['description']",
                        containsString("통합 이유:")))
                .andExpect(jsonPath("$['paths']['/api/departure-places/searches']['post']['description']",
                        containsString("`번지` 포함")))
                .andExpect(jsonPath("$['paths']['/api/departure-places/searches']['post']['parameters'][?(@.name == 'inviteCode')]").exists())
                .andExpect(jsonPath("$['paths']['/api/departure-places/searches']['post']['security'].length()").value(2))
                .andExpect(jsonPath("$['paths']['/api/departure-places/searches']['post']['security'][0]['bearerAuth']").exists())
                .andExpect(jsonPath("$['paths']['/api/departure-places/searches']['post']['security'][1]").isEmpty())
                .andExpect(jsonPath("$['paths']['/api/meetings/invitations/{inviteCode}/departure-places/searches']").doesNotExist())
                .andExpect(jsonPath("$['paths']['/api/addresses/searches']").doesNotExist());
    }

    @Test
    void searchRequiresAccessTokenOrInviteCode() throws Exception {
        mockMvc.perform(post("/api/departure-places/searches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("keyword", "서울역"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void searchValidatesKeyword() throws Exception {
        mockMvc.perform(post("/api/departure-places/searches")
                        .header("Authorization", "Bearer " + signupAndGetAccessToken("departure-search-validation"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void searchReturnsServiceUnavailableWithoutKakaoRestApiKey() throws Exception {
        mockMvc.perform(post("/api/departure-places/searches")
                        .header("Authorization", "Bearer " + signupAndGetAccessToken("departure-search-unavailable"))
                        .queryParam("inviteCode", "UNKNOWN123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("keyword", "서울역"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("DEPARTURE_PLACE_SEARCH_UNAVAILABLE"));
    }

    @Test
    void guestSearchRejectsUnknownInvitationBeforeCallingProvider() throws Exception {
        mockMvc.perform(post("/api/departure-places/searches")
                        .queryParam("inviteCode", "UNKNOWN123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("keyword", "서울역"))))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MEETING_INVITATION_NOT_FOUND"));
    }

    @Test
    void guestSearchWithValidInvitationDoesNotRequireAccessToken() throws Exception {
        String hostAccessToken = signupAndGetAccessToken("guest-departure-search-host");
        String inviteCode = createMeetingAndGetInviteCode(hostAccessToken);

        mockMvc.perform(post("/api/departure-places/searches")
                        .queryParam("inviteCode", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("keyword", "서울역"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("DEPARTURE_PLACE_SEARCH_UNAVAILABLE"));
    }

    @Test
    void invalidAccessTokenDoesNotFallBackToAValidInvitation() throws Exception {
        String hostAccessToken = signupAndGetAccessToken("invalid-token-fallback-host");
        String inviteCode = createMeetingAndGetInviteCode(hostAccessToken);

        mockMvc.perform(post("/api/departure-places/searches")
                        .header("Authorization", "Bearer invalid-token")
                        .queryParam("inviteCode", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("keyword", "서울역"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void removedAddressSearchEndpointIsNotAvailable() throws Exception {
        mockMvc.perform(post("/api/addresses/searches")
                        .header("Authorization", "Bearer " + signupAndGetAccessToken("removed-address-search"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("keyword", "테헤란로 123"))))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_ENDPOINT_NOT_FOUND"));
    }

    @Test
    void removedInviteScopedDepartureSearchEndpointIsNotAvailable() throws Exception {
        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/departure-places/searches", "UNKNOWN123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("keyword", "서울역"))))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_ENDPOINT_NOT_FOUND"));
    }

    private String signupAndGetAccessToken(String loginId) throws Exception {
        String response = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginId", loginId,
                                "password", "password123!",
                                "nickname", "departure-search"
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String createMeetingAndGetInviteCode(String accessToken) throws Exception {
        CreateMeetingRequest request = new CreateMeetingRequest(
                "guest-search",
                "guest departure place search",
                6,
                PlanningType.PLACE_ONLY,
                null,
                null,
                null,
                null,
                null,
                new SaveParticipationRequest.DepartureRequest(
                        "company",
                        "Seoul Gangnam",
                        BigDecimal.valueOf(37.498095),
                        BigDecimal.valueOf(127.027610),
                        TransportationMode.PUBLIC_TRANSIT
                ),
                1440
        );
        String createResponse = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(createResponse).get("inviteCode").asText();
    }
}
