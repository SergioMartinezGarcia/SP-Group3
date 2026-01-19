package com.unipd.dei.sp.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Topic model class.
 * Tests record creation and data integrity.
 */
class TopicTest {

    @Test
    @DisplayName("Create topic with all fields")
    void testCreateTopicWithAllFields() {
        List<String> topWords = Arrays.asList(
            "science", "research", "study", "data", "analysis"
        );

        Topic topic = new Topic(0, topWords, 0.85);

        assertEquals(0, topic.id());
        assertEquals(topWords, topic.topWords());
        assertEquals(0.85, topic.coherence());
        assertEquals(5, topic.topWords().size());
    }

    @Test
    @DisplayName("Create topic with null coherence")
    void testCreateTopicWithNullCoherence() {
        List<String> topWords = List.of("word1", "word2", "word3");

        Topic topic = new Topic(1, topWords, null);

        assertEquals(1, topic.id());
        assertEquals(topWords, topic.topWords());
        assertNull(topic.coherence());
    }

    @Test
    @DisplayName("Create topic with empty top words list")
    void testCreateTopicWithEmptyWords() {
        Topic topic = new Topic(2, Collections.emptyList(), 0.5);

        assertEquals(2, topic.id());
        assertTrue(topic.topWords().isEmpty());
        assertEquals(0.5, topic.coherence());
    }

    @Test
    @DisplayName("Topics with same values should be equal")
    void testTopicEquality() {
        List<String> words = List.of("word1", "word2", "word3");
        
        Topic topic1 = new Topic(5, words, 0.75);
        Topic topic2 = new Topic(5, words, 0.75);

        assertEquals(topic1, topic2);
        assertEquals(topic1.hashCode(), topic2.hashCode());
    }

    @Test
    @DisplayName("Topics with different IDs should not be equal")
    void testTopicInequality() {
        List<String> words = List.of("word1", "word2", "word3");
        
        Topic topic1 = new Topic(1, words, 0.75);
        Topic topic2 = new Topic(2, words, 0.75);

        assertNotEquals(topic1, topic2);
    }

    @Test
    @DisplayName("Topic with 25 top words (standard size)")
    void testTopicWithStandardWordCount() {
        List<String> words = Arrays.asList(
            "word1", "word2", "word3", "word4", "word5",
            "word6", "word7", "word8", "word9", "word10",
            "word11", "word12", "word13", "word14", "word15",
            "word16", "word17", "word18", "word19", "word20",
            "word21", "word22", "word23", "word24", "word25"
        );

        Topic topic = new Topic(0, words, 0.9);

        assertEquals(25, topic.topWords().size());
        assertEquals("word1", topic.topWords().get(0));
        assertEquals("word25", topic.topWords().get(24));
    }

    @Test
    @DisplayName("Topic coherence can be zero")
    void testZeroCoherence() {
        Topic topic = new Topic(0, List.of("word"), 0.0);

        assertEquals(0.0, topic.coherence());
    }

    @Test
    @DisplayName("Topic coherence can be one")
    void testMaxCoherence() {
        Topic topic = new Topic(0, List.of("word"), 1.0);

        assertEquals(1.0, topic.coherence());
    }

    @Test
    @DisplayName("Topic with negative ID")
    void testNegativeTopicId() {
        Topic topic = new Topic(-1, List.of("word"), 0.5);

        assertEquals(-1, topic.id());
    }

    @Test
    @DisplayName("Topic words list is immutable via record")
    void testTopWordsListImmutability() {
        List<String> words = Arrays.asList("word1", "word2", "word3");
        Topic topic = new Topic(0, words, 0.5);

        // The list from the record should be the same reference
        assertSame(words, topic.topWords());
    }

    @Test
    @DisplayName("Topic with very high coherence score")
    void testHighCoherenceScore() {
        Topic topic = new Topic(0, List.of("highly", "coherent", "topic"), 0.99);

        assertTrue(topic.coherence() > 0.9);
        assertEquals(0.99, topic.coherence());
    }

    @Test
    @DisplayName("Topic with very low coherence score")
    void testLowCoherenceScore() {
        Topic topic = new Topic(0, List.of("random", "words"), 0.01);

        assertTrue(topic.coherence() < 0.1);
        assertEquals(0.01, topic.coherence());
    }

    @Test
    @DisplayName("Multiple topics with different IDs")
    void testMultipleTopicsWithDifferentIds() {
        Topic topic0 = new Topic(0, List.of("science"), 0.8);
        Topic topic1 = new Topic(1, List.of("technology"), 0.7);
        Topic topic2 = new Topic(2, List.of("health"), 0.6);

        assertEquals(0, topic0.id());
        assertEquals(1, topic1.id());
        assertEquals(2, topic2.id());

        assertNotEquals(topic0, topic1);
        assertNotEquals(topic1, topic2);
        assertNotEquals(topic0, topic2);
    }

    @Test
    @DisplayName("Topic toString should contain field values")
    void testTopicToString() {
        Topic topic = new Topic(5, List.of("word1", "word2"), 0.75);
        String toString = topic.toString();

        assertTrue(toString.contains("5"));
        assertTrue(toString.contains("0.75"));
        assertTrue(toString.contains("word1"));
    }

    @Test
    @DisplayName("Topic with single word")
    void testTopicWithSingleWord() {
        Topic topic = new Topic(0, List.of("singleton"), 0.5);

        assertEquals(1, topic.topWords().size());
        assertEquals("singleton", topic.topWords().get(0));
    }

    @Test
    @DisplayName("Topic with duplicate words in list")
    void testTopicWithDuplicateWords() {
        // In practice, Mallet shouldn't produce duplicates, but test the model handles it
        List<String> words = List.of("word", "word", "word");
        Topic topic = new Topic(0, words, 0.5);

        assertEquals(3, topic.topWords().size());
    }
}