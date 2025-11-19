package com.salang.matching_poc.controller;

import com.salang.matching_poc.model.dto.MatchHistoryResponse;
import com.salang.matching_poc.repository.MatchHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
public class HistoryController {

    private final MatchHistoryRepository matchHistoryRepository;

    @GetMapping
    public ResponseEntity<List<MatchHistoryResponse>> getHistory(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("matchedAt").descending());
        List<MatchHistoryResponse> history = matchHistoryRepository.findAll(pageable).stream()
                .map(MatchHistoryResponse::new).toList();
        return ResponseEntity.ok(history);
    }
}
