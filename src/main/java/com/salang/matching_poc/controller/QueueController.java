package com.salang.matching_poc.controller;

import com.salang.matching_poc.model.dto.*;
import com.salang.matching_poc.service.MatchingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/queue")
@RequiredArgsConstructor
public class QueueController {

    private final MatchingService matchingService;

    @PostMapping("/join")
    public ResponseEntity<JoinQueueResponse> joinQueue(@Valid @RequestBody JoinQueueRequest request) {
        JoinQueueResponse response = matchingService.joinQueue(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/leave")
    public ResponseEntity<LeaveQueueResponse> leaveQueue(@Valid @RequestBody LeaveQueueRequest request) {
        LeaveQueueResponse response = matchingService.leaveQueue(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<StatusResponse> getStatus(@PathVariable("userId") UUID userId) {
        StatusResponse response = matchingService.getStatus(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ack")
    public ResponseEntity<AckResponse> acknowledgeMatch(@Valid @RequestBody AckRequest request) {
        AckResponse response = matchingService.acknowledgeMatch(request);
        return ResponseEntity.ok(response);
    }
}
