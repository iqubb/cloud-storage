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
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }
            log.error("MinIO storage error response: {}", e.getMessage());
            throw new ResourceOperationException("Storage operation failed", e);
        } catch (MinioException e) {
            log.error("MinIO API error: {}", e.getMessage());
            throw new ResourceOperationException("Storage communication failure", e);
        } catch (IOException e) {
            log.error("IO error during storage access: {}", e.getMessage());
            throw new ResourceOperationException("Storage connection problem", e);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Security configuration error: {}", e.getMessage());
            throw new ResourceOperationException("Storage authentication failure", e);
        } catch (Exception e) {
            log.error("Unexpected error during directory check: {}", e.getMessage());
            throw new ResourceOperationException("Storage operation unexpected error", e);
        }
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
