package com.qubb.cloud.utils;

import com.qubb.cloud.resource.ResourceInfoResponse;
import io.minio.messages.Item;

public class ResourceResponseBuilder {
    private static final String DIRECTORY_TYPE = "DIRECTORY";
    private static final String FILE_TYPE = "FILE";

    public static ResourceInfoResponse buildFromItem(Item item) {
        String objectName = item.objectName();
        boolean isDirectory = objectName.endsWith("/") || item.isDir();
        String name = PathUtils.getResourceName(objectName);

        return ResourceInfoResponse.builder()
                .path(PathUtils.getParentPath(objectName))
                .name(isDirectory ? name + "/" : name)
                .size(isDirectory ? null : item.size())
                .type(isDirectory ? DIRECTORY_TYPE : FILE_TYPE)
                .build();
    }

    public static ResourceInfoResponse buildFromObjectName(String objectName, Long size) {
        boolean isDirectory = objectName.endsWith("/");
        String name = PathUtils.getResourceName(objectName);

        return ResourceInfoResponse.builder()
                .path(PathUtils.getParentPath(objectName))
                .name(isDirectory ? name + "/" : name)
                .size(isDirectory ? null : size)
                .type(isDirectory ? DIRECTORY_TYPE : FILE_TYPE)
                .build();
    }
}
