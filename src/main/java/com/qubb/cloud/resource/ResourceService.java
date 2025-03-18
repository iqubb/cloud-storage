package com.qubb.cloud.resource;

import com.qubb.cloud.exceptions.ResourceNotFoundException;
import com.qubb.cloud.exceptions.ResourceOperationException;
import com.qubb.cloud.exceptions.UserNotFoundException;
import com.qubb.cloud.minio.DeleteService;
import com.qubb.cloud.minio.DownloadService;
import com.qubb.cloud.minio.MinioService;
import com.qubb.cloud.user.UserDetailsImpl;
import com.qubb.cloud.utils.PathUtils;
import com.qubb.cloud.utils.RequestValidator;
import com.qubb.cloud.utils.ResourceResponseBuilder;
import com.qubb.cloud.utils.ResourceValidator;
import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final MinioClient minioClient;
    private final RequestValidator requestValidator;
    private final MinioService minioService;
    private final DeleteService deleteService;
    private final DownloadService downloadService;
    private final ResourceValidator resourceValidator;

    @Value("${minio.bucket}")
    private String bucketName;

    public ResourceInfoResponse getResourceInfo(String path, UserDetailsImpl userDetails) {
        requestValidator.validateUserAndPath(userDetails, path);

        String objectName = PathUtils.buildFullUserPath(getUserId(userDetails), path);
        if (!resourceValidator.isSourceResourceExists(objectName)) {
            throw new ResourceNotFoundException(objectName);
        }
        return buildResponse(objectName);
    }

    private int getUserId(UserDetailsImpl user) {
        if (user == null || user.user() == null) {
            throw new UserNotFoundException("User not authenticated");
        }
        return user.user().getId();
    }

    public void deleteResource(String path, UserDetailsImpl userDetails) {
        requestValidator.validateUserAndPath(userDetails, path);
        if (path.endsWith("/")) {
            deleteService.deleteDirectory(path);
        } else {
            deleteService.deleteFile(path);
        }
    }

    public DownloadResult downloadResource(String path, UserDetailsImpl userDetails) {
        requestValidator.validateUserAndPath(userDetails, path);
        if (path.endsWith("/")) {
            return downloadService.downloadDirectory(path);
        } else {
            return downloadService.downloadFile(path);
        }
    }

    public ResourceInfoResponse moveResource(String from, String to, UserDetailsImpl userDetails) {
        requestValidator.validateUserAndPath(userDetails, from);
        requestValidator.validateUserAndPath(userDetails, to);

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
        requestValidator.validateUserAndPath(userDetails, query);

        String rootPath = PathUtils.buildUserRootPath(getUserId(userDetails));

        return minioService.recursiveListObjects(rootPath)
                .filter(item -> matchesSearch(item.objectName(), rootPath, query))
                .map(item -> mapToResponse(item.objectName(), item))
                .collect(Collectors.toList());
    }

    public List<ResourceInfoResponse> uploadResources(String targetPath, MultipartFile[] files, UserDetailsImpl userDetails) {
        requestValidator.validateUserAndPath(userDetails, targetPath);
        String prefix = "user-" + userDetails.user().getId() + "-files/";
        String fullPath = prefix + targetPath;

        minioService.isDirectoryExists(fullPath);

        return Arrays.stream(files)
                .flatMap(file -> processFile(file, fullPath).stream())
                .collect(Collectors.toList());
    }

    private List<ResourceInfoResponse> processFile(MultipartFile file, String basePath) {
        try {
            String fileName = file.getOriginalFilename();
            String objectName = basePath + fileName;
            if (minioService.objectExists(objectName)) {
                throw new ResourceOperationException("File already exists: " + objectName);
            }
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return List.of(ResourceInfoResponse.builder()
                    .path(PathUtils.getParentPath(objectName))
                    .name(PathUtils.getResourceName(objectName))
                    .size(file.getSize())
                    .type("FILE")
                    .build()
            );
        } catch (Exception exception) {
            throw new ResourceOperationException("Failed to upload file: " + exception.getMessage());
        }
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
