package com.qubb.cloud.directory;

import com.qubb.cloud.exception.*;
import com.qubb.cloud.resource.ResourceInfoResponse;
import com.qubb.cloud.user.UserDetailsImpl;
import io.minio.*;
import io.minio.errors.MinioException;
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
        initBucket();
        ensureUserRootDirectoryExists(userDetails);

        validateResourceRequest(path, userDetails);
        String prefix = "user-" + userDetails.user().getId() + "-files/";
        String fullPath = prefix + path;

        if (!isDirectoryExists(fullPath)) {
            throw new ResourceNotFoundException("Directory not found: " + fullPath);
        }

        return getContent(fullPath);
    }

    private void ensureUserRootDirectoryExists(UserDetailsImpl userDetails) {
        String userRootPath = "user-" + userDetails.user().getId() + "-files/";
        if (!isDirectoryExists(userRootPath)) {
            createEmptyFolder("", userDetails);
        }
    }

    private void initBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            if (!exists) {
                synchronized (this) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                    }
                System.out.println("Корзина создана: " + bucketName);
            } else {
                System.out.println("Корзина уже существует: " + bucketName);
            }
        } catch (MinioException e) {
            throw new RuntimeException("Ошибка при инициализации корзины: " + bucketName, e);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при инициализации корзины", e);
        }
    }


    public ResourceInfoResponse createEmptyFolder(String path, UserDetailsImpl userDetails) {
        validateResourceRequest(path, userDetails);

        String normalizedPath = path.isEmpty() ? "" : (path.endsWith("/") ? path : path + "/");
        String defaultPrefix = "user-" + userDetails.user().getId() + "-files/";
        String fullPath = defaultPrefix + normalizedPath;

        String parentPath = getParentPath(fullPath);
        if (!parentPath.isEmpty() && !isDirectoryExists(parentPath)) {
            throw new ResourceNotFoundException("Parent directory does not exist");
        }

        if (isDirectoryExists(fullPath)) {
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

        if (userDetails == null || userDetails.user() == null) {
            throw new UserNotFoundException("User not found");
        }
    }

    private String getParentPath(String resourcePath) {
        if (resourcePath.isEmpty()) {
            return "";
        }
        if (resourcePath.endsWith("/")) {
            int lastSlashIndex = resourcePath.lastIndexOf('/', resourcePath.length() - 2);
            return lastSlashIndex == -1 ? "" : resourcePath.substring(0, lastSlashIndex + 1);
        } else {
            int lastSlashIndex = resourcePath.lastIndexOf('/');
            return lastSlashIndex == -1 ? "" : resourcePath.substring(0, lastSlashIndex + 1);
        }
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
            if (directoryPath.isEmpty()) {
                return true;
            }

            try {
                minioClient.statObject(StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(directoryPath)
                        .build());
                return true;
            } catch (Exception e) {
                Iterable<Result<Item>> results = minioClient.listObjects(
                        ListObjectsArgs.builder()
                                .bucket(bucketName)
                                .prefix(directoryPath.endsWith("/") ? directoryPath : directoryPath + "/")
                                .maxKeys(1)
                                .build());
                return results.iterator().hasNext();
            }
        } catch (Exception e) {
            log.error("Error checking directory existence: {}", directoryPath, e);
            return false;
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
