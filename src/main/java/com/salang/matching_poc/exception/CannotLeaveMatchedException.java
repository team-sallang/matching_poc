package com.salang.matching_poc.exception;

public class CannotLeaveMatchedException extends RuntimeException {
    public CannotLeaveMatchedException() {
        super("CANNOT_LEAVE_MATCHED");
    }
}

