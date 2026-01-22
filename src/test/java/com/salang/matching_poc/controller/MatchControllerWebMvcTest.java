package com.salang.matching_poc.controller;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salang.matching_poc.constants.MatchingConstants;
import com.salang.matching_poc.controller.dto.MatchRequest;
import com.salang.matching_poc.controller.dto.MatchResponse;
import com.salang.matching_poc.exception.AlreadyInQueueException;
import com.salang.matching_poc.exception.GlobalExceptionHandler;
import com.salang.matching_poc.exception.UserNotFoundException;
import com.salang.matching_poc.exception.UserNotInQueueException;
import com.salang.matching_poc.service.MatchService;

@WebMvcTest(value = MatchController.class, excludeAutoConfiguration = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class
})
@Import(GlobalExceptionHandler.class)
@DisplayName("MatchController 슬라이스 테스트")
@SuppressWarnings("null")
class MatchControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MatchService matchService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("매칭 요청 성공 - 즉시 매칭된 경우 200 OK")
    void requestMatch_Matched_Returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        MatchRequest request = new MatchRequest(userId);
        MatchResponse<?> response = MatchResponse.matched(roomId, OffsetDateTime.now());

        doReturn(response).when(matchService).requestMatch(any(MatchRequest.class));

        mockMvc.perform(post("/api/v1/match")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(MatchingConstants.MATCHED_STATUS))
                .andExpect(jsonPath("$.data.room_id").value(roomId.toString()))
                .andExpect(jsonPath("$.data.matched_at").exists());
    }

    @Test
    @DisplayName("매칭 요청 성공 - 대기열 등록된 경우 202 Accepted")
    void requestMatch_Waiting_Returns202() throws Exception {
        UUID userId = UUID.randomUUID();
        MatchRequest request = new MatchRequest(userId);
        MatchResponse<?> response = MatchResponse.waiting("매칭 대기열에 등록되었습니다.", OffsetDateTime.now());

        doReturn(response).when(matchService).requestMatch(any(MatchRequest.class));

        mockMvc.perform(post("/api/v1/match")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(MatchingConstants.WAITING_STATUS))
                .andExpect(jsonPath("$.data.message").exists())
                .andExpect(jsonPath("$.data.queued_at").exists());
    }

    @Test
    @DisplayName("매칭 요청 실패 - user_id가 null이면 400 Bad Request")
    void requestMatch_InvalidRequest_Returns400() throws Exception {
        String invalidJson = "{}";

        mockMvc.perform(post("/api/v1/match")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("매칭 요청 실패 - 사용자 없음 404 Not Found")
    void requestMatch_UserNotFound_Returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        MatchRequest request = new MatchRequest(userId);

        when(matchService.requestMatch(any(MatchRequest.class)))
                .thenThrow(new UserNotFoundException());

        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/match")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("매칭 요청 실패 - 이미 큐에 있음 409 Conflict")
    void requestMatch_AlreadyInQueue_Returns409() throws Exception {
        UUID userId = UUID.randomUUID();
        MatchRequest request = new MatchRequest(userId);

        when(matchService.requestMatch(any(MatchRequest.class)))
                .thenThrow(new AlreadyInQueueException());

        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/v1/match")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("ALREADY_IN_QUEUE"));
    }

    @Test
    @DisplayName("매칭 취소 성공 - 204 No Content")
    void cancelMatch_Success_Returns204() throws Exception {
        UUID userId = UUID.randomUUID();
        MatchRequest request = new MatchRequest(userId);

        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(delete("/api/v1/match")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("매칭 취소 실패 - user_id가 null이면 400 Bad Request")
    void cancelMatch_InvalidRequest_Returns400() throws Exception {
        String invalidJson = "{}";

        mockMvc.perform(delete("/api/v1/match")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("매칭 취소 실패 - 큐에 없음 404 Not Found")
    void cancelMatch_NotInQueue_Returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        MatchRequest request = new MatchRequest(userId);

        doThrow(new UserNotInQueueException())
                .when(matchService).cancelMatch(any(MatchRequest.class));

        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(delete("/api/v1/match")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("NOT_IN_QUEUE"));
    }
}
