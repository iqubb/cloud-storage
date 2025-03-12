package com.qubb.cloud.exception;

public class DirectoryAlreadyExistsException extends RuntimeException {
    public DirectoryAlreadyExistsException(String message) {
        super(message);
    }
}
