package com.qubb.cloud.utils;

import com.qubb.cloud.resource.ResourceInfoResponse;
import io.minio.StatObjectResponse;
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

    public static ResourceInfoResponse buildFromObjectName(String objectName, StatObjectResponse stat) {
        boolean isDirectory = objectName.endsWith("/");
        String name = PathUtils.getResourceName(objectName);

        return ResourceInfoResponse.builder()
                .path(PathUtils.getParentPath(objectName))
                .name(isDirectory ? name + "/" : name)
                .size(isDirectory ? null : stat.size())
                .type(isDirectory ? DIRECTORY_TYPE : FILE_TYPE)
                .build();
    }
}
