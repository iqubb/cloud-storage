package com.qubb.cloud.service;

import com.qubb.cloud.payload.UsernameResponse;
import com.qubb.cloud.exception.UserNotAuthorizedException;
import com.qubb.cloud.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserService {

    public UsernameResponse getCurrentUser(UserDetailsImpl userDetails) {
        var user = userDetails.user();
        if (user == null) {
            throw new UserNotAuthorizedException("User not authorized");
        }
        return UsernameResponse.builder()
                .username(user.getUsername())
                .build();
    }
}
