package com.qubb.cloud.resource;

import com.qubb.cloud.exception.IncorrectPathException;
import com.qubb.cloud.exception.ResourceNotFoundException;
import com.qubb.cloud.exception.ResourceOperationException;
import com.qubb.cloud.exception.UserNotFoundException;
import com.qubb.cloud.user.UserDetailsImpl;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    public ResourceInfoResponse getResourceInfo(String path, UserDetailsImpl userDetails) {
        validateResourceRequest(path, userDetails);
        String objectName = "user-" + userDetails.user().getId() + "-files/" + path;
        checkSourceExists(objectName);
        return buildResponse(objectName);
    }

    public void deleteResource(String path, UserDetailsImpl userDetails) {
        validateResourceRequest(path, userDetails);
        try {
            if (path.endsWith("/")) {
                deleteDirectory(path);
            } else {
                deleteFile(path);
            }
        } catch (Exception exception) {
            throw new ResourceNotFoundException("Failed to delete resource: " + exception.getMessage());
        }
    }

    public DownloadResult downloadResource(String path, UserDetailsImpl userDetails) {
        validateResourceRequest(path, userDetails);
        if (path.endsWith("/")) {
            return downloadDirectory(path);
        } else {
            return downloadFile(path);
        }
    }

    public ResourceInfoResponse moveResource(String from, String to, UserDetailsImpl userDetails) {
        validateResourceRequest(from, userDetails);
        validateResourceRequest(to, userDetails);
        //String basePrefix = "user-" + userDetails.user().getId() + "-files/";
        String sourceObject = from;
        String targetObject = to;

        checkSourceExists(sourceObject);
        checkTargetParentExists(targetObject);

        copyResource(sourceObject, targetObject);
        deleteResource(sourceObject);

        return buildResponse(targetObject);
    }

    public List<ResourceInfoResponse> search(String query, UserDetailsImpl userDetails) {
        validateResourceRequest(query, userDetails);

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

                if(isMatch(relativePath, query)) {
                    response.add(mapToResponse(objectName, item));
                }
            }
            return response;
        } catch (Exception exception) {
            throw new ResourceOperationException("Search failed: " + exception.getMessage());
        }
    }

    public List<ResourceInfoResponse> uploadResources(String targetPath, MultipartFile[] files, UserDetailsImpl userDetails) {
        validateResourceRequest(targetPath, userDetails);
        String prefix = "user-" + userDetails.user().getId() + "-files/";
        String fullPath = prefix + targetPath;

        isDirectoryExists(fullPath);

        return Arrays.stream(files)
                .flatMap(file -> processFile(file, fullPath).stream())
                .collect(Collectors.toList());
    }

    private List<ResourceInfoResponse> processFile(MultipartFile file, String basePath) {
        try {
            String fileName = file.getOriginalFilename();
            String objectName = basePath + fileName;
            if (isObjectExists(objectName)) {
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
                    .path(getParentPath(objectName))
                    .name(getResourceName(objectName))
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

    private DownloadResult downloadDirectory(String directoryPath) {
        try {
            ByteArrayOutputStream zipStream = new ByteArrayOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(zipStream);

            getListOfItems(directoryPath).forEach(item -> {
                try (InputStream is = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(item.objectName())
                                .build())) {

                    String entryName = item.objectName().substring(directoryPath.length());
                    zipOut.putNextEntry(new ZipEntry(entryName));
                    IOUtils.copy(is, zipOut);
                    zipOut.closeEntry();
                } catch (Exception exception) {
                    throw new ResourceOperationException("Error processing file: " + exception.getMessage());
                }
            });

            zipOut.finish();
            byte[] zipBytes = zipStream.toByteArray();

            //String zipName = directoryPath.replaceAll("/$", "") + ".zip";
            String zipName = directoryPath.substring(directoryPath.lastIndexOf('/') + 1);
            return new DownloadResult(
                    new ByteArrayResource(zipBytes),
                    MediaType.parseMediaType("application/zip"),
                    zipName
            );
        } catch (Exception exception) {
            throw new ResourceOperationException("Directory download failed: " + exception.getMessage());
        }
    }

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
        }  catch (ErrorResponseException e) {
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

    private void validateResourceRequest(String path, UserDetailsImpl userDetails) {

        var user = userDetails.user();
        if (user == null) {
            throw new UserNotFoundException("User not found");
        }
        if (path == null || path.isEmpty()) {
            return;
        }
        String allowedCharacters = "^[a-zA-Z0-9\\-._!*'()/ @$=:+;,]+$";

        if (!path.matches(allowedCharacters)) {
            throw new IncorrectPathException("Path contains invalid characters: " + path);
        }

        if (path.contains("..") || path.contains("//")) {
            throw new IncorrectPathException("Invalid path structure: " + path);
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

    private boolean isMatch(String path, String query) {
        return path.toLowerCase().contains(query.toLowerCase());
    }

    private ResourceInfoResponse mapToResponse(String objectName, Item item) {
        boolean isDirectory = objectName.endsWith("/");
        String parentPath = getParentPath(objectName);
        String name = getResourceName(objectName);

        return ResourceInfoResponse.builder()
                .path(parentPath)
                .name(name)
                .size(isDirectory ? null : item.size())
                .type(isDirectory ? "DIRECTORY" : "FILE")
                .build();
    }

    private void checkSourceExists(String objectName) {
        try {
            if (objectName.endsWith("/")) {
                isDirectoryExists(objectName);
            } else {
                minioClient.statObject(StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build());
            }
        } catch (Exception e) {
            throw new ResourceNotFoundException("Source resource not found");
        }
    }


    private void checkTargetParentExists(String targetObject) {
        String parentDir = getParentPath(targetObject);
        if (!isDirectory(parentDir)) {
            throw new ResourceNotFoundException("Target directory does not exist: " + parentDir);
        }

        if (isObjectExists(targetObject)) {
            throw new ResourceOperationException("Target resource already exists");
        }
    }

    private boolean isObjectExists(String objectName) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            return true;
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            return false;
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
                    .path(getParentPath(objectName))
                    .name(getResourceName(objectName))
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
                    .path(getParentPath(objectName))
                    .name(getResourceName(objectName))
                    .size(stat.size())
                    .type("FILE")
                    .build();
        } catch (Exception exception) {
            throw new ResourceOperationException("Failed to get resource info: " + exception.getMessage());
        }
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
            } catch (Exception exception) {
                Iterable<Result<Item>> results = minioClient.listObjects(
                        ListObjectsArgs.builder()
                                .bucket(bucketName)
                                .prefix(directoryPath.endsWith("/") ? directoryPath : directoryPath + "/")
                                .maxKeys(1)
                                .build());
                return results.iterator().hasNext();
            }
        } catch (Exception exception) {
            log.error("Error checking directory existence: {}", directoryPath, exception);
            return false;
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
