package com.qubb.cloud.storage;

import com.qubb.cloud.minio.DeleteService;
import com.qubb.cloud.minio.DownloadService;
import com.qubb.cloud.minio.MinioService;
import com.qubb.cloud.minio.UploadService;
import com.qubb.cloud.resource.DownloadResult;
import com.qubb.cloud.resource.ResourceInfoResponse;
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
    public DownloadResult download(String path) {
        return downloadService.download(path);
    }

    @Override
    public List<ResourceInfoResponse> upload(MultipartFile[] files, String basePath) {
        return uploadService.upload(files, basePath);
    }

    @Override
    public void copyResource(String source, String target) {
        minioService.copyResource(source, target);
    }

    @Override
    public Stream<Item> recursiveListObjects(String rootPath) {
        return minioService.recursiveListObjects(rootPath);
    }
}
