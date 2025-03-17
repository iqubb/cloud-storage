package com.qubb.cloud.utils;

import com.qubb.cloud.exceptions.UserNotFoundException;
import com.qubb.cloud.user.UserDetailsImpl;
import org.springframework.stereotype.Component;

@Component
public class RequestValidator {
    public void validateUserAndPath(UserDetailsImpl userDetails, String path) {
        if (userDetails == null || userDetails.user() == null) {
            throw new UserNotFoundException("User not found");
        }
        PathUtils.validatePath(path);
    }
}
