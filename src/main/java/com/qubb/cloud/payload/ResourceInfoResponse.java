package com.qubb.cloud.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResourceInfoResponse(String path, String name, Long size, String type) {
}
