package com.payprosys.exception;

/** Demo: 403 when role check fails. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
