package com.salang.matching_poc.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.salang.matching_poc.model.dto.MatchHistoryResponse;
import com.salang.matching_poc.repository.MatchHistoryRepository;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
@Validated
public class HistoryController {

    private final MatchHistoryRepository matchHistoryRepository;

    @GetMapping
    public ResponseEntity<List<MatchHistoryResponse>> getHistory(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        log.debug("Fetching match history: page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("matchedAt").descending());
        List<MatchHistoryResponse> history = matchHistoryRepository.findAll(pageable).stream()
                .map(MatchHistoryResponse::new).toList();
        log.debug("Found {} match history records", history.size());
        return ResponseEntity.ok(history);
    }
}
