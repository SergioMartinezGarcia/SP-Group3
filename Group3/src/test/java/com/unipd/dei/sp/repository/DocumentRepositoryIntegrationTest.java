package com.unipd.dei.sp.repository;

import com.unipd.dei.sp.model.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DocumentRepository using Testcontainers.
 * Tests real MongoDB interactions with a containerized database.
 */
@DataMongoTest
@Testcontainers
@ContextConfiguration(classes = com.unipd.dei.sp.Group3.Group3Application.class)
class DocumentRepositoryIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(
        DockerImageName.parse("mongo:7.0")
    );

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private DocumentRepository documentRepository;

    @AfterEach
    void cleanup() {
        documentRepository.deleteAll();
    }

    @BeforeAll
    static void checkContainer() {
        assertTrue(mongoDBContainer.isRunning(), "MongoDB container should be running");
    }

    @Test
    @DisplayName("Save and retrieve document from MongoDB")
    void testSaveAndRetrieveDocument() {
        Document doc = new Document(
            "test-doc-1",
            "https://example.com/article",
            "Test content",
            "Test Source",
            "123456789",
            Map.of("section", "Technology")
        );

        documentRepository.save(doc);

        Optional<Document> retrieved = documentRepository.findById("test-doc-1");
        
        assertTrue(retrieved.isPresent());
        assertEquals("test-doc-1", retrieved.get().id());
        assertEquals("Test content", retrieved.get().content());
        assertEquals("Technology", retrieved.get().metadata().get("section"));
    }

    @Test
    @DisplayName("Save document with topics")
    void testSaveDocumentWithTopics() {
        Document.TopicInfo topicInfo = new Document.TopicInfo(
            0, 
            0.75, 
            List.of("science", "research", "study")
        );

        Document doc = new Document(
            "test-doc-2",
            "https://example.com/article",
            "Science article",
            "Source",
            "123456789",
            Map.of(),
            Map.of(0, topicInfo)
        );

        documentRepository.save(doc);

        Document retrieved = documentRepository.findById("test-doc-2").orElseThrow();
        
        assertNotNull(retrieved.topics());
        assertEquals(1, retrieved.topics().size());
        assertEquals(0.75, retrieved.topics().get(0).weight());
        assertEquals(3, retrieved.topics().get(0).topWords().size());
    }

    @Test
    @DisplayName("Update existing document")
    void testUpdateDocument() {
        Document original = new Document(
            "test-doc-3",
            "https://example.com/original",
            "Original content",
            "Source",
            "123456789",
            Map.of()
        );

        documentRepository.save(original);

        Document updated = new Document(
            "test-doc-3",
            "https://example.com/updated",
            "Updated content",
            "Source",
            "987654321",
            Map.of("updated", "true")
        );

        documentRepository.save(updated);

        Document retrieved = documentRepository.findById("test-doc-3").orElseThrow();
        
        assertEquals("Updated content", retrieved.content());
        assertEquals("https://example.com/updated", retrieved.url());
        assertEquals("true", retrieved.metadata().get("updated"));
    }

    @Test
    @DisplayName("Delete document from MongoDB")
    void testDeleteDocument() {
        Document doc = new Document(
            "test-doc-4",
            "https://example.com/article",
            "Content to delete",
            "Source",
            "123456789",
            Map.of()
        );

        documentRepository.save(doc);
        assertTrue(documentRepository.existsById("test-doc-4"));

        documentRepository.deleteById("test-doc-4");
        
        assertFalse(documentRepository.existsById("test-doc-4"));
    }

    @Test
    @DisplayName("Find all documents")
    void testFindAllDocuments() {
        Document doc1 = new Document("doc-1", "url1", "content1", "source", "123", Map.of());
        Document doc2 = new Document("doc-2", "url2", "content2", "source", "124", Map.of());
        Document doc3 = new Document("doc-3", "url3", "content3", "source", "125", Map.of());

        documentRepository.saveAll(List.of(doc1, doc2, doc3));

        List<Document> allDocs = documentRepository.findAll();
        
        assertEquals(3, allDocs.size());
    }

    @Test
    @DisplayName("Count documents in repository")
    void testCountDocuments() {
        assertEquals(0, documentRepository.count());

        documentRepository.save(new Document("doc-1", "url", "content", "source", "123", Map.of()));
        assertEquals(1, documentRepository.count());

        documentRepository.save(new Document("doc-2", "url", "content", "source", "124", Map.of()));
        assertEquals(2, documentRepository.count());
    }

    @Test
    @DisplayName("Delete all documents")
    void testDeleteAll() {
        documentRepository.save(new Document("doc-1", "url", "content", "source", "123", Map.of()));
        documentRepository.save(new Document("doc-2", "url", "content", "source", "124", Map.of()));

        assertEquals(2, documentRepository.count());

        documentRepository.deleteAll();
        
        assertEquals(0, documentRepository.count());
    }

    @Test
    @DisplayName("getDocumentById should return correct document")
    void testGetDocumentById() {
        Document doc = new Document(
            "specific-id",
            "https://example.com",
            "Specific content",
            "Source",
            "123456789",
            Map.of()
        );

        documentRepository.save(doc);

        Document retrieved = documentRepository.getDocumentById("specific-id");
        
        assertNotNull(retrieved);
        assertEquals("specific-id", retrieved.id());
        assertEquals("Specific content", retrieved.content());
    }

    @Test
    @DisplayName("getDocumentById with non-existent ID should return null")
    void testGetDocumentByIdNotFound() {
        Document retrieved = documentRepository.getDocumentById("non-existent");
        
        assertNull(retrieved);
    }

    @Test
    @DisplayName("Save multiple documents with batch operation")
    void testBatchSave() {
        List<Document> documents = List.of(
            new Document("batch-1", "url1", "content1", "source", "123", Map.of()),
            new Document("batch-2", "url2", "content2", "source", "124", Map.of()),
            new Document("batch-3", "url3", "content3", "source", "125", Map.of()),
            new Document("batch-4", "url4", "content4", "source", "126", Map.of()),
            new Document("batch-5", "url5", "content5", "source", "127", Map.of())
        );

        documentRepository.saveAll(documents);

        assertEquals(5, documentRepository.count());
    }

    @Test
    @DisplayName("Handle documents with complex metadata")
    void testComplexMetadata() {
        Map<String, String> metadata = Map.of(
            "section", "Technology",
            "webTitle", "Breaking News in AI",
            "author", "John Doe",
            "publicationDate", "2025-01-19T10:30:00Z",
            "wordCount", "1500"
        );

        Document doc = new Document(
            "complex-meta",
            "https://example.com",
            "Content",
            "Source",
            "123456789",
            metadata
        );

        documentRepository.save(doc);

        Document retrieved = documentRepository.findById("complex-meta").orElseThrow();
        
        assertEquals(5, retrieved.metadata().size());
        assertEquals("Breaking News in AI", retrieved.metadata().get("webTitle"));
        assertEquals("1500", retrieved.metadata().get("wordCount"));
    }

    @Test
    @DisplayName("Handle documents with multiple topics")
    void testMultipleTopics() {
        Map<Integer, Document.TopicInfo> topics = Map.of(
            0, new Document.TopicInfo(0, 0.5, List.of("word1", "word2")),
            1, new Document.TopicInfo(1, 0.3, List.of("word3", "word4")),
            2, new Document.TopicInfo(2, 0.2, List.of("word5", "word6"))
        );

        Document doc = new Document(
            "multi-topic",
            "https://example.com",
            "Content",
            "Source",
            "123456789",
            Map.of(),
            topics
        );

        documentRepository.save(doc);

        Document retrieved = documentRepository.findById("multi-topic").orElseThrow();
        
        assertEquals(3, retrieved.topics().size());
        assertTrue(retrieved.topics().containsKey(0));
        assertTrue(retrieved.topics().containsKey(1));
        assertTrue(retrieved.topics().containsKey(2));
    }

    @Test
    @DisplayName("Persist and retrieve document with null optional fields")
    void testNullOptionalFields() {
        Document doc = new Document(
            "null-fields",
            null,
            null,
            null,
            null,
            null,
            null
        );

        documentRepository.save(doc);

        Document retrieved = documentRepository.findById("null-fields").orElseThrow();
        
        assertEquals("null-fields", retrieved.id());
        assertNull(retrieved.url());
        assertNull(retrieved.content());
        assertNull(retrieved.metadata());
        assertNull(retrieved.topics());
    }
}