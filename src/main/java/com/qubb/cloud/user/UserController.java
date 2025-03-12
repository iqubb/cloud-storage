package com.qubb.cloud.user;

import com.qubb.cloud.auth.UsernameResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UsernameResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        var response = userService.getCurrentUser(userDetails);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
