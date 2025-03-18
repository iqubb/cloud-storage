package com.qubb.cloud.minio;

import com.qubb.cloud.exceptions.ResourceOperationException;
import com.qubb.cloud.resource.DownloadResult;
import com.qubb.cloud.utils.PathUtils;
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

    public DownloadResult download(String objectName) {
        if (objectName.endsWith("/")) {
            return downloadDirectory(objectName);
        } else {
            return downloadFile(objectName);
        }
    }

    private DownloadResult downloadFile(String objectName) {
        try {
            InputStream stream = minioService.getObject(objectName);
            String filename = PathUtils.getResourceName(objectName);
            return new DownloadResult(
                    new InputStreamResource(stream),
                    MediaType.APPLICATION_OCTET_STREAM,
                    filename
            );
        } catch (Exception e) {
            throw new ResourceOperationException("Failed to download file: " + objectName, e);
        }
    }

    private DownloadResult downloadDirectory(String directoryPath) {
        try (ByteArrayOutputStream zipStream = new ByteArrayOutputStream();
             ZipOutputStream zipOut = new ZipOutputStream(zipStream)) {

            minioService.recursiveListObjects(directoryPath)
                    .forEach(item -> packageZipEntry(item, zipOut));

            zipOut.finish();
            return new DownloadResult(
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

