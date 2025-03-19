package com.qubb.cloud.payload;

import lombok.Builder;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

@Builder
public record DownloadResponse(Resource resource, MediaType mediaType, String filename) {
}
