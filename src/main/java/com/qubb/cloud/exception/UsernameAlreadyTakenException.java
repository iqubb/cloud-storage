package com.qubb.cloud.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class UsernameAlreadyTakenException extends RuntimeException {
    public UsernameAlreadyTakenException(String message) {
        super(message);
    }
}
