package com.salang.matching_poc.exception;

public class AlreadyInQueueException extends RuntimeException {
    public AlreadyInQueueException() {
        super("ALREADY_IN_QUEUE");
    }
}

