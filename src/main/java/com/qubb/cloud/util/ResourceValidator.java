package com.qubb.cloud.util;

import com.qubb.cloud.exception.ResourceNotFoundException;
import com.qubb.cloud.exception.ResourceOperationException;
import com.qubb.cloud.storage.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourceValidator {

    private final MinioService minioService;

    public void checkTargetParentExists(String targetObject) {
        String parentDir = PathUtils.getParentPath(targetObject);
        if (!minioService.isDirectory(parentDir)) {
            throw new ResourceNotFoundException("Target directory does not exist: " + parentDir);
        }
        if (minioService.objectExists(targetObject)) {
            throw new ResourceOperationException("Target resource already exists");
        }
    }

    public boolean isSourceResourceExists(String objectName) {
        if (objectName.endsWith("/")) {
            return minioService.isDirectoryExists(objectName);
        } else {
            return minioService.objectExists(objectName);
        }
    }
}
