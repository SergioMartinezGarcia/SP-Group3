package com.unipd.dei.sp.mallet;

import cc.mallet.topics.ParallelTopicModel;
import com.unipd.dei.sp.model.Document;
import com.unipd.dei.sp.model.Topic;
import com.unipd.dei.sp.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MalletTopicModelingService.
 * Tests topic model training, inference, and topic retrieval functionality.
 */
@ExtendWith(MockitoExtension.class)
class MalletTopicModelingServiceTest {

    @Mock
    private TopicRepository topicRepository;

    @InjectMocks
    private MalletTopicModelingService malletService;

    private List<Document> sampleDocuments;

    @BeforeEach
    void setUp() {
        sampleDocuments = createSampleDocuments();
    }

    @Test
    @DisplayName("Model should not be trained initially")
    void testModelNotTrainedInitially() {
        assertFalse(malletService.isModelTrained());
    }

    @Test
    @DisplayName("Train topic model successfully with sample documents")
    void testTrainTopicModel() throws IOException {
        when(topicRepository.save(any(Topic.class))).thenAnswer(i -> i.getArgument(0));

        malletService.trainTopicModel(sampleDocuments);

        assertTrue(malletService.isModelTrained());
        verify(topicRepository, times(1)).deleteAll();
        verify(topicRepository, atLeast(1)).save(any(Topic.class));
    }

    @Test
    @DisplayName("Train model saves correct number of topics to repository")
    void testTrainModelSavesTopics() throws IOException {
        when(topicRepository.save(any(Topic.class))).thenAnswer(i -> i.getArgument(0));

        malletService.trainTopicModel(sampleDocuments);

        // Should save 10 topics (NUM_TOPICS = 10)
        verify(topicRepository, times(10)).save(any(Topic.class));
    }

    @Test
    @DisplayName("Get document topic distribution before training should throw exception")
    void testGetTopicDistributionWithoutTraining() {
        Document doc = sampleDocuments.get(0);

        assertThrows(IllegalStateException.class, () -> {
            malletService.getDocumentTopicDistribution(doc);
        });
    }

    @Test
    @DisplayName("Get document topic distribution after training")
    void testGetDocumentTopicDistribution() throws IOException {
        when(topicRepository.save(any(Topic.class))).thenAnswer(i -> i.getArgument(0));

        // Train the model first
        malletService.trainTopicModel(sampleDocuments);

        // Infer topics for a new document
        Document testDoc = new Document(
            "test-doc",
            "https://example.com/test",
            "science research technology data analysis computer machine learning artificial intelligence",
            "Test Source",
            "123456789",
            Map.of()
        );

        Map<Integer, Double> distribution = malletService.getDocumentTopicDistribution(testDoc);

        assertNotNull(distribution);
        assertFalse(distribution.isEmpty());
        
        // All weights should be between 0 and 1
        for (Double weight : distribution.values()) {
            assertTrue(weight >= 0.0 && weight <= 1.0);
        }
    }

    @Test
    @DisplayName("Topic distribution weights should sum to approximately 1.0")
    void testTopicDistributionSumsToOne() throws IOException {
        when(topicRepository.save(any(Topic.class))).thenAnswer(i -> i.getArgument(0));

        malletService.trainTopicModel(sampleDocuments);

        Document testDoc = sampleDocuments.get(0);
        Map<Integer, Double> distribution = malletService.getDocumentTopicDistribution(testDoc);

        double sum = distribution.values().stream().mapToDouble(Double::doubleValue).sum();
        
        // Sum should be close to 1.0 (allowing for rounding and filtering)
        assertTrue(sum >= 0.8 && sum <= 1.0);
    }

    @Test
    @DisplayName("Get all topics from repository")
    void testGetAllTopics() {
        List<Topic> mockTopics = List.of(
            new Topic(0, List.of("word1", "word2", "word3"), 0.85),
            new Topic(1, List.of("word4", "word5", "word6"), 0.75)
        );

        when(topicRepository.findAll()).thenReturn(mockTopics);

        List<Topic> topics = malletService.getAllTopics();

        assertEquals(2, topics.size());
        verify(topicRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Get topic by ID when topic exists")
    void testGetTopicByIdExists() {
        Topic mockTopic = new Topic(5, List.of("science", "research", "study"), 0.9);
        when(topicRepository.findById(5)).thenReturn(Optional.of(mockTopic));

        Topic topic = malletService.getTopicById(5);

        assertNotNull(topic);
        assertEquals(5, topic.id());
        assertEquals(3, topic.topWords().size());
        verify(topicRepository, times(1)).findById(5);
    }

    @Test
    @DisplayName("Get topic by ID when topic does not exist")
    void testGetTopicByIdNotExists() {
        when(topicRepository.findById(anyInt())).thenReturn(Optional.empty());

        Topic topic = malletService.getTopicById(99);

        assertNull(topic);
        verify(topicRepository, times(1)).findById(99);
    }

    @Test
    @DisplayName("Train model with empty document list")
    void testTrainWithEmptyDocuments() throws IOException {
        List<Document> emptyList = new ArrayList<>();

        assertDoesNotThrow(() -> malletService.trainTopicModel(emptyList));
        
        verify(topicRepository, times(1)).deleteAll();
    }

    @Test
    @DisplayName("Train model with documents containing null content")
    void testTrainWithNullContent() throws IOException {
        List<Document> docsWithNull = List.of(
            new Document("1", "url1", null, "source", "123", Map.of()),
            new Document("2", "url2", "valid content here", "source", "124", Map.of())
        );

        when(topicRepository.save(any(Topic.class))).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> malletService.trainTopicModel(docsWithNull));
        assertTrue(malletService.isModelTrained());
    }

    @Test
    @DisplayName("Train model with documents containing empty content")
    void testTrainWithEmptyContent() throws IOException {
        List<Document> docsWithEmpty = List.of(
            new Document("1", "url1", "", "source", "123", Map.of()),
            new Document("2", "url2", "   ", "source", "124", Map.of())
        );

        when(topicRepository.save(any(Topic.class))).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> malletService.trainTopicModel(docsWithEmpty));
    }

    @Test
    @DisplayName("Infer topics for document with short content")
    void testInferTopicsShortContent() throws IOException {
        when(topicRepository.save(any(Topic.class))).thenAnswer(i -> i.getArgument(0));

        malletService.trainTopicModel(sampleDocuments);

        Document shortDoc = new Document(
            "short",
            "url",
            "science",
            "source",
            "123",
            Map.of()
        );

        Map<Integer, Double> distribution = malletService.getDocumentTopicDistribution(shortDoc);

        assertNotNull(distribution);
    }

    @Test
    @DisplayName("Infer topics for document with very long content")
    void testInferTopicsLongContent() throws IOException {
        when(topicRepository.save(any(Topic.class))).thenAnswer(i -> i.getArgument(0));

        malletService.trainTopicModel(sampleDocuments);

        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longContent.append("science technology research data analysis computer programming ");
        }

        Document longDoc = new Document(
            "long",
            "url",
            longContent.toString(),
            "source",
            "123",
            Map.of()
        );

        Map<Integer, Double> distribution = malletService.getDocumentTopicDistribution(longDoc);

