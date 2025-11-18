package com.salang.matching_poc.controller;

import com.salang.matching_poc.model.dto.MatchHistoryResponse;
import com.salang.matching_poc.repository.MatchHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
public class HistoryController {

    private final MatchHistoryRepository matchHistoryRepository;

    @GetMapping
    public ResponseEntity<List<MatchHistoryResponse>> getHistory() {
        List<MatchHistoryResponse> history = matchHistoryRepository.findAll().stream()
                .map(MatchHistoryResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }
}
