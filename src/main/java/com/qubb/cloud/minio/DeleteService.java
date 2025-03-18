package com.qubb.cloud.minio;


import com.qubb.cloud.exceptions.ResourceNotFoundException;
import com.qubb.cloud.exceptions.ResourceOperationException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeleteService {

    private final MinioService minioService;

    public void delete(String objectName) {
        if (objectName.endsWith("/")) {
            deleteDirectory(objectName);
        } else {
            deleteFile(objectName);
        }
    }

    public void deleteFile(String objectName) {
        try {
            minioService.removeObject(objectName);
        } catch (Exception e) {
            throw new ResourceOperationException("Failed to delete file: " + objectName, e);
        }
    }

    public void deleteDirectory(String directoryPath) {
        List<String> objectsToDelete = minioService
                .recursiveListObjects(directoryPath)
                .map(Item::objectName)
                .toList();
        if (objectsToDelete.isEmpty()) {
            throw new ResourceNotFoundException("Directory not found or empty: " + directoryPath);
        }
        objectsToDelete.forEach(this::deleteFile);
    }
}

