package com.qubb.cloud.auth;

import com.qubb.cloud.user.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication operations")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "User Registration",
            description = """
            Registers a new user with a given username and password.
            If successful, the user session is created immediately, and a cookie is set.
            Errors:
              400 - Validation errors (e.g., username is too short).
              409 - Username is already taken.
              500 - Unknown server error.
            """,
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "User successfully registered",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UsernameResponse.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "409", description = "Username is already taken"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PostMapping("/sign-up")
    public ResponseEntity<UsernameResponse> register(
            @Valid @RequestBody UserCredentials request) {

        var response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "User Authentication",
            description = """
            Authenticates a user with a username and password.
            On success, returns a JSON response with the username.
            Errors:
              400 - Validation errors (e.g., username is too short).
              401 - Invalid credentials (user not found or wrong password).
              500 - Unknown server error.
            """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User successfully authenticated",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UsernameResponse.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "401", description = "Invalid credentials"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PostMapping("/sign-in")
    public ResponseEntity<UsernameResponse> authenticate(
            @Valid @RequestBody UserCredentials request) {

        var response = authService.authenticate(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(
            summary = "User Logout",
            description = """
            Logs out the authenticated user by invalidating their session.
            No request body is required.
            Errors:
              401 - User is not authenticated.
              500 - Unknown server error.
            """,
            responses = {
                    @ApiResponse(responseCode = "204", description = "User successfully logged out"),
                    @ApiResponse(responseCode = "401", description = "User is not authenticated"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @SecurityRequirement(name = "basicAuth")
    @PostMapping("/sign-out")
    public ResponseEntity<?> logout(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletRequest request) {

        var session = request.getSession(false);
        authService.logout(userDetails, session);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
