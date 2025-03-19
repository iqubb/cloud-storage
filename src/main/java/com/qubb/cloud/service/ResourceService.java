package com.qubb.cloud.service;

import com.qubb.cloud.exception.ResourceNotFoundException;
import com.qubb.cloud.exception.UserNotFoundException;
import com.qubb.cloud.payload.DownloadResponse;
import com.qubb.cloud.payload.ResourceInfoResponse;
import com.qubb.cloud.storage.StorageOperations;
import com.qubb.cloud.security.UserDetailsImpl;
import com.qubb.cloud.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final StorageOperations storageOperations;
    private final ValidationFacade validationFacade;

    public ResourceInfoResponse getResourceInfo(String path, UserDetailsImpl userDetails) {
        validationFacade.validateRequest(userDetails, path);
        String objectName = PathUtils.buildFullUserPath(getUserId(userDetails), path);
        if (!validationFacade.isSourceResourceExists(objectName)) {
            throw new ResourceNotFoundException(objectName);
        }
        return ResourceResponseBuilder.buildFromObjectName(objectName, storageOperations.statObject(objectName));
    }

    public DownloadResponse downloadResource(String path, UserDetailsImpl userDetails) {
        validationFacade.validateRequest(userDetails, path);
        return storageOperations.download(path);
    }

    public List<ResourceInfoResponse> uploadResources(String targetPath,
                                                      MultipartFile[] files,
                                                      UserDetailsImpl userDetails) {
        validationFacade.validateRequest(userDetails, targetPath);
        String fullPath = PathUtils.buildFullUserPath(getUserId(userDetails), targetPath);
        return storageOperations.upload(files, fullPath);
    }

    public void deleteResource(String path, UserDetailsImpl userDetails) {
        validationFacade.validateRequest(userDetails, path);
        storageOperations.delete(path);
    }

    public ResourceInfoResponse moveResource(String from, String to, UserDetailsImpl userDetails) {
        validationFacade.validateRequest(userDetails, from, to);
        from = PathUtils.buildFullUserPath(getUserId(userDetails), from);
        to = PathUtils.buildFullUserPath(getUserId(userDetails), to);
        validationFacade.isSourceResourceExists(from);
        validationFacade.checkTargetParentExists(to);
        storageOperations.copyResource(from, to);
        deleteResource(from, userDetails);
        return ResourceResponseBuilder.buildFromObjectName(to, storageOperations.statObject(to));
    }

    public List<ResourceInfoResponse> search(String query, UserDetailsImpl userDetails) {
        validationFacade.validateRequest(userDetails, query);
        String rootPath = PathUtils.buildUserRootPath(getUserId(userDetails));
        return storageOperations.recursiveListObjects(rootPath)
                .filter(item -> matchesSearch(item.objectName(), rootPath, query))
                .map(item -> ResourceResponseBuilder.buildFromObjectName(item.objectName(), storageOperations.statObject(item.objectName())))
                .collect(Collectors.toList());
    }

    private boolean matchesSearch(String objectName, String userPrefix, String query) {
        String relativePath = objectName.substring(userPrefix.length());
        return relativePath.toLowerCase().contains(query.toLowerCase());
    }

    private int getUserId(UserDetailsImpl user) {
        if (user == null || user.user() == null) {
            throw new UserNotFoundException("User not authenticated");
        }
        return user.user().getId();
    }
}
