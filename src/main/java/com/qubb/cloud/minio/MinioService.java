package com.qubb.cloud.minio;

import com.qubb.cloud.exceptions.ResourceOperationException;
import com.qubb.cloud.utils.PathUtils;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    public void initializeBucket() {
        try {
            if (!bucketExists()) {
                createBucket();
            }
        } catch (Exception e) {
            throw new ResourceOperationException("Bucket initialization failed");
        }
    }

    public void removeObject(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new ResourceOperationException("Failed to remove object: " + objectName, e);
        }
    }

    public Stream<Item> recursiveListObjects(String prefix) {
        Iterable<Result<Item>> iterable = minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .recursive(true)
                .build());
        return StreamSupport.stream(iterable.spliterator(), false)
                .map(result -> {
                    try {
                        return result.get();
                    } catch (Exception e) {
                        throw new ResourceOperationException("Failed to process MinIO item", e);
                    }
                });
    }

    public boolean isDirectoryExists(String directoryPath) {
        if (directoryPath.isEmpty()) {
            return true;
        }
        final String normalizedPath = PathUtils.normalizeDirectoryPath(directoryPath);
        try {
            return checkExplicitDirectory(normalizedPath) || checkImplicitDirectory(normalizedPath);
        } catch (Exception e) {
            log.error("Directory existence check failed for path: {}", normalizedPath, e);
            return false;
        }
    }

    public boolean objectExists(String objectName) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void createDirectoryObject(String path) {
        String normalizedPath = PathUtils.normalizeDirectoryPath(path);
        try (var stream = new ByteArrayInputStream(new byte[0])) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(normalizedPath)
                    .stream(stream, 0, -1)
                    .contentType("application/x-directory")
                    .build());
        } catch (Exception e) {
            throw new ResourceOperationException("Directory creation failed: " + path);
        }
    }

    private boolean checkExplicitDirectory(String path) {
        return objectExists(path);
    }

    private boolean checkImplicitDirectory(String path) {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(path)
                            .maxKeys(1)
                            .build());
            return results.iterator().hasNext();
        } catch (Exception e) {
            throw new ResourceOperationException("Error listing directory contents", e);
        }
    }

    private boolean bucketExists() {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            throw new ResourceOperationException("Bucket check failed");
        }
    }

    private synchronized void createBucket() {
        try {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
            log.info("Bucket created: {}", bucketName);
        } catch (Exception e) {
            throw new ResourceOperationException("Bucket creation failed");
        }
    }
}
