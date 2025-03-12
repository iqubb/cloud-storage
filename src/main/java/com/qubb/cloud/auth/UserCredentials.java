package com.qubb.cloud.auth;

import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UserCredentials(
        @Size(min = 3, max = 20, message = "Username length should be between 3 and 20") String username,
        @Size(min = 6, max = 20, message = "Password length should be between 6 and 20") String password
) {

}
