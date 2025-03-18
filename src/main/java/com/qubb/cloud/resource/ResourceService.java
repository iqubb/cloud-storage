package com.qubb.cloud.resource;

import com.qubb.cloud.exceptions.ResourceNotFoundException;
import com.qubb.cloud.exceptions.ResourceOperationException;
import com.qubb.cloud.exceptions.UserNotFoundException;
import com.qubb.cloud.minio.DeleteService;
import com.qubb.cloud.minio.DownloadService;
import com.qubb.cloud.minio.MinioService;
import com.qubb.cloud.minio.UploadService;
import com.qubb.cloud.user.UserDetailsImpl;
import com.qubb.cloud.utils.PathUtils;
import com.qubb.cloud.utils.RequestValidator;
import com.qubb.cloud.utils.ResourceResponseBuilder;
import com.qubb.cloud.utils.ResourceValidator;
import io.minio.*;
import io.minio.messages.Item;
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
    private final MinioService minioService;
    private final DeleteService deleteService;
    private final DownloadService downloadService;
    private final UploadService uploadService;
    private final ResourceValidator resourceValidator;

    public ResourceInfoResponse getResourceInfo(String path, UserDetailsImpl userDetails) {
        requestValidator.validateRequest(userDetails, path);

        String objectName = PathUtils.buildFullUserPath(getUserId(userDetails), path);
        if (!resourceValidator.isSourceResourceExists(objectName)) {
            throw new ResourceNotFoundException(objectName);
        }
        return buildResponse(objectName);
    }


    public void deleteResource(String path, UserDetailsImpl userDetails) {
        requestValidator.validateRequest(userDetails, path);
        deleteService.delete(path);
    }

    public DownloadResult downloadResource(String path, UserDetailsImpl userDetails) {
        requestValidator.validateRequest(userDetails, path);
        return downloadService.download(path);
    }

    public List<ResourceInfoResponse> uploadResources(String targetPath, MultipartFile[] files, UserDetailsImpl userDetails) {
        requestValidator.validateRequest(userDetails, targetPath);

        String fullPath = PathUtils.buildFullUserPath(getUserId(userDetails), targetPath);
        return uploadService.upload(files, fullPath);

    }
    public ResourceInfoResponse moveResource(String from, String to, UserDetailsImpl userDetails) {
        requestValidator.validateRequest(userDetails, from, to);


        String targetPath = PathUtils.buildFullUserPath(getUserId(userDetails), to);
        resourceValidator.isSourceResourceExists(from);
        resourceValidator.checkTargetParentExists(targetPath);

        minioService.copyResource(from, targetPath);
        deleteResource(from, userDetails);

        return buildResponse(targetPath);
    }

    private boolean matchesSearch(String objectName, String userPrefix, String query) {
        String relativePath = objectName.substring(userPrefix.length());
        return relativePath.toLowerCase().contains(query.toLowerCase());
    }

    public List<ResourceInfoResponse> search(String query, UserDetailsImpl userDetails) {
        requestValidator.validateRequest(userDetails, query);

        String rootPath = PathUtils.buildUserRootPath(getUserId(userDetails));

        return minioService.recursiveListObjects(rootPath)
                .filter(item -> matchesSearch(item.objectName(), rootPath, query))
                .map(item -> mapToResponse(item.objectName(), item))
                .collect(Collectors.toList());
    }

    private int getUserId(UserDetailsImpl user) {
        if (user == null || user.user() == null) {
            throw new UserNotFoundException("User not authenticated");
        }
        return user.user().getId();
    }

    private ResourceInfoResponse mapToResponse(String objectName, Item item) {
        boolean isDirectory = objectName.endsWith("/");
        String parentPath = PathUtils.getParentPath(objectName);
        String name = PathUtils.getResourceName(objectName);

        return ResourceInfoResponse.builder()
                .path(parentPath)
                .name(name)
                .size(isDirectory ? null : item.size())
                .type(isDirectory ? "DIRECTORY" : "FILE")
                .build();
    }

    private ResourceInfoResponse buildResponse(String objectName) {
        if (minioService.isDirectory(objectName)) {
            return ResourceResponseBuilder.buildDirectoryResponse(objectName);
        }
        try {
            StatObjectResponse stat = minioService.statObject(objectName);
            return ResourceResponseBuilder.buildFileResponse(objectName, stat.size());
        } catch (Exception e) {
            throw new ResourceOperationException("Failed to get resource info: " + e.getMessage());
        }
    }
}
