package com.unipd.dei.sp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.unipd.dei.sp.model.Document;

/*
 * Repository for accessing Document entities in MongoDB.
 * Provides standard database operations and custom query methods.
 */
public interface DocumentRepository extends MongoRepository<Document, String> {

    // Retrieves a document by its unique identifier
    public Document getDocumentById(String id);
}