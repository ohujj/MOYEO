package com.moyeo.development;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LocalDevAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesInitializedTestAccountTokenEndpointInLocalProfile() throws Exception {
        mockMvc.perform(post("/api/auth/dev/tokens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userOne.accessToken").isString())
                .andExpect(jsonPath("$.userOne.user.nickname").value("개발 사용자 1"))
                .andExpect(jsonPath("$.userTwo.accessToken").isString())
                .andExpect(jsonPath("$.userTwo.user.nickname").value("개발 사용자 2"));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/auth/dev/tokens']").exists());
    }
}
