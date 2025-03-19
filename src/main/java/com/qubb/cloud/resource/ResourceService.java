package com.qubb.cloud.resource;

import com.qubb.cloud.exceptions.ResourceNotFoundException;
import com.qubb.cloud.exceptions.UserNotFoundException;
import com.qubb.cloud.minio.MinioService;
import com.qubb.cloud.storage.StorageOperations;
import com.qubb.cloud.user.UserDetailsImpl;
import com.qubb.cloud.utils.PathUtils;
import com.qubb.cloud.utils.RequestValidator;
import com.qubb.cloud.utils.ResourceResponseBuilder;
import com.qubb.cloud.utils.ResourceValidator;
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
    private final RequestValidator requestValidator;
    private final StorageOperations storageOperations;
    private final ResourceValidator resourceValidator;
    private final MinioService minioService;

    public ResourceInfoResponse getResourceInfo(String path, UserDetailsImpl userDetails) {
        requestValidator.validateRequest(userDetails, path);
        String objectName = PathUtils.buildFullUserPath(getUserId(userDetails), path);
        if (!resourceValidator.isSourceResourceExists(objectName)) {
            throw new ResourceNotFoundException(objectName);
        }
        return ResourceResponseBuilder.buildFromObjectName(objectName, minioService.statObject(objectName));
    }

    public DownloadResult downloadResource(String path, UserDetailsImpl userDetails) {
        requestValidator.validateRequest(userDetails, path);
        return storageOperations.download(path);
    }

    public List<ResourceInfoResponse> uploadResources(String targetPath,
                                                      MultipartFile[] files,
                                                      UserDetailsImpl userDetails) {
        requestValidator.validateRequest(userDetails, targetPath);
        String fullPath = PathUtils.buildFullUserPath(getUserId(userDetails), targetPath);
        return storageOperations.upload(files, fullPath);
    }

    public void deleteResource(String path, UserDetailsImpl userDetails) {
        requestValidator.validateRequest(userDetails, path);
        storageOperations.delete(path);
    }

    public ResourceInfoResponse moveResource(String from, String to, UserDetailsImpl userDetails) {
        requestValidator.validateRequest(userDetails, from, to);
        from = PathUtils.buildFullUserPath(getUserId(userDetails), from);
        to = PathUtils.buildFullUserPath(getUserId(userDetails), to);
        resourceValidator.isSourceResourceExists(from);
        resourceValidator.checkTargetParentExists(to);
        storageOperations.copyResource(from, to);
        deleteResource(from, userDetails);
        return ResourceResponseBuilder.buildFromObjectName(to, minioService.statObject(to));
    }

    public List<ResourceInfoResponse> search(String query, UserDetailsImpl userDetails) {
        requestValidator.validateRequest(userDetails, query);
        String rootPath = PathUtils.buildUserRootPath(getUserId(userDetails));
        return storageOperations.recursiveListObjects(rootPath)
                .filter(item -> matchesSearch(item.objectName(), rootPath, query))
                .map(item -> ResourceResponseBuilder.buildFromObjectName(item.objectName(), minioService.statObject(item.objectName())))
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
