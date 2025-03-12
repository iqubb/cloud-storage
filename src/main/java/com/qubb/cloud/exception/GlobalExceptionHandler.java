package com.qubb.cloud.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UsernameAlreadyTakenException.class)
    public ResponseEntity<ProblemDetail> handleUsernameAlreadyTakenException(
            UsernameAlreadyTakenException exception, HttpServletRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setType(URI.create("https://tools.ietf.org/html/rfc7807#section-3.1"));

        problemDetail.setTitle("Registration Error");
        problemDetail.setProperty("message", exception.getMessage());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(DirectoryAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleDirectoryAlreadyExistsException(
            DirectoryAlreadyExistsException exception, HttpServletRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setType(URI.create("https://tools.ietf.org/html/rfc7807#section-3.1"));

        problemDetail.setTitle("Folder creation error");
        problemDetail.setProperty("message", exception.getMessage());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(IncorrectPathException.class)
    public ResponseEntity<ProblemDetail> handleIncorrectPathException(
            IncorrectPathException exception, HttpServletRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setType(URI.create("https://tools.ietf.org/html/rfc7807#section-3.1"));

        problemDetail.setTitle("Get resource Error");
        problemDetail.setProperty("message", exception.getMessage());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFoundException(
            UserNotFoundException exception, HttpServletRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setType(URI.create("https://tools.ietf.org/html/rfc7807#section-3.1"));

        problemDetail.setTitle("User not authorized error");
        problemDetail.setProperty("message", exception.getMessage());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFoundException(
            ResourceNotFoundException exception, HttpServletRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setType(URI.create("https://tools.ietf.org/html/rfc7807#section-3.1"));

        problemDetail.setTitle("Get resource info error");
        problemDetail.setProperty("message", exception.getMessage());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception, HttpServletRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setType(URI.create("https://tools.ietf.org/html/rfc7807#section-3.1"));
        problemDetail.setTitle("Validation Error");
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        Map<String, String> errors = new HashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        problemDetail.setDetail("Validation failed for one or more fields.");
        problemDetail.setProperty("message", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(InvalidUserCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidUserCredentialsException(
            InvalidUserCredentialsException exception, HttpServletRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problemDetail.setType(URI.create("https://tools.ietf.org/html/rfc7807#section-3.1"));
        problemDetail.setTitle("Sign-in error");
        problemDetail.setProperty("message", exception.getMessage());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

    @ExceptionHandler({ResourceOperationException.class})
    public ProblemDetail handleResourceOperationException(ResourceOperationException exception, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "An unexpected error occurred");
        problemDetail.setType(URI.create("https://api.example.com/errors/internal-server-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("path", request.getRequestURI());
        problemDetail.setProperty("errorDetails", exception.getMessage());
        return problemDetail;
    }

    @ExceptionHandler({Exception.class})
    public ProblemDetail handleGenericException(Exception ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://api.example.com/errors/internal-server-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("path", request.getRequestURI());
        problemDetail.setProperty("errorDetails", ex.getMessage());
        return problemDetail;
    }
}
