package com.qubb.cloud.directory;

import com.qubb.cloud.file.ResourceInfoResponse;
import com.qubb.cloud.user.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/resource/directory")
public class DirectoryController {

    private final DirectoryService directoryService;

    @PostMapping
    public ResponseEntity<?> createEmptyFolder(
            @RequestParam("path") String path,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

            var response = directoryService.createEmptyFolder(path, userDetails);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ResourceInfoResponse>> getDirectoryContentInfo(
            @RequestParam("path") String path,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        var response = directoryService.getDirectoryContentInfo(path, userDetails);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
