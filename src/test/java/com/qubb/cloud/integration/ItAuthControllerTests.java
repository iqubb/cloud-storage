package com.qubb.cloud.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qubb.cloud.repository.UserRepository;
import com.qubb.cloud.util.DataUtil;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ItAuthControllerTests extends AbstractControllerBaseTest{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Test register user functionality")
    public void givenNewUser_whenRegisterUser_thenUserIsSaved() throws Exception {

        //Given
        var request = DataUtil.getKikwiRegisterRequest();

        //When
        ResultActions result = mockMvc.perform(post("/api/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        );

        //Then
        result.andExpect(status().isCreated())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.username", CoreMatchers.is("kikwi")));
    }

    @Test
    @DisplayName("Test register user with duplicate username functionality")
    public void givenUserWithDuplicateUsername_whenRegisterUser_thenErrorResponse() throws Exception {

        //Given
        var existingUser = DataUtil.getKikwiTransient();
        userRepository.save(existingUser);
        var registerRequest = DataUtil.getKikwiRegisterRequest();


        //When
        ResultActions result = mockMvc.perform(post("/api/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
        );

        //Then
        result.andExpect(status().isConflict())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status", CoreMatchers.is(409)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.detail", CoreMatchers.containsString("User " + registerRequest.username() + " already registered")));
    }
}
