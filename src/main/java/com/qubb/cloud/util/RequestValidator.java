package com.qubb.cloud.util;

import com.qubb.cloud.exception.IncorrectPathException;
import com.qubb.cloud.exception.UserNotFoundException;
import com.qubb.cloud.security.UserDetailsImpl;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@Component
public class RequestValidator {

    public void validateRequest(UserDetailsImpl user, String... paths) {
        validateUser(user);
        Stream.of(paths).forEach(this::validatePath);
    }

    private void validateUser(UserDetailsImpl user) {
        if (user == null || user.user() == null) {
            throw new UserNotFoundException("User not found");
        }
    }

    private void validatePath(String path) {
        if (path == null || path.isEmpty()) return;

        if (!path.matches("^[\\w\\-._!*'()/@$=:+;, ]+$")) {
            throw new IncorrectPathException("Invalid characters in path: " + path);
        }
        if (path.contains("..") || path.contains("//")) {
            throw new IncorrectPathException("Invalid path structure: " + path);
        }
    }
}
