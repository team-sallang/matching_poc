package com.salang.matching_poc.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor // 코드 수정 필요.
@AllArgsConstructor // 코드 수정 필요.
public class AckResponse {
    private String status; // "IDLE"
}
