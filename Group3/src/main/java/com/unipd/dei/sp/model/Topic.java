package com.unipd.dei.sp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Represents a topic extracted by Mallet topic modeling
 * Stored in MongoDB
 */
@Document(collection = "topics")
public record Topic(
    @Id
    Integer id,
    List<String> topWords,
    Double coherence
) {}