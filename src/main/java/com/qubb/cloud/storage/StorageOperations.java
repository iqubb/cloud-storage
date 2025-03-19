package com.qubb.cloud.storage;

import com.qubb.cloud.payload.DownloadResponse;
import com.qubb.cloud.payload.ResourceInfoResponse;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Stream;

public interface StorageOperations {
    void delete(String path);
    DownloadResponse download(String path);
    List<ResourceInfoResponse> upload(MultipartFile[] files, String basePath);
    void copyResource(String source, String target);
    Stream<Item> recursiveListObjects(String rootPath);
    StatObjectResponse statObject(String path);

}
