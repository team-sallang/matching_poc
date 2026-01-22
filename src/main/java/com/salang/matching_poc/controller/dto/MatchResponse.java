package com.salang.matching_poc.controller.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.salang.matching_poc.constants.MatchingConstants;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchResponse<T> {

    private final String status;
    private final T data;

    public static MatchResponse<MatchedData> matched(UUID roomId, OffsetDateTime matchedAt) {
        return new MatchResponse<>(MatchingConstants.MATCHED_STATUS, new MatchedData(roomId, matchedAt));
    }

    public static MatchResponse<WaitingData> waiting(String message, OffsetDateTime queuedAt) {
        return new MatchResponse<>(MatchingConstants.WAITING_STATUS, new WaitingData(message, queuedAt));
    }

    @Getter
    @AllArgsConstructor
    public static class MatchedData {
        @JsonProperty("room_id") // camel case mapping 위함
        private UUID roomId;

        @JsonProperty("matched_at")
        private OffsetDateTime matchedAt;
    }

    @Getter
    @AllArgsConstructor
    public static class WaitingData {
        private String message;

        @JsonProperty("queued_at")
        private OffsetDateTime queuedAt;
    }
}
