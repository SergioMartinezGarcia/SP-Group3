package com.projectmustafa.model;

import java.time.Instant;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Document(
        String id,
        String url,
        String content,
        String source,
        Instant timestamp,
        Map<String, String> metadata) {
}
