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
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final URI PROBLEM_TYPE_URI = URI.create("https://tools.ietf.org/html/rfc7807#section-3.1");
    private static final String TIMESTAMP_PROPERTY = "timestamp";
    private static final String ERROR_DETAILS_PROPERTY = "errors";

    private ProblemDetail createBaseProblemDetail(HttpStatus status, String title, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setType(PROBLEM_TYPE_URI);
        problemDetail.setTitle(title);
        problemDetail.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }

    @ExceptionHandler(UsernameAlreadyTakenException.class)
    public ResponseEntity<ProblemDetail> handleUsernameAlreadyTakenException(
            UsernameAlreadyTakenException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = createBaseProblemDetail(
                HttpStatus.CONFLICT,
                "Username Conflict",
                request
        );
        problemDetail.setDetail(ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }


    @ExceptionHandler(IncorrectPathException.class)
    public ResponseEntity<ProblemDetail> handleIncorrectPathException(
            IncorrectPathException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = createBaseProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid Path",
                request
        );
        problemDetail.setDetail(ex.getMessage());
        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFoundException(
            UserNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = createBaseProblemDetail(
                HttpStatus.NOT_FOUND,
                "User Not Found",
                request
        );
        problemDetail.setDetail(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = createBaseProblemDetail(
                HttpStatus.NOT_FOUND,
                "Resource Not Found",
                request
        );
        problemDetail.setDetail(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ValidationError(
                        fieldError.getField(),
                        fieldError.getDefaultMessage(),
                        fieldError.getRejectedValue())
                )
                .collect(Collectors.toList());

        ProblemDetail problemDetail = createBaseProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Validation Error",
                request
        );
        problemDetail.setProperty(ERROR_DETAILS_PROPERTY, errors);
        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(InvalidUserCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidUserCredentialsException(
            HttpServletRequest request) {
        ProblemDetail problemDetail = createBaseProblemDetail(
                HttpStatus.UNAUTHORIZED,
                "Authentication Failed",
                request
        );
        problemDetail.setDetail("Invalid username or password");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

    @ExceptionHandler(ResourceOperationException.class)
    public ResponseEntity<ProblemDetail> handleResourceOperationException(
            ResourceOperationException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = createBaseProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Resource Operation Error",
                request
        );
        problemDetail.setDetail(ex.getMessage());
        return ResponseEntity.internalServerError().body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleAllExceptions(
            Exception ex, HttpServletRequest request) {
        ProblemDetail problemDetail = createBaseProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                request
        );
        problemDetail.setDetail("An unexpected error occurred");
        problemDetail.setProperty(ERROR_DETAILS_PROPERTY, ex.getMessage());
        return ResponseEntity.internalServerError().body(problemDetail);
    }

    private record ValidationError(String field, String message, Object rejectedValue) {}
}