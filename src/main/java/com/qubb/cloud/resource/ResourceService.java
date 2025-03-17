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
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
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

    @Value("${minio.bucket}")
    private String bucketName;

    public ResourceInfoResponse getResourceInfo(String path, UserDetailsImpl userDetails) {
        requestValidator.validateUserAndPath(userDetails, path);

        String objectName = PathUtils.buildFullUserPath(getUserId(userDetails), path);
        if(!isSourceResourceExists(objectName)) {
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
        try {
            if (path.endsWith("/")) {
                deleteService.deleteDirectory(path);
            } else {
                deleteService.deleteFile(path);
            }
        } catch (Exception e) {
            throw new ResourceNotFoundException("Delete operation failed for: " + path, e);
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
        //String basePrefix = "user-" + userDetails.user().getId() + "-files/";
        String sourceObject = from;
        String targetObject = to;

        isSourceResourceExists(sourceObject);
        checkTargetParentExists(targetObject);

        copyResource(sourceObject, targetObject);
        deleteResource(sourceObject);

        return buildResponse(targetObject);
    }

    public List<ResourceInfoResponse> search(String query, UserDetailsImpl userDetails) {
        requestValidator.validateUserAndPath(userDetails, query);

        String prefix = "user-" + userDetails.user().getId() + "-files/";
        try {
            var response = new ArrayList<ResourceInfoResponse>();
            Iterable<Result<Item>> items = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );
            for (Result<Item> result : items) {
                var item = result.get();
                String objectName = item.objectName();
                String relativePath = objectName.substring(prefix.length());

                if (isMatch(relativePath, query)) {
                    response.add(mapToResponse(objectName, item));
                }
            }
            return response;
        } catch (Exception exception) {
            throw new ResourceOperationException("Search failed: " + exception.getMessage());
        }
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

    private void deleteFile(String objectName) throws Exception {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());

            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                throw new ResourceNotFoundException("File not found: " + objectName);
            }
            throw new ResourceOperationException("Failed to delete file: " + e.getMessage());
        } catch (Exception e) {
            throw new ResourceOperationException("Failed to delete file: " + e.getMessage());
        }
    }

    private void deleteDirectory(String directoryPath) throws Exception {
        List<String> objectsToDelete = new ArrayList<>();
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(directoryPath)
                        .recursive(true)
                        .build());

        for (Result<Item> result : results) {
            Item item = result.get();
            objectsToDelete.add(item.objectName());
        }

        if (objectsToDelete.isEmpty()) {
            throw new ResourceNotFoundException("Directory not found");
        }

        for (String objectName : objectsToDelete) {
            deleteFile(objectName);
        }
    }

//    private DownloadResult downloadDirectory(String directoryPath) {
//        try {
//            ByteArrayOutputStream zipStream = new ByteArrayOutputStream();
//            ZipOutputStream zipOut = new ZipOutputStream(zipStream);
//
//            getListOfItems(directoryPath).forEach(item -> {
//                try (InputStream is = minioClient.getObject(
//                        GetObjectArgs.builder()
//                                .bucket(bucketName)
//                                .object(item.objectName())
//                                .build())) {
//
//                    String entryName = item.objectName().substring(directoryPath.length());
//                    zipOut.putNextEntry(new ZipEntry(entryName));
//                    IOUtils.copy(is, zipOut);
//                    zipOut.closeEntry();
//                } catch (Exception exception) {
//                    throw new ResourceOperationException("Error processing file: " + exception.getMessage());
//                }
//            });
//
//            zipOut.finish();
//            byte[] zipBytes = zipStream.toByteArray();
//
//            //String zipName = directoryPath.replaceAll("/$", "") + ".zip";
//            String zipName = directoryPath.substring(directoryPath.lastIndexOf('/') + 1);
//            return new DownloadResult(
//                    new ByteArrayResource(zipBytes),
//                    MediaType.parseMediaType("application/zip"),
//                    zipName
//            );
//        } catch (Exception exception) {
//            throw new ResourceOperationException("Directory download failed: " + exception.getMessage());
//        }
//    }

    private DownloadResult downloadFile(String objectName) {
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());

            String filename = objectName.substring(objectName.lastIndexOf("/") + 1);
            return new DownloadResult(
                    new InputStreamResource(stream),
                    MediaType.APPLICATION_OCTET_STREAM,
                    filename
            );
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                throw new ResourceNotFoundException("File not found: " + objectName);
            }
            throw new ResourceOperationException("Failed to delete file: " + e.getMessage());
        } catch (Exception e) {
            throw new ResourceOperationException("Failed to delete file: " + e.getMessage());
        }
    }

    private List<Item> getListOfItems(String prefix) {
        var items = new ArrayList<Item>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(true)
                            .build());

            for (Result<Item> result : results) {
                items.add(result.get());
            }
        } catch (Exception exception) {
            throw new ResourceOperationException("Error listing objects: " + exception.getMessage());
        }
        if (items.isEmpty()) {
            throw new ResourceNotFoundException("Directory is empty or not found");
        }
        return items;
    }

    private boolean isMatch(String path, String query) {
        return path.toLowerCase().contains(query.toLowerCase());
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

    private boolean isSourceResourceExists(String objectName) {
        if (objectName.endsWith("/")) {
            return minioService.isDirectoryExists(objectName);
        } else {
            return minioService.objectExists(objectName);
        }
    }


    private void checkTargetParentExists(String targetObject) {
        String parentDir = PathUtils.getParentPath(targetObject);
        if (!isDirectory(parentDir)) {
            throw new ResourceNotFoundException("Target directory does not exist: " + parentDir);
        }

        if (minioService.objectExists(targetObject)) {
            throw new ResourceOperationException("Target resource already exists");
        }
    }

    private void copyResource(String source, String target) {
        try {
            Iterable<Result<Item>> items = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(source)
                            .recursive(true)
                            .build());

            for (Result<Item> itemResult : items) {
                Item item = itemResult.get();
                String sourceKey = item.objectName();
                String targetKey = target + sourceKey.substring(source.length());
                minioClient.copyObject(
                        CopyObjectArgs.builder()
                                .bucket(bucketName)
                                .object(targetKey)
                                .source(CopySource.builder()
                                        .bucket(bucketName)
                                        .object(sourceKey)
                                        .build())
                                .build());
            }
        } catch (Exception e) {
            throw new ResourceOperationException("Copy failed: " + e.getMessage());
        }
    }

    private void deleteResource(String objectName) {
        try {
            if (objectName.endsWith("/")) {
                deleteDirectory(objectName);
            } else {
                deleteFile(objectName);
            }
        } catch (Exception exception) {
            throw new ResourceOperationException("Delete source failed: " + exception.getMessage());
        }
    }

    private ResourceInfoResponse buildResponse(String objectName) {
        if (isDirectory(objectName)) {
            return ResourceInfoResponse.builder()
                    .path(PathUtils.getParentPath(objectName))
                    .name(PathUtils.getResourceName(objectName))
                    .type("DIRECTORY")
                    .build();
        }
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());

            return ResourceInfoResponse.builder()
                    .path(PathUtils.getParentPath(objectName))
                    .name(PathUtils.getResourceName(objectName))
                    .size(stat.size())
                    .type("FILE")
                    .build();
        } catch (Exception exception) {
            throw new ResourceOperationException("Failed to get resource info: " + exception.getMessage());
        }
    }

    private boolean isDirectory(String objectName) {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(objectName + "/")
                            .maxKeys(1)
                            .build());
            return results.iterator().hasNext();
        } catch (Exception exception) {
            throw new ResourceOperationException("Failed to check directory: " + exception.getMessage());
        }
    }

}
