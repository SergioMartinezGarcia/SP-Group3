package com.unipd.dei.sp.model;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.Id;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @brief Represents a document retrieved from the Guardian API.
 *
 * This class models a news document with its associated metadata,
 * content, and source information. It is designed to work with
 * Spring Data and JSON deserialization.
 * 
 * @details
 * Instances of this class are immutable records. It includes
 * fields for the document's unique identifier, URL, content,
 * source, timestamp, and any additional metadata provided by
 * the API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Document(
        /**
         * @brief Unique identifier of the document.
         * 
         * This field is annotated with @Id to indicate the primary key
         * for persistence frameworks like Spring Data.
         */
        @Id
        String id,

        /**
         * @brief URL of the document.
         * 
         * This is the link to the original content on the Guardian website.
         */
        String url,

        /**
         * @brief Full text content of the document.
         */
        String content,

        /**
         * @brief Source of the document.
         * 
         * Typically indicates the publisher or API source.
         */
        String source,

        /**
         * @brief Timestamp indicating when the document was retrieved or published.
         */
        Instant timestamp,

        /**
         * @brief Additional metadata for the document.
         * 
         * A map of key-value pairs containing supplementary information
         * about the document, such as section, author, tags, etc.
         */
        Map<String, String> metadata) {
}
