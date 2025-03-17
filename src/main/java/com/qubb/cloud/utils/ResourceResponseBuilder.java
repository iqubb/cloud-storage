package com.qubb.cloud.utils;

import com.qubb.cloud.resource.ResourceInfoResponse;
import io.minio.messages.Item;

public class ResourceResponseBuilder {
    private static final String DIRECTORY_TYPE = "DIRECTORY";
    private static final String FILE_TYPE = "FILE";

    public static ResourceInfoResponse buildFromItem(Item item, String basePath) {
        String objectName = item.objectName();
        boolean isDirectory = isDirectory(item);

        String entryName = extractEntryName(objectName, basePath);

        return isDirectory
                ? buildDirectory(objectName, basePath, entryName)
                : buildFile(objectName, basePath, entryName, item.size());
    }

    public static ResourceInfoResponse buildFromParams(String prefix,
                                                       String entryName,
                                                       boolean isDirectory,
                                                       long size) {
        String fullPath = buildFullPath(prefix, entryName, isDirectory);
        return isDirectory
                ? buildDirectory(fullPath, prefix, entryName)
                : buildFile(fullPath, prefix, entryName, size);
    }

    private static ResourceInfoResponse buildFile(String objectName,
                                                  String basePath,
                                                  String entryName,
                                                  long size) {
        return ResourceInfoResponse.builder()
                .path(normalizeBasePath(basePath))
                .name(entryName)
                .size(size)
                .type(FILE_TYPE)
                .build();
    }

    private static ResourceInfoResponse buildDirectory(String objectName,
                                                       String basePath,
                                                       String entryName) {
        return ResourceInfoResponse.builder()
                .path(normalizeBasePath(basePath))
                .name(entryName + "/")
                .type(DIRECTORY_TYPE)
                .build();
    }

    private static boolean isDirectory(Item item) {
        return item.objectName().endsWith("/") || item.isDir();
    }

    private static String normalizeBasePath(String path) {
        return PathUtils.normalize(path);
    }

    private static String extractEntryName(String objectName, String basePath) {
        return PathUtils.extractEntryName(objectName, basePath);
    }

    private static String buildFullPath(String prefix, String entryName, boolean isDirectory) {
        return PathUtils.normalize(prefix) + entryName + (isDirectory ? "/" : "");
    }
}
