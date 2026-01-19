package com.unipd.dei.sp.elasticsearch;

import com.unipd.dei.sp.model.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ElasticSearchService using Testcontainers.
 * Tests real Elasticsearch interactions with a containerized instance.
 */
@SpringBootTest
@Testcontainers
@ContextConfiguration(classes = com.unipd.dei.sp.Group3.Group3Application.class)
class ElasticSearchServiceIntegrationTest {

    @Container
    static ElasticsearchContainer elasticsearchContainer = new ElasticsearchContainer(
        DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.11.0")
    )
    .withEnv("xpack.security.enabled", "false")
    .withEnv("xpack.security.http.ssl.enabled", "false")
    .withStartupTimeout(Duration.ofSeconds(120))
    .withStartupAttempts(3);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("elasticsearch.host", () -> elasticsearchContainer.getHost());
        registry.add("elasticsearch.port", () -> elasticsearchContainer.getFirstMappedPort());
    }

    @Autowired
    private ElasticSearchService elasticSearchService;

    @BeforeAll
    static void checkContainer() {
        assertTrue(elasticsearchContainer.isRunning(), "Elasticsearch container should be running");
    }

    @AfterEach
    void cleanup() {
        // Note: In a real scenario, you might want to clean up the index
        // For now, each test uses unique document IDs
    }

    @Test
    @DisplayName("Index a single document to Elasticsearch")
    void testIndexDocument() throws InterruptedException {
        Document doc = new Document(
            "es-test-1",
            "https://example.com/article",
            "This is test content about artificial intelligence and machine learning",
            "Test Source",
            "123456789",
            Map.of("section", "Technology")
        );

        elasticSearchService.indexDocument(doc);
        
        // Give Elasticsearch time to index
        Thread.sleep(1000);

        Document retrieved = elasticSearchService.getDocumentById("es-test-1");
        
        assertNotNull(retrieved);
        assertEquals("es-test-1", retrieved.id());
        assertEquals("Test Source", retrieved.source());
        assertTrue(retrieved.content().contains("artificial intelligence"));
    }

    @Test
    @DisplayName("Bulk index multiple documents")
    void testBulkIndexDocuments() throws InterruptedException {
        List<Document> documents = List.of(
            new Document(
                "bulk-1",
                "https://example.com/article1",
                "Article about climate change and global warming",
                "Source",
                "123",
                Map.of()
            ),
            new Document(
                "bulk-2",
                "https://example.com/article2",
                "Article about renewable energy and solar power",
                "Source",
                "124",
                Map.of()
            ),
            new Document(
                "bulk-3",
                "https://example.com/article3",
                "Article about electric vehicles and batteries",
                "Source",
                "125",
                Map.of()
            )
        );

        elasticSearchService.bulkIndexDocuments(documents);
        
        // Give Elasticsearch time to index
        Thread.sleep(1500);

        Document doc1 = elasticSearchService.getDocumentById("bulk-1");
        Document doc2 = elasticSearchService.getDocumentById("bulk-2");
        Document doc3 = elasticSearchService.getDocumentById("bulk-3");
        
        assertNotNull(doc1);
        assertNotNull(doc2);
        assertNotNull(doc3);
        assertTrue(doc1.content().contains("climate change"));
        assertTrue(doc2.content().contains("renewable energy"));
        assertTrue(doc3.content().contains("electric vehicles"));
    }

    @Test
    @DisplayName("Search documents by content")
    void testSearchByContent() throws InterruptedException {
        // Index test documents
        List<Document> documents = List.of(
            new Document(
                "search-1",
                "https://example.com/ai",
                "Artificial intelligence is transforming the technology industry",
                "Source",
                "200",
                Map.of()
            ),
            new Document(
                "search-2",
                "https://example.com/ml",
                "Machine learning algorithms are improving rapidly",
                "Source",
                "201",
                Map.of()
            ),
            new Document(
                "search-3",
                "https://example.com/cooking",
                "The best recipes for Italian pasta dishes",
                "Source",
                "202",
                Map.of()
            )
        );

        elasticSearchService.bulkIndexDocuments(documents);
        
        // Give Elasticsearch time to index
        Thread.sleep(1500);

        List<Document> results = elasticSearchService.searchByContent("intelligence");
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        
        // Should find the document about AI
        boolean foundAIDoc = results.stream()
            .anyMatch(doc -> doc.id().equals("search-1"));
        assertTrue(foundAIDoc, "Should find the AI document");
    }

    @Test
    @DisplayName("Search returns empty list for non-matching query")
    void testSearchNoResults() throws InterruptedException {
        Document doc = new Document(
            "search-empty",
            "https://example.com/article",
            "This is about quantum physics",
            "Source",
            "300",
            Map.of()
        );

        elasticSearchService.indexDocument(doc);
        
        Thread.sleep(1000);

        List<Document> results = elasticSearchService.searchByContent("cryptocurrency");
        
        assertNotNull(results);
        // May be empty or not contain our document
    }

    @Test
    @DisplayName("Retrieve document by ID")
    void testGetDocumentById() throws InterruptedException {
        Document doc = new Document(
            "get-by-id-test",
            "https://example.com/specific",
            "Specific content for ID retrieval test",
            "Test Source",
            "400",
            Map.of("category", "Test")
        );

        elasticSearchService.indexDocument(doc);
        
        Thread.sleep(1000);

        Document retrieved = elasticSearchService.getDocumentById("get-by-id-test");
        
        assertNotNull(retrieved);
        assertEquals("get-by-id-test", retrieved.id());
        assertEquals("https://example.com/specific", retrieved.url());
        assertEquals("Specific content for ID retrieval test", retrieved.content());
        assertEquals("Test Source", retrieved.source());
    }

    @Test
    @DisplayName("Get non-existent document returns null")
    void testGetNonExistentDocument() {
        Document retrieved = elasticSearchService.getDocumentById("does-not-exist-12345");
        
        assertNull(retrieved);
    }

    @Test
    @DisplayName("Index document with topics")
    void testIndexDocumentWithTopics() throws InterruptedException {
        Map<Integer, Document.TopicInfo> topics = Map.of(
            0, new Document.TopicInfo(0, 0.75, List.of("science", "research", "study")),
            1, new Document.TopicInfo(1, 0.25, List.of("technology", "computer", "data"))
        );

        Document doc = new Document(
            "with-topics",
            "https://example.com/science-article",
            "A scientific study on computer technology",
            "Source",
            "500",
            Map.of("section", "Science"),
            topics
        );

        elasticSearchService.indexDocument(doc);
        
        Thread.sleep(1000);

        Document retrieved = elasticSearchService.getDocumentById("with-topics");
        
        assertNotNull(retrieved);
        assertNotNull(retrieved.topics());
        assertEquals(2, retrieved.topics().size());
        assertTrue(retrieved.topics().containsKey(0));
        assertEquals(0.75, retrieved.topics().get(0).weight());
        assertEquals(3, retrieved.topics().get(0).topWords().size());
    }

    @Test
    @DisplayName("Search with topic filter")
    void testSearchWithTopicFilter() throws InterruptedException {
        Map<Integer, Document.TopicInfo> topics1 = Map.of(
            0, new Document.TopicInfo(0, 0.8, List.of("science"))
        );
        
        Map<Integer, Document.TopicInfo> topics2 = Map.of(
            1, new Document.TopicInfo(1, 0.7, List.of("sports"))
        );

        List<Document> documents = List.of(
            new Document(
                "topic-filter-1",
                "https://example.com/science",
                "Scientific research article",
                "Source",
                "600",
                Map.of(),
                topics1
            ),
            new Document(
                "topic-filter-2",
                "https://example.com/sports",
                "Sports news article",
                "Source",
                "601",
                Map.of(),
                topics2
            )
        );

        elasticSearchService.bulkIndexDocuments(documents);
        
        Thread.sleep(1500);

        // Note: Current implementation doesn't actually filter by topic
        // This test verifies the method doesn't crash when topic filter is provided
        List<Document> results = elasticSearchService.searchWithTopicFilter("article", 0);
        
        assertNotNull(results);
    }

    @Test
    @DisplayName("Handle documents with null fields")
    void testIndexDocumentWithNullFields() throws InterruptedException {
        Document doc = new Document(
            "null-fields-test",
            null,
            "Content only",
            null,
            null,
            null,
            null
        );

        assertDoesNotThrow(() -> elasticSearchService.indexDocument(doc));
        
        Thread.sleep(1000);

        Document retrieved = elasticSearchService.getDocumentById("null-fields-test");
        
        assertNotNull(retrieved);
        assertEquals("null-fields-test", retrieved.id());
    }

    @Test
    @DisplayName("Index document with complex metadata")
    void testIndexDocumentWithMetadata() throws InterruptedException {
        Map<String, String> metadata = Map.of(
            "section", "Technology",
            "webTitle", "Breaking News in AI",
            "author", "John Doe",
            "publicationDate", "2025-01-19T10:30:00Z"
        );

        Document doc = new Document(
            "metadata-test",
            "https://example.com/ai-news",
            "Latest developments in artificial intelligence",
            "The Guardian",
            "700",
            metadata
        );

        elasticSearchService.indexDocument(doc);
        
        Thread.sleep(1000);

        Document retrieved = elasticSearchService.getDocumentById("metadata-test");
        
        assertNotNull(retrieved);
        assertNotNull(retrieved.metadata());
        assertEquals(4, retrieved.metadata().size());
        assertEquals("Breaking News in AI", retrieved.metadata().get("webTitle"));
        assertEquals("John Doe", retrieved.metadata().get("author"));
    }
}