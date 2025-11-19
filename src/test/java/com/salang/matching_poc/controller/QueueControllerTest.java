package com.salang.matching_poc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salang.matching_poc.exception.GlobalExceptionHandler;
import com.salang.matching_poc.model.dto.*;
import com.salang.matching_poc.model.enums.Status;
import com.salang.matching_poc.service.MatchingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QueueController.class)
@Import({ GlobalExceptionHandler.class, QueueControllerTest.TestConfig.class })
@SuppressWarnings("null") // 테스트에서 userId 변수들은 null이 아님을 보장
class QueueControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public MatchingService matchingService() {
            return mock(MatchingService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MatchingService matchingService;

    private UUID userId1;

    @BeforeEach
    void setUp() {
        userId1 = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /queue/join - 정상 요청")
    void joinQueue_success() throws Exception {
        // given
        JoinQueueRequest request = JoinQueueRequest.builder()
                .userId(userId1)
                .gender("male")
                .build();

        JoinQueueResponse response = JoinQueueResponse.builder()
                .status(Status.WAITING.name())
                .build();

        when(matchingService.joinQueue(any(JoinQueueRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/queue/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    @DisplayName("POST /queue/leave - 정상 요청")
    void leaveQueue_success() throws Exception {
        // given
        LeaveQueueRequest request = LeaveQueueRequest.builder()
                .userId(userId1)
                .build();

        LeaveQueueResponse response = LeaveQueueResponse.builder()
                .status(Status.IDLE.name())
                .build();

        when(matchingService.leaveQueue(any(LeaveQueueRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/queue/leave")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IDLE"));
    }

    @Test
    @DisplayName("GET /queue/status/{userId} - 정상 요청")
    void getStatus_success() throws Exception {
        // given
        StatusResponse response = StatusResponse.builder()
                .status(Status.WAITING.name())
                .build();

        when(matchingService.getStatus(any(UUID.class))).thenReturn(response);

        // when & then
        mockMvc.perform(get("/queue/status/{userId}", userId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    @DisplayName("POST /queue/ack - 정상 요청")
    void acknowledgeMatch_success() throws Exception {
        // given
        AckRequest request = AckRequest.builder()
                .userId(userId1)
                .build();

        AckResponse response = AckResponse.builder()
                .status(Status.IDLE.name())
                .build();

        when(matchingService.acknowledgeMatch(any(AckRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/queue/ack")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IDLE"));
    }

    @Test
    @DisplayName("POST /queue/join - 유효하지 않은 요청")
    void joinQueue_validationError() throws Exception {
        // given - userId가 null인 경우
        JoinQueueRequest request = JoinQueueRequest.builder()
                .gender("male")
                .build();

        // when & then
        mockMvc.perform(post("/queue/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
