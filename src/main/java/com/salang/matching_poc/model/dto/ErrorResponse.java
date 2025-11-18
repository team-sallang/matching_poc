package com.salang.matching_poc.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor // 코드 수정 필요.
@AllArgsConstructor // 코드 수정 필요.
public class ErrorResponse {
    private String error; // "ALREADY_IN_QUEUE" | "TOO_MANY_REQUESTS" | "CANNOT_LEAVE_MATCHED"
}
