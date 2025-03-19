package com.qubb.cloud.storage;

import com.qubb.cloud.resource.DownloadResult;
import com.qubb.cloud.resource.ResourceInfoResponse;
import io.minio.messages.Item;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Stream;

public interface StorageOperations {
    void delete(String path);
    DownloadResult download(String path);
    List<ResourceInfoResponse> upload(MultipartFile[] files, String basePath);
    void copyResource(String source, String target);
    Stream<Item> recursiveListObjects(String rootPath);
    
}
