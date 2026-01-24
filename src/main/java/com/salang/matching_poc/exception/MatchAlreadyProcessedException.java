package com.salang.matching_poc.exception;

public class MatchAlreadyProcessedException extends RuntimeException {

    public MatchAlreadyProcessedException(String message) {
        super(message);
    }
}
