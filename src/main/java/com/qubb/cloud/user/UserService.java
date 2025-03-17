package com.qubb.cloud.user;

import com.qubb.cloud.auth.UsernameResponse;
import com.qubb.cloud.exceptions.UserNotAuthorizedException;
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
