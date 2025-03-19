package com.qubb.cloud.controller;

import com.qubb.cloud.payload.UsernameResponse;
import com.qubb.cloud.security.UserDetailsImpl;
import com.qubb.cloud.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@SecurityRequirement(name = "sessionCookie")
@Tag(name = "User", description = "Operations related to the current user")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Get Current User",
            description = """
            Retrieves information about the currently authenticated user.
            Errors:
              401 - User is not authenticated.
              500 - Unknown server error.
            """,
            security = @SecurityRequirement(name = "sessionCookie"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved user info",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UsernameResponse.class)
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "User is not authenticated"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/me")
    public ResponseEntity<UsernameResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        var response = userService.getCurrentUser(userDetails);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
