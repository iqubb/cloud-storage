package com.qubb.cloud.file;

import lombok.Builder;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

@Builder
public record DownloadResult(Resource resource, MediaType mediaType, String filename) {
}
