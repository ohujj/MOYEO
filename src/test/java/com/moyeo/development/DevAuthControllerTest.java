package com.moyeo.development;

import com.moyeo.controller.auth.AuthResponse;
import com.moyeo.global.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:moyeo-dev-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "moyeo.jwt.secret=development-test-jwt-secret-development-test-jwt-secret"
})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class DevAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Test
    void issuesAccessTokenForInitializedDevAccount() throws Exception {
        String response = mockMvc.perform(post("/api/auth/dev/tokens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userOne.accessToken").isString())
                .andExpect(jsonPath("$.userOne.user.nickname").value("개발 사용자 1"))
                .andExpect(jsonPath("$.userTwo.accessToken").isString())
                .andExpect(jsonPath("$.userTwo.user.nickname").value("개발 사용자 2"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(response, DevAuthTokensResponse.class).userOne();
        assertThat(jwtTokenProvider.parse(authResponse.accessToken()).nickname()).isEqualTo("개발 사용자 1");
    }
}
