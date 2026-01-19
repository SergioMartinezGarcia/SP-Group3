package com.unipd.dei.sp.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Document model class.
 * Tests record creation, accessors, and topic information handling.
 */
class DocumentTest {

    @Test
    @DisplayName("Create document without topics")
    void testCreateDocumentWithoutTopics() {
        Map<String, String> metadata = Map.of(
            "section", "Technology",
            "webTitle", "Test Article"
        );

        Document doc = new Document(
            "doc-123",
            "https://example.com/article",
            "Article content here",
            "The Guardian",
            "1234567890",
            metadata
        );

        assertEquals("doc-123", doc.id());
        assertEquals("https://example.com/article", doc.url());
        assertEquals("Article content here", doc.content());
        assertEquals("The Guardian", doc.source());
        assertEquals("1234567890", doc.timestamp());
        assertEquals(metadata, doc.metadata());
        assertNull(doc.topics());
    }

    @Test
    @DisplayName("Create document with topics")
    void testCreateDocumentWithTopics() {
        Map<Integer, Document.TopicInfo> topics = Map.of(
            0, new Document.TopicInfo(0, 0.45, List.of("science", "research", "study")),
            1, new Document.TopicInfo(1, 0.25, List.of("technology", "computer", "data"))
        );

        Document doc = new Document(
            "doc-123",
            "https://example.com/article",
            "Article content",
            "The Guardian",
            "1234567890",
            Map.of(),
            topics
        );

        assertNotNull(doc.topics());
        assertEquals(2, doc.topics().size());
        assertTrue(doc.topics().containsKey(0));
        assertTrue(doc.topics().containsKey(1));
    }

    @Test
    @DisplayName("TopicInfo record should store values correctly")
    void testTopicInfo() {
        List<String> topWords = List.of("science", "research", "study", "data");
        Document.TopicInfo topicInfo = new Document.TopicInfo(5, 0.75, topWords);

        assertEquals(5, topicInfo.id());
        assertEquals(0.75, topicInfo.weight());
        assertEquals(topWords, topicInfo.topWords());
        assertEquals(4, topicInfo.topWords().size());
    }

    @Test
    @DisplayName("Document records should be equal when all fields match")
    void testDocumentEquality() {
        Map<String, String> metadata = Map.of("key", "value");
        
        Document doc1 = new Document(
            "doc-1",
            "https://example.com",
            "content",
            "source",
            "123",
            metadata
        );

        Document doc2 = new Document(
            "doc-1",
            "https://example.com",
            "content",
            "source",
            "123",
            metadata
        );

        assertEquals(doc1, doc2);
        assertEquals(doc1.hashCode(), doc2.hashCode());
    }

    @Test
    @DisplayName("Document records should not be equal when fields differ")
    void testDocumentInequality() {
        Document doc1 = new Document(
            "doc-1",
            "https://example.com",
            "content",
            "source",
            "123",
            Map.of()
        );

        Document doc2 = new Document(
            "doc-2",  // Different ID
            "https://example.com",
            "content",
            "source",
            "123",
            Map.of()
        );

        assertNotEquals(doc1, doc2);
    }

    @Test
    @DisplayName("Handle null metadata gracefully")
    void testNullMetadata() {
        Document doc = new Document(
            "doc-1",
            "https://example.com",
            "content",
            "source",
            "123",
            null
        );

        assertNull(doc.metadata());
        assertDoesNotThrow(() -> doc.metadata());
    }

    @Test
    @DisplayName("Handle null content gracefully")
    void testNullContent() {
        Document doc = new Document(
            "doc-1",
            "https://example.com",
            null,
            "source",
            "123",
            Map.of()
        );

        assertNull(doc.content());
    }

    @Test
    @DisplayName("Empty topics map should be handled")
    void testEmptyTopics() {
        Map<Integer, Document.TopicInfo> emptyTopics = new HashMap<>();
        
        Document doc = new Document(
            "doc-1",
            "https://example.com",
            "content",
            "source",
            "123",
            Map.of(),
            emptyTopics
        );

        assertNotNull(doc.topics());
        assertTrue(doc.topics().isEmpty());
    }

    @Test
    @DisplayName("Multiple topics with varying weights")
    void testMultipleTopicsWithWeights() {
        Map<Integer, Document.TopicInfo> topics = Map.of(
            0, new Document.TopicInfo(0, 0.50, List.of("word1", "word2")),
            1, new Document.TopicInfo(1, 0.30, List.of("word3", "word4")),
            2, new Document.TopicInfo(2, 0.15, List.of("word5", "word6")),
            3, new Document.TopicInfo(3, 0.05, List.of("word7", "word8"))
        );

        Document doc = new Document(
            "doc-1",
            "https://example.com",
            "content",
            "source",
            "123",
            Map.of(),
            topics
        );

        assertEquals(4, doc.topics().size());
        assertEquals(0.50, doc.topics().get(0).weight());
        assertEquals(0.30, doc.topics().get(1).weight());
        assertEquals(0.15, doc.topics().get(2).weight());
        assertEquals(0.05, doc.topics().get(3).weight());
    }

    @Test
    @DisplayName("TopicInfo with empty top words list")
    void testTopicInfoWithEmptyWords() {
        Document.TopicInfo topicInfo = new Document.TopicInfo(1, 0.25, List.of());

        assertEquals(1, topicInfo.id());
        assertEquals(0.25, topicInfo.weight());
        assertTrue(topicInfo.topWords().isEmpty());
    }

    @Test
    @DisplayName("Document with all null optional fields")
    void testDocumentWithAllNullOptionalFields() {
        Document doc = new Document(
            "doc-1",
            null,
            null,
            null,
            null,
            null,
            null
        );

        assertEquals("doc-1", doc.id());
        assertNull(doc.url());
        assertNull(doc.content());
        assertNull(doc.source());
        assertNull(doc.timestamp());
        assertNull(doc.metadata());
        assertNull(doc.topics());
    }

    @Test
    @DisplayName("Metadata with various data types as strings")
    void testMetadataWithVariousTypes() {
        Map<String, String> metadata = Map.of(
            "section", "Technology",
            "webTitle", "Breaking News",
            "publicationDate", "2025-01-19",
            "wordCount", "1500",
            "premium", "false"
        );

        Document doc = new Document(
            "doc-1",
            "https://example.com",
            "content",
            "source",
            "123",
            metadata
        );

        assertEquals("Technology", doc.metadata().get("section"));
        assertEquals("Breaking News", doc.metadata().get("webTitle"));
        assertEquals("2025-01-19", doc.metadata().get("publicationDate"));
        assertEquals("1500", doc.metadata().get("wordCount"));
        assertEquals("false", doc.metadata().get("premium"));
    }
}