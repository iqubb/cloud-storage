package com.qubb.cloud.storage;

import com.qubb.cloud.exception.ResourceOperationException;
import com.qubb.cloud.util.PathUtils;
import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    public void createBucketIfNotExists() {
        try {
            if (!bucketExists()) {
                createBucket();
            }
        } catch (Exception e) {
            throw new ResourceOperationException("Bucket initialization failed: " + bucketName, e);
        }
    }

    public InputStream getObject(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            throw new ResourceOperationException("Failed to get object: " + objectName, e);
        }
    }

    public void putObject(String objectName, InputStream stream, Long size, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(stream, size, -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            throw new ResourceOperationException("Failed to put object: " + objectName, e);
        }
    }

    public StatObjectResponse statObject(String objectName) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            throw new ResourceOperationException("Failed to stat object: " + objectName, e);
        }
    }

    public void copyObject(String source, String target) {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(target)
                            .source(CopySource.builder()
                                    .bucket(bucketName)
                                    .object(source)
                                    .build())
                            .build()
            );
        } catch (Exception e) {
            throw new ResourceOperationException("Copy failed from " + source + " to " + target, e);
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
            throw new ResourceOperationException("Directory creation failed: " + path, e);
        }
    }

    public void copyDirectoryContents(String source, String target) {
        recursiveListObjects(source)
                .forEach(item -> {
                    String sourceKey = item.objectName();
                    String targetKey = target + sourceKey.substring(source.length());
                    copyObject(sourceKey, targetKey);
                });
    }

    public Stream<Item> recursiveListObjects(String prefix) {
        return listObjectsInternal(prefix, true);
    }

    public Stream<Item> listObjects(String prefix) {
        return listObjectsInternal(prefix, false);
    }

    public boolean isDirectory(String objectName) {
        return objectName.endsWith("/") || hasChildrenInPath(objectName);
    }

    public boolean isDirectoryExists(String directoryPath) {
        if (directoryPath.isEmpty()) {
            return true;
        }
        final String normalizedPath = PathUtils.normalizeDirectoryPath(directoryPath);
        try {
            return existsAsDirectoryObject(normalizedPath) || hasChildrenInPath(normalizedPath);
        } catch (Exception e) {
            log.error("Directory existence check failed for path: {}", normalizedPath, e);
            return false;
        }
    }

    private Stream<Item> listObjectsInternal(String prefix, boolean recursive) {
        Iterable<Result<Item>> iterable = minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .recursive(recursive)
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

    private boolean existsAsDirectoryObject(String path) {
        return objectExists(path);
    }

    private boolean hasChildrenInPath(String path) {
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