        assertNotNull(distribution);
        assertFalse(distribution.isEmpty());
    }

    @Test
    @DisplayName("Topic distribution should only include topics above threshold")
    void testTopicDistributionThreshold() throws IOException {
        when(topicRepository.save(any(Topic.class))).thenAnswer(i -> i.getArgument(0));

        malletService.trainTopicModel(sampleDocuments);

        Document doc = sampleDocuments.get(0);
        Map<Integer, Double> distribution = malletService.getDocumentTopicDistribution(doc);

        // All included topics should have weight > 0.01 (threshold in code)
        for (Double weight : distribution.values()) {
            assertTrue(weight > 0.01);
        }
    }

    @Test
    @DisplayName("Multiple training calls should work")
    void testMultipleTrainingCalls() throws IOException {
        when(topicRepository.save(any(Topic.class))).thenAnswer(i -> i.getArgument(0));

        malletService.trainTopicModel(sampleDocuments);
        assertTrue(malletService.isModelTrained());

        // Train again with different documents
        malletService.trainTopicModel(createDifferentDocuments());
        assertTrue(malletService.isModelTrained());

        verify(topicRepository, times(2)).deleteAll();
    }

    @Test
    @DisplayName("Saved topics should have valid structure")
    void testSavedTopicsStructure() throws IOException {
        List<Topic> capturedTopics = new ArrayList<>();
        when(topicRepository.save(any(Topic.class))).thenAnswer(invocation -> {
            Topic topic = invocation.getArgument(0);
            capturedTopics.add(topic);
            return topic;
        });

        malletService.trainTopicModel(sampleDocuments);

        assertEquals(10, capturedTopics.size());

        for (Topic topic : capturedTopics) {
            assertNotNull(topic);
            assertTrue(topic.id() >= 0 && topic.id() < 10);
            assertNotNull(topic.topWords());
            assertTrue(topic.topWords().size() <= 25); // NUM_TOP_WORDS
        }
    }

    @Test
    @DisplayName("Handle documents with special characters")
    void testDocumentsWithSpecialCharacters() throws IOException {
        List<Document> specialDocs = List.of(
            new Document("1", "url", "science & technology: research, data!", "source", "123", Map.of()),
            new Document("2", "url", "artificial-intelligence machine_learning deep@learning", "source", "124", Map.of())
        );

        when(topicRepository.save(any(Topic.class))).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> malletService.trainTopicModel(specialDocs));
        assertTrue(malletService.isModelTrained());
    }

    @Test
    @DisplayName("Topic words should not contain stopwords")
    void testTopicWordsExcludeStopwords() throws IOException {
        List<Topic> capturedTopics = new ArrayList<>();
        when(topicRepository.save(any(Topic.class))).thenAnswer(invocation -> {
            Topic topic = invocation.getArgument(0);
            capturedTopics.add(topic);
            return topic;
        });

        malletService.trainTopicModel(sampleDocuments);

        // Common stopwords that should be filtered out
        List<String> stopwords = List.of("the", "a", "an", "and", "or", "but", "in", "on", "at");

        for (Topic topic : capturedTopics) {
            for (String word : topic.topWords()) {
                assertFalse(stopwords.contains(word.toLowerCase()), 
                    "Topic words should not contain stopword: " + word);
            }
        }
    }

    // Helper method to create sample documents for testing
    private List<Document> createSampleDocuments() {
        return List.of(
            new Document(
                "doc-1",
                "https://example.com/science",
                "science research experiment laboratory hypothesis theory analysis data observation",
                "Source",
                "123",
                Map.of()
            ),
            new Document(
                "doc-2",
                "https://example.com/tech",
                "technology computer software programming algorithm artificial intelligence machine learning",
                "Source",
                "124",
                Map.of()
            ),
            new Document(
                "doc-3",
                "https://example.com/health",
                "health medicine patient treatment diagnosis doctor hospital medical care therapy",
                "Source",
                "125",
                Map.of()
            ),
            new Document(
                "doc-4",
                "https://example.com/business",
                "business economy market finance investment stock trading company profit revenue",
                "Source",
                "126",
                Map.of()
            ),
            new Document(
                "doc-5",
                "https://example.com/sports",
                "sports athlete competition championship tournament victory team player game match",
                "Source",
                "127",
                Map.of()
            )
        );
    }

    // Helper method to create different set of documents
    private List<Document> createDifferentDocuments() {
        return List.of(
            new Document(
                "new-1",
                "url",
                "climate change environment sustainability renewable energy carbon emissions",
                "Source",
                "200",
                Map.of()
            ),
            new Document(
                "new-2",
                "url",
                "education learning student teacher school university academic knowledge",
                "Source",
                "201",
                Map.of()
            )
        );
    }
}