package com.unipd.dei.sp.model;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a document retrieved from the Guardian API.
 * Documents are enriched with topic information after topic model training.
 */
@org.springframework.data.elasticsearch.annotations.Document(indexName = "documents")
@org.springframework.data.mongodb.core.mapping.Document(collection = "documents")
@JsonIgnoreProperties(ignoreUnknown = true)
public record Document(
        @Id
        String id,

        String url,

        String content,

        String source,

        @Field(type = FieldType.Long)
        String timestamp,

        Map<String, String> metadata,

        /**
         * Topic information for this document
         * Map of topic ID to TopicInfo containing weight and top words
         * This field is populated after topic model training
         */
        Map<Integer, TopicInfo> topics) {

    /**
     * Represents topic information for a document
     */
    public record TopicInfo(
        Integer id,
        Double weight,
        java.util.List<String> topWords
    ) {}

    /**
     * Constructor without topics (for initial document creation)
     */
    public Document(String id, String url, String content, String source, 
                   String timestamp, Map<String, String> metadata) {
        this(id, url, content, source, timestamp, metadata, null);
    }
}