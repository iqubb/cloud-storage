package com.qubb.cloud.auth;

import com.qubb.cloud.user.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sign-up")
    public ResponseEntity<UsernameResponse> register(
            @Valid @RequestBody UserCredentials request) {

        var response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/sign-in")
    public ResponseEntity<UsernameResponse> authenticate(
            @Valid @RequestBody UserCredentials request) {

        var response = authService.authenticate(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/sign-out")
    public ResponseEntity<?> logout(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletRequest request) {

        var session = request.getSession(false);
        authService.logout(userDetails, session);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
