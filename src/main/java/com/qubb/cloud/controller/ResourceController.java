package com.qubb.cloud.controller;

import com.qubb.cloud.payload.ResourceInfoResponse;
import com.qubb.cloud.service.ResourceService;
import com.qubb.cloud.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resource")
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "sessionCookie")
@Tag(name = "Resource", description = "Operations for managing resources")
public class ResourceController {

    private final ResourceService resourceService;

    @Operation(
            summary = "Get Resource Information",
            description = """
            Returns information about the requested resource (file or folder).
            The 'path' query parameter must contain the full URL-encoded path to the resource.
            For folders, the path must end with a '/'.
            """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Resource information retrieved successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResourceInfoResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid or missing path"),
                    @ApiResponse(responseCode = "401", description = "User not authorized"),
                    @ApiResponse(responseCode = "404", description = "Resource not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping
    public ResponseEntity<ResourceInfoResponse> getResourceInfo(
            @RequestParam("path") String path,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        var response = resourceService.getResourceInfo(path, userDetails);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete Resource",
            description = """
            Deletes the specified resource by its full URL-encoded path.
            For folders, the path must end with a '/'.
            On success, returns 204 No Content with no response body.
            """,
            responses = {
                    @ApiResponse(responseCode = "204", description = "Resource deleted successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid or missing path"),
                    @ApiResponse(responseCode = "401", description = "User not authorized"),
                    @ApiResponse(responseCode = "404", description = "Resource not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @DeleteMapping
    public ResponseEntity<Void> deleteResource(
            @RequestParam("path") String path,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        resourceService.deleteResource(path, userDetails);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Download Resource",
            description = """
            Downloads the specified resource by its full URL-encoded path.
            If the resource is a file, its binary content is returned with
            Content-Type: application/octet-stream. If the resource is a folder,
            a ZIP archive containing its contents is returned.
            """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Resource downloaded successfully",
                            content = @Content(mediaType = "application/octet-stream")
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid or missing path"),
                    @ApiResponse(responseCode = "401", description = "User not authorized"),
                    @ApiResponse(responseCode = "404", description = "Resource not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadResource(
            @RequestParam("path") String path,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        var response = resourceService.downloadResource(path, userDetails);
        return ResponseEntity.ok()
                .contentType(response.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + response.filename() + "\"")
                .body(response.resource());
    }

    @Operation(
            summary = "Move or Rename Resource",
            description = """
            Moves or renames a resource (file or folder) based on the provided parameters.
            If only the name changes, the resource is renamed. If only the path changes, the resource is moved.
            The 'from' and 'to' query parameters represent the full URL-encoded paths to the resource.
            """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Resource moved/renamed successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResourceInfoResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid or missing path"),
                    @ApiResponse(responseCode = "401", description = "User not authorized"),
                    @ApiResponse(responseCode = "404", description = "Resource not found"),
                    @ApiResponse(responseCode = "409", description = "Resource already exists at destination"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/move")
    public ResponseEntity<ResourceInfoResponse> moveResource(
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        var response = resourceService.moveResource(from, to, userDetails);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Search Resources",
            description = """
            Searches for resources using the given URL-encoded query parameter.
            Returns a JSON array containing the resources that match the query.
            Each resource includes the path to the parent folder, the resource name,
            the file size (if the resource is a file), and the resource type (FILE or DIRECTORY).
            """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Search completed successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResourceInfoResponse[].class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid or missing search query"),
                    @ApiResponse(responseCode = "401", description = "User not authorized"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/search")
    public ResponseEntity<List<ResourceInfoResponse>> search(
            @RequestParam("query") String query,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        var response = resourceService.search(query, userDetails);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Upload Resources",
            description = """
            Uploads one or more files to the specified target folder.
            The 'path' query parameter defines the folder in which the resources will be stored.
            If a file's name includes a subdirectory (e.g., 'upload_folder/test.txt'),
            the server creates that subdirectory within the storage folder.
            """,
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Resources uploaded successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResourceInfoResponse[].class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid request body"),
                    @ApiResponse(responseCode = "404", description = "Target folder not found"),
                    @ApiResponse(responseCode = "409", description = "File already exists"),
                    @ApiResponse(responseCode = "401", description = "User not authorized"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ResourceInfoResponse>> uploadResources(
            @RequestParam("path") String targetPath,
            @RequestParam("object") MultipartFile[] files,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        var response = resourceService.uploadResources(targetPath, files, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
