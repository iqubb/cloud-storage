package com.qubb.cloud.directory;

import com.qubb.cloud.exception.*;
import com.qubb.cloud.file.ResourceInfoResponse;
import com.qubb.cloud.user.UserDetailsImpl;
import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectoryService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    public List<ResourceInfoResponse> getDirectoryContentInfo(String path, UserDetailsImpl userDetails) {
        validateResourceRequest(path, userDetails);
        String prefix = "user-" + userDetails.user().getId() + "-files/";
        String fullPath = prefix + path;
        isDirectoryExists(fullPath);
        return getContent(fullPath);
    }

    public ResourceInfoResponse createEmptyFolder(String path, UserDetailsImpl userDetails) {
        validateResourceRequest(path, userDetails);

        // Нормализация пути
        String normalizedPath = path.endsWith("/") ? path : path + "/";
        String defaultPrefix = "user-" + userDetails.user().getId() + "-files/";
        String fullPath = defaultPrefix + normalizedPath;

        String parentPath = getParentPath(fullPath);
        boolean isParentDirectoryExists = isDirectoryExists(parentPath);
        boolean isNewEmptyDirectoryExists = isDirectoryExists(fullPath);
        if(isNewEmptyDirectoryExists) {
            throw new DirectoryAlreadyExistsException("Directory already exists");
        }


        try (InputStream stream = new ByteArrayInputStream(new byte[0])) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullPath)
                            .stream(stream, 0, -1)
                            .contentType("application/x-directory")
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to create folder: {}", fullPath, e);
            throw new RuntimeException("Failed to create folder", e);
        }

        return new ResourceInfoResponse(
                parentPath,
                getResourceName(fullPath),
                null,
                "DIRECTORY"
        );
    }

    private void validateResourceRequest(String path, UserDetailsImpl userDetails) {
        if (path == null || path.trim().isEmpty()) {
            throw new IncorrectPathException("Path cannot be empty");
        }
        if (userDetails == null || userDetails.user() == null) {
            throw new UserNotFoundException("User not found");
        }
    }

    private String getParentPath(String resourcePath) {
        if (resourcePath.endsWith("/")) {
            // Для папки: "folder1/folder2/" -> "folder1/"
            return resourcePath.substring(0, resourcePath.lastIndexOf("/", resourcePath.length() - 2) + 1);
        }
        // Для файла: "folder1/folder2/file.txt" -> "folder1/folder2/"
        return resourcePath.substring(0, resourcePath.lastIndexOf("/") + 1);
    }

    private String getResourceName(String resourcePath) {
        if (resourcePath.endsWith("/")) {
            // Для папки: "folder1/folder2/" -> "folder2"
            String[] parts = resourcePath.split("/", -1);
            return parts[parts.length - 2]; // Предпоследний элемент
        }
        // Для файла: "folder1/folder2/file.txt" -> "file.txt"
        return resourcePath.substring(resourcePath.lastIndexOf("/") + 1);
    }

    private boolean isDirectoryExists(String directoryPath) {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(directoryPath)
                            .maxKeys(1)
                            .build());

            if (!results.iterator().hasNext()) {
                throw new ResourceNotFoundException("Directory not found: " + directoryPath);
            }
            return true;
        } catch (Exception exception) {
            throw new ResourceNotFoundException(exception.getMessage());
        }
    }

    private List<ResourceInfoResponse> getContent(String directoryPath) {
        var content = new ArrayList<ResourceInfoResponse>();
        String prefix = directoryPath.endsWith("/") ? directoryPath : directoryPath + "/";
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(false)
                            .build()
            );
            Set<String> processedEntries = new HashSet<>();

            for (Result<Item> result : results) {
                Item item = result.get();
                String objectName = item.objectName();

                if(objectName.equals(prefix)) continue;
                String relativePath = objectName.substring(prefix.length());
                String[] parts = relativePath.split("/", 2);
                String entryName = parts[0];

                if (processedEntries.add(entryName)) {
                    content.add(mapToResource(prefix, entryName, parts.length > 1));
                }
            }
            return content;

        }catch (Exception exception) {
            throw new ResourceOperationException("Failed to list directory contents");
        }
    }

    private ResourceInfoResponse mapToResource(String prefix, String entryName, boolean isDirectory) {
        return ResourceInfoResponse.builder()
                .path(prefix)
                .name(isDirectory ? entryName + "/" : entryName)
                .size(isDirectory ? null : getFileSize(prefix + entryName))
                .type(isDirectory ? "DIRECTORY" : "FILE")
                .build();
    }

    private Long getFileSize(String objectName) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
            return stat.size();
        } catch (Exception e) {
            return null;
        }
    }
}
