package com.qubb.cloud.storage;

import com.qubb.cloud.exception.ResourceOperationException;
import com.qubb.cloud.payload.DownloadResponse;
import com.qubb.cloud.util.PathUtils;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class DownloadService {

    private final MinioService minioService;

    public DownloadResponse download(String objectName) {
        if (objectName.endsWith("/")) {
            return downloadDirectory(objectName);
        } else {
            return downloadFile(objectName);
        }
    }

    private DownloadResponse downloadFile(String objectName) {
        try {
            InputStream stream = minioService.getObject(objectName);
            String filename = PathUtils.getResourceName(objectName);
            return new DownloadResponse(
                    new InputStreamResource(stream),
                    MediaType.APPLICATION_OCTET_STREAM,
                    filename
            );
        } catch (Exception e) {
            throw new ResourceOperationException("Failed to download file: " + objectName, e);
        }
    }

    private DownloadResponse downloadDirectory(String directoryPath) {
        try (ByteArrayOutputStream zipStream = new ByteArrayOutputStream();
             ZipOutputStream zipOut = new ZipOutputStream(zipStream)) {

            minioService.recursiveListObjects(directoryPath)
                    .forEach(item -> packageZipEntry(item, zipOut));

            zipOut.finish();
            return new DownloadResponse(
                    new ByteArrayResource(zipStream.toByteArray()),
                    MediaType.parseMediaType("application/zip"),
                    PathUtils.getResourceName(directoryPath) + ".zip"
            );
        } catch (IOException e) {
            throw new ResourceOperationException("Directory packaging failed");
        }
    }

    private void packageZipEntry(Item item, ZipOutputStream zipOut) {
        try (InputStream is = minioService.getObject(item.objectName())) {
            String entryName = PathUtils.getResourceName(item.objectName());
            zipOut.putNextEntry(new ZipEntry(entryName));
            IOUtils.copy(is, zipOut);
            zipOut.closeEntry();
        } catch (Exception e) {
            throw new ResourceOperationException("Failed to package: " + item.objectName());
        }
    }
}

