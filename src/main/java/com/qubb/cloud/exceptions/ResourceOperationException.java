package com.qubb.cloud.exceptions;

public class ResourceOperationException extends RuntimeException {
    public ResourceOperationException(String message) {
        super(message);
    }
    public ResourceOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
