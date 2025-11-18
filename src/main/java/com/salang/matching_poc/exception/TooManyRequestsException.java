package com.salang.matching_poc.exception;

public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException() {
        super("TOO_MANY_REQUESTS");
    }
}

