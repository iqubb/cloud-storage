package com.qubb.cloud.storage;

import com.qubb.cloud.payload.DownloadResponse;
import com.qubb.cloud.payload.ResourceInfoResponse;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.stream.Stream;


@RequiredArgsConstructor
@Service
public class StorageOperationsImpl implements StorageOperations {
    private final MinioService minioService;
    private final DeleteService deleteService;
    private final DownloadService downloadService;
    private final UploadService uploadService;

    @Override
    public void delete(String path) {
        deleteService.delete(path);
    }

    @Override
    public DownloadResponse download(String path) {
        return downloadService.download(path);
    }

    @Override
    public List<ResourceInfoResponse> upload(MultipartFile[] files, String basePath) {
        return uploadService.upload(files, basePath);
    }

    @Override
    public void copyResource(String source, String target) {
        minioService.copyDirectoryContents(source, target);
    }

    @Override
    public Stream<Item> recursiveListObjects(String rootPath) {
        return minioService.recursiveListObjects(rootPath);
    }

    @Override
    public StatObjectResponse statObject(String path) {
        return minioService.statObject(path);
    }
}
