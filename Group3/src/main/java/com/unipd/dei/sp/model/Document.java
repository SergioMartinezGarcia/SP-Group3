package com.unipd.dei.sp.model;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
 * Represents a document from The Guardian API.
 * Documents can be enriched with topic information after processing.
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

        /*
         * Topic information for this document.
         * Maps topic IDs to their weights and representative words.
         * Populated after topic model training.
         */
        Map<Integer, TopicInfo> topics) {

    // Contains topic weight and top words for a single topic
    public record TopicInfo(
        Integer id,
        Double weight,
        java.util.List<String> topWords
    ) {}

    // Constructor for creating documents without topic information
    public Document(String id, String url, String content, String source, 
                   String timestamp, Map<String, String> metadata) {
        this(id, url, content, source, timestamp, metadata, null);
    }
}