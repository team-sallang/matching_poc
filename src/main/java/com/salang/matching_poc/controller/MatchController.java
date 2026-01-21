package com.salang.matching_poc.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.salang.matching_poc.constants.MatchingConstants;
import com.salang.matching_poc.controller.dto.MatchRequest;
import com.salang.matching_poc.controller.dto.MatchResponse;
import com.salang.matching_poc.service.MatchService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/match")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @PostMapping
    public ResponseEntity<MatchResponse<?>> requestMatch(@RequestBody @Valid MatchRequest request) {
        MatchResponse<?> response = matchService.requestMatch(request);
        if (MatchingConstants.WAITING_STATUS.equals(response.getStatus())) {
            return ResponseEntity.accepted().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> cancelMatch(@RequestBody @Valid MatchRequest request) {
        matchService.cancelMatch(request);
        return ResponseEntity.noContent().build();
    }
}
