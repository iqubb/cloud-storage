package com.qubb.cloud.utils;

public class PathUtils {

    public static String getResourceName(String resourcePath) {
        if (resourcePath.endsWith("/")) {
            // Для папки: "folder1/folder2/" -> "folder2"
            String[] parts = resourcePath.split("/", -1);
            return parts[parts.length - 2]; // Предпоследний элемент
        }
        // Для файла: "folder1/folder2/file.txt" -> "file.txt"
        return resourcePath.substring(resourcePath.lastIndexOf("/") + 1);
    }

    public static String buildUserRootPath(int id) {
        return String.format("user-%d-files/", id);
    }

    public static String buildFullUserPath(int id, String path) {
        String root = buildUserRootPath(id);
        if (path.contains(root)) {
            return path;
        }
        return root + path;
    }

    public static String normalizeDirectoryPath(String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    public static String getParentPath(String resourcePath) {
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

    public static String normalize(String path) {
        if (path == null || path.isEmpty()) return "";
        String normalized = path.replace("\\", "/").trim();
        if (!normalized.isEmpty() && !normalized.endsWith("/")) {
            normalized += "/";
        }
        return normalized;
    }
}
