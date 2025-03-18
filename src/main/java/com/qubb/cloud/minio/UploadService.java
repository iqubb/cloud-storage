package com.qubb.cloud.minio;

import com.qubb.cloud.exceptions.ResourceOperationException;
import com.qubb.cloud.resource.ResourceInfoResponse;
import com.qubb.cloud.utils.PathUtils;
import com.qubb.cloud.utils.ResourceResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UploadService {

    private final MinioService minioService;

    public List<ResourceInfoResponse> upload(MultipartFile[] files, String path)  {
        return Arrays.stream(files)
                .flatMap(file -> processFile(file, path).stream())
                .collect(Collectors.toList());
    }

    private List<ResourceInfoResponse> processFile(MultipartFile file, String basePath) {
        try {
            String relativePath = file.getOriginalFilename();
            String objectName = buildObjectName(basePath, relativePath);
            createParentDirectories(objectName);

            if (minioService.objectExists(objectName)) {
                throw new ResourceOperationException("File already exists: " + objectName);
            }

            minioService.putObject(objectName, file.getInputStream(), file.getSize(), file.getContentType());

            return List.of(ResourceResponseBuilder.buildFromParams(
                    PathUtils.getParentPath(objectName),
                    PathUtils.getResourceName(objectName),
                    false,
                    file.getSize()
            ));
        } catch (Exception e) {
            throw new ResourceOperationException("Failed to upload file: " + e.getMessage());
        }
    }

    private void createParentDirectories(String objectName) {
        String parentDir = PathUtils.getParentPath(objectName);
        if (!parentDir.isEmpty() && !minioService.isDirectoryExists(parentDir)) {
            minioService.createDirectoryObject(parentDir);
        }
    }

    private String buildObjectName(String basePath, String relativePath) {
        return PathUtils.normalize(basePath) + relativePath;
    }
}
