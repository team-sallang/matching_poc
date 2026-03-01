package com.salang.matching_poc.exception;

public class MatchAlreadyProcessedException extends IllegalStateException {

    public MatchAlreadyProcessedException(String message) {
        super(message);
    }
}
