package com.salang.matching_poc.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "User not in queue")
public class UserNotInQueueException extends RuntimeException {
    public UserNotInQueueException() {
        super("매칭 대기열에 등록되어 있지 않습니다.");
    }
}
