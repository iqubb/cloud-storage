package com.qubb.cloud.directory;

import com.qubb.cloud.exceptions.*;
import com.qubb.cloud.minio.MinioService;
import com.qubb.cloud.resource.ResourceInfoResponse;
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectoryService {

    private final MinioClient minioClient;
    private final MinioService minioService;
    private final ResourceValidator resourceValidator;
    private final RequestValidator requestValidator;

    @Value("${minio.bucket}")
    private String bucketName;

    public List<ResourceInfoResponse> getDirectoryContentInfo(String path, UserDetailsImpl userDetails) {
        requestValidator.validateUserAndPath(userDetails, path);
        minioService.initializeBucket();

        ensureUserRootDirectoryExists(userDetails);

        String fullPath = PathUtils.buildFullUserPath(getUserId(userDetails), path);
        if (!minioService.isDirectoryExists(fullPath)) {
            throw new ResourceNotFoundException("Directory not found: " + fullPath);
        }
        return listDirectoryContents(fullPath);
    }

    public ResourceInfoResponse createEmptyFolder(String path, UserDetailsImpl userDetails) {
        requestValidator.validateUserAndPath(userDetails, path);

        String fullPath = PathUtils.normalize(PathUtils.buildFullUserPath(getUserId(userDetails), path));
        String parentPath = PathUtils.getParentPath(fullPath);

        if (!parentPath.isEmpty() && !minioService.isDirectoryExists(parentPath)) {
            throw new ResourceNotFoundException("Parent directory does not exist");
        } else if (minioService.isDirectoryExists(fullPath)) {
            throw new DirectoryAlreadyExistsException("Directory already exists");
        }

        minioService.createDirectoryObject(fullPath);
        return ResourceResponseBuilder.buildDirectoryResponse(fullPath);
    }


    private List<ResourceInfoResponse> listDirectoryContents(String directoryPath) {
        try {
            return StreamSupport.stream(listObjects(directoryPath).spliterator(), false)
                    .map(this::unwrapItemResult)
                    .filter(item -> !item.objectName().equals(directoryPath))
                    .map(item -> ResourceResponseBuilder.buildFromItem(item, directoryPath))
                    .distinct()
                    .toList();
        } catch (Exception e) {
            throw new ResourceOperationException("Directory listing failed: " + directoryPath);
        }
    }

    private Stream<Result<Item>> listObjects(String prefix) {
        return StreamSupport.stream(minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .recursive(false)
                .build()).spliterator(), false);
    }

    private Item unwrapItemResult(Result<Item> result) {
        try {
            return result.get();
        } catch (Exception e) {
            throw new ResourceOperationException("Failed to process MinIO item");
        }
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
