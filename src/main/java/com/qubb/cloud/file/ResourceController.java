package com.qubb.cloud.file;

import com.qubb.cloud.user.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resource")
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public ResponseEntity<ResourceInfoResponse> getResourceInfo(
            @RequestParam("path") String path,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        var response = resourceService.getResourceInfo(path, userDetails);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteResource(
            @RequestParam("path") String path,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        resourceService.deleteResource(path, userDetails);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadResource(
            @RequestParam("path") String path,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        var response = resourceService.downloadResource(path, userDetails);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(response.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + response.filename() + "\"")
                .body(response.resource());
    }

    @GetMapping("/move")
    public ResponseEntity<ResourceInfoResponse> moveResource(
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        var response = resourceService.moveResource(from, to, userDetails);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ResourceInfoResponse>> search(
            @RequestParam("query") String query,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        var response = resourceService.search(query, userDetails);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping
    public ResponseEntity<List<ResourceInfoResponse>> uploadResources(
            @RequestParam("path") String targetPath,
            @RequestParam("files") MultipartFile[] files,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        var response = resourceService.uploadResources(targetPath, files, userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
