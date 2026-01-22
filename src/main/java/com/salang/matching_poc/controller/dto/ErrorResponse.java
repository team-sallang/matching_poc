package com.salang.matching_poc.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String status = "ERROR";
    private final ErrorData error;

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorData(code, message));
    }

    @Getter
    @AllArgsConstructor
    private static class ErrorData {
        private String code;
        private String message;
    }
}
