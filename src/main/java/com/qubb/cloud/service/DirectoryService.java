package com.qubb.cloud.service;

import com.qubb.cloud.exception.*;
import com.qubb.cloud.storage.MinioService;
import com.qubb.cloud.payload.ResourceInfoResponse;
import com.qubb.cloud.security.UserDetailsImpl;
import com.qubb.cloud.util.PathUtils;
import com.qubb.cloud.util.RequestValidator;
import com.qubb.cloud.util.ResourceResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectoryService {

    private final MinioService minioService;
    private final RequestValidator requestValidator;

    public List<ResourceInfoResponse> getDirectoryContentInfo(String path, UserDetailsImpl userDetails) {
        requestValidator.validateRequest(userDetails, path);
        minioService.createBucketIfNotExists();

        ensureUserRootDirectoryExists(userDetails);

        String fullPath = PathUtils.buildFullUserPath(getUserId(userDetails), path);
        if (!minioService.isDirectoryExists(fullPath)) {
            throw new ResourceNotFoundException("Directory not found: " + fullPath);
        }
        return minioService.listObjects(fullPath)
                .filter(item -> !item.objectName().equals(fullPath))
                .map(ResourceResponseBuilder::buildFromItem)
                .distinct()
                .toList();
    }

    public ResourceInfoResponse createEmptyFolder(String path, UserDetailsImpl userDetails) {
        requestValidator.validateRequest(userDetails, path);

        String fullPath = PathUtils.normalize(PathUtils.buildFullUserPath(getUserId(userDetails), path));
        String parentPath = PathUtils.getParentPath(fullPath);

        if (!parentPath.isEmpty() && !minioService.isDirectoryExists(parentPath)) {
            throw new ResourceNotFoundException("Parent directory does not exist");
        } else if (minioService.isDirectoryExists(fullPath)) {
            throw new DirectoryAlreadyExistsException("Directory already exists");
        }

        minioService.createDirectoryObject(fullPath);
        return ResourceResponseBuilder.buildFromObjectName(fullPath, minioService.statObject(fullPath));
    }

    private void ensureUserRootDirectoryExists(UserDetailsImpl userDetails) {
        String userRootPath = PathUtils.buildUserRootPath(getUserId(userDetails));
        if (!minioService.isDirectoryExists(userRootPath)) {
            minioService.createDirectoryObject(userRootPath);
        }
    }

    private int getUserId(UserDetailsImpl user) {
        if (user == null || user.user() == null) {
            throw new UserNotFoundException("User not authenticated");
        }
        return user.user().getId();
    }
}
