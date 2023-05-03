package com.kakaoscan.profile.domain.exception;

public class InvalidAccessException extends Exception {
    public InvalidAccessException() {
        super();
    }

    public InvalidAccessException(String message) {
        super(message);
    }
}
