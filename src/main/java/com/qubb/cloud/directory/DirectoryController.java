package com.qubb.cloud.directory;

import com.qubb.cloud.resource.ResourceInfoResponse;
import com.qubb.cloud.user.UserDetailsImpl;
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
import org.springframework.web.bind.annotation.*;
import java.util.List;

@SecurityRequirement(name = "sessionCookie")
@Tag(name = "Directory", description = "Operations for managing directories")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/directory")
public class DirectoryController {

    private final DirectoryService directoryService;

    @Operation(
            summary = "Create Empty Folder",
            description = """
            Creates an empty folder in storage.
            The 'path' query parameter specifies the full path to the new folder.
            For example, if the existing structure is 'folder1/folder2/' and you want to create a new folder named 'folder3'
            within it, you should pass 'folder1/folder2/folder3/' as the path.
            On success, returns 201 Created with a JSON array containing the created folder resource.
            Errors:
              400 - Invalid or missing path for the new folder.
              401 - User not authorized.
              404 - Parent directory does not exist.
              409 - Folder already exists.
              500 - Unknown error.
            """,
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Folder created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResourceInfoResponse[].class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid or missing folder path"),
                    @ApiResponse(responseCode = "401", description = "User not authorized"),
                    @ApiResponse(responseCode = "404", description = "Parent directory does not exist"),
                    @ApiResponse(responseCode = "409", description = "Folder already exists"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PostMapping
    public ResponseEntity<?> createEmptyFolder(
            @RequestParam("path") String path,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        var response = directoryService.createEmptyFolder(path, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get Directory Content",
            description = """
            Retrieves a list of resources located within the specified directory (non-recursively).
            The 'path' query parameter must contain the full path to the directory, and for folders, the path must end with a '/'.
            On success, returns 200 OK with a JSON array of resource information.
            Errors:
              400 - Invalid or missing path.
              401 - User not authorized.
              404 - Directory not found.
              500 - Unknown error.
            """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Directory content retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ResourceInfoResponse[].class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid or missing path"),
                    @ApiResponse(responseCode = "401", description = "User not authorized"),
                    @ApiResponse(responseCode = "404", description = "Directory not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping
    public ResponseEntity<List<ResourceInfoResponse>> getDirectoryContentInfo(
            @RequestParam("path") String path,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        var response = directoryService.getDirectoryContentInfo(path, userDetails);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
