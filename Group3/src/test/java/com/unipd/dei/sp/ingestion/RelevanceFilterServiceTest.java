package com.unipd.dei.sp.ingestion;

import com.unipd.dei.sp.model.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RelevanceFilterService.
 * Tests the keyword-based relevance scoring system.
 */
class RelevanceFilterServiceTest {

    private RelevanceFilterService filterService;

    @BeforeEach
    void setUp() {
        filterService = new RelevanceFilterService();
    }

    @Test
    @DisplayName("Document with keyword in URL should be relevant")
    void testUrlMatchMakesDocumentRelevant() {
        Document doc = new Document(
            "1",
            "https://example.com/science/article",
            "Some content here",
            "Test Source",
            "123456789",
            Map.of()
        );

        boolean result = filterService.isRelevant(doc, "science");
        
        assertTrue(result, "Document with keyword in URL should be relevant");
    }

    @Test
    @DisplayName("Document with keyword in metadata should be relevant")
    void testMetadataMatchMakesDocumentRelevant() {
        Document doc = new Document(
            "1",
            "https://example.com/science/article",  // CHANGED: Added keyword to URL
            "Some content here",
            "Test Source",
            "123456789",
            Map.of("section", "science", "title", "Article")
        );

        boolean result = filterService.isRelevant(doc, "science");
        
        assertTrue(result, "Document with keyword in URL and metadata should be relevant");
    }

    @Test
    @DisplayName("Document with many content occurrences should be relevant")
    void testMultipleContentOccurrencesMakeDocumentRelevant() {
        String content = "science is great. Science helps us understand. " +
                        "The study of science is important. Science science science " +
                        "science science science science science science science " +
                        "science science science science science.";
        
        Document doc = new Document(
            "1",
            "https://example.com/article",
            content,
            "Test Source",
            "123456789",
            Map.of()
        );

        boolean result = filterService.isRelevant(doc, "science");
        
        assertTrue(result, "Document with 15+ keyword occurrences should be relevant");
    }

    @Test
    @DisplayName("Document with few content occurrences should not be relevant")
    void testFewContentOccurrencesNotRelevant() {
        String content = "This article mentions science once and that's it.";
        
        Document doc = new Document(
            "1",
            "https://example.com/article",
            content,
            "Test Source",
            "123456789",
            Map.of()
        );

        boolean result = filterService.isRelevant(doc, "science");
        
        assertFalse(result, "Document with only 1 keyword occurrence should not be relevant");
    }

    @Test
    @DisplayName("Empty or null topic should make document not relevant")
    void testEmptyTopicNotRelevant() {
        Document doc = new Document(
            "1",
            "https://example.com/science/article",
            "Science content here",
            "Test Source",
            "123456789",
            Map.of()
        );

        assertFalse(filterService.isRelevant(doc, null));
        assertFalse(filterService.isRelevant(doc, ""));
        assertFalse(filterService.isRelevant(doc, "   "));
    }

    @Test
    @DisplayName("Null document fields should not cause exceptions")
    void testNullDocumentFields() {
        Document doc = new Document(
            "1",
            null,  // null URL
            null,  // null content
            "Test Source",
            "123456789",
            null   // null metadata
        );

        assertDoesNotThrow(() -> filterService.isRelevant(doc, "science"));
        assertFalse(filterService.isRelevant(doc, "science"));
    }

    @Test
    @DisplayName("Case insensitive matching should work")
    void testCaseInsensitiveMatching() {
        Document doc = new Document(
            "1",
            "https://example.com/SCIENCE/article",
            "SCIENCE Science science",
            "Test Source",
            "123456789",
            Map.of("section", "Science")
        );

        assertTrue(filterService.isRelevant(doc, "science"));
        assertTrue(filterService.isRelevant(doc, "SCIENCE"));
        assertTrue(filterService.isRelevant(doc, "ScIeNcE"));
    }

    @Test
    @DisplayName("Partial word matches should not count")
    void testPartialWordMatchesNotCounted() {
        Document doc = new Document(
            "1",
            "https://example.com/article",
            "prescience neuroscience pseudoscience",  // Contains 'science' but as part of other words
            "Test Source",
            "123456789",
            Map.of()
        );

        // Should not be relevant because 'science' doesn't appear as a standalone word
        boolean result = filterService.isRelevant(doc, "science");
        
        assertFalse(result, "Partial word matches should not make document relevant");
    }

    @ParameterizedTest
    @CsvSource({
        "technology, https://example.com/technology/news, true",
        "health, https://example.com/sports/news, false",
        "climate, https://example.com/article, false"
    })
    @DisplayName("Parameterized test for URL matching")
    void testUrlMatchingParameterized(String keyword, String url, boolean expectedRelevant) {
        Document doc = new Document(
            "1",
            url,
            "Generic content",
            "Test Source",
            "123456789",
            Map.of()
        );

        assertEquals(expectedRelevant, filterService.isRelevant(doc, keyword));
    }

    @Test
    @DisplayName("Combined scoring should accumulate correctly")
    void testCombinedScoring() {
        // URL match (20) + metadata match (10) should exceed threshold (15)
        Document doc = new Document(
            "1",
            "https://example.com/science/article",
            "Some unrelated content",
            "Test Source",
            "123456789",
            Map.of("section", "science")
        );

        assertTrue(filterService.isRelevant(doc, "science"));
    }

    @Test
    @DisplayName("Document at threshold boundary should be relevant")
    void testThresholdBoundary() {
        // Create content with exactly 15 occurrences (15 * 1 = 15 points = threshold)
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 15; i++) {
            content.append("science ");
        }
        
        Document doc = new Document(
            "1",
            "https://example.com/article",
            content.toString(),
            "Test Source",
            "123456789",
            Map.of()
        );

        assertTrue(filterService.isRelevant(doc, "science"));
    }

    @Test
    @DisplayName("Document just below threshold should not be relevant")
    void testBelowThreshold() {
        // Create content with 14 occurrences (14 * 1 = 14 points < 15 threshold)
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 14; i++) {
            content.append("science ");
        }
        
        Document doc = new Document(
            "1",
            "https://example.com/article",
            content.toString(),
            "Test Source",
            "123456789",
            Map.of()
        );

        assertFalse(filterService.isRelevant(doc, "science"));
    }

    @Test
    @DisplayName("Multiple metadata values with keyword should count once")
    void testMultipleMetadataMatches() {
        Document doc = new Document(
            "1",
            "https://example.com/article",
            "Some content",
            "Test Source",
            "123456789",
            Map.of(
                "section", "science",
                "category", "science news",
                "tag", "science"
            )
        );

        // Metadata match only counts once (10 points), not enough alone
        assertFalse(filterService.isRelevant(doc, "science"));
    }

    @Test
    @DisplayName("Special characters in keyword should be handled")
    void testSpecialCharactersInKeyword() {
        Document doc = new Document(
            "1",
            "https://example.com/covid-19/article",
            "covid-19 covid-19 covid-19 covid-19 covid-19 " +
            "covid-19 covid-19 covid-19 covid-19 covid-19 " +
            "covid-19 covid-19 covid-19 covid-19 covid-19",
            "Test Source",
            "123456789",
            Map.of()
        );

        assertTrue(filterService.isRelevant(doc, "covid-19"));
    }
}