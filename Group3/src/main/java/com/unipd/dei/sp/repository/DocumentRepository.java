package com.unipd.dei.sp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.unipd.dei.sp.model.Document;

/**
 * @brief Repository interface for accessing Document entities.
 *
 * This interface provides CRUD operations for {@link Document} objects
 * stored in a MongoDB database. It extends {@link MongoRepository} to
 * leverage Spring Data's repository features, including basic
 * operations like save, findAll, delete, and custom query methods.
 */
public interface DocumentRepository extends MongoRepository<Document, String> {

    /**
     * @brief Retrieves a document by its unique identifier.
     *
     * @param id The unique identifier of the document.
     * @return The {@link Document} with the specified id, or null if none is found.
     *
     * @details
     * This method allows fetching a single document from the database
     * using its unique id. Spring Data automatically generates the
     * query based on the method name.
     */
    public Document getDocumentById(String id);
}

