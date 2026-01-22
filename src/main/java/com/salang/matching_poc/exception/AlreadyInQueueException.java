package com.salang.matching_poc.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT, reason = "User already in queue")
public class AlreadyInQueueException extends RuntimeException {
    public AlreadyInQueueException() {
        super("이미 매칭 대기열에 등록되어 있습니다.");
    }
}
