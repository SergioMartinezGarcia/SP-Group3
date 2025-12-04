package com.unipd.dei.sp.ingestion;

import com.unipd.dei.sp.model.Document;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RelevanceFilterServiceTest {

    private final RelevanceFilterService filterService = new RelevanceFilterService();

    @Test
    void testIsRelevant_HighRelevance() {
        Document doc = new Document("1", "http://example.com/science", "This is a science article.", "Source",
                Instant.now(), Map.of("section", "science"));
        assertTrue(filterService.isRelevant(doc, "science"));
    }

    @Test
    void testIsRelevant_LowRelevance() {
        Document doc = new Document("2", "http://example.com/other", "This is a random article.", "Source",
                Instant.now(), Map.of("section", "other"));
        assertFalse(filterService.isRelevant(doc, "science"));
    }

    @Test
    void testIsRelevant_NullTopic() {
        Document doc = new Document("3", "http://example.com", "Content", "Source", Instant.now(), Map.of());
        assertFalse(filterService.isRelevant(doc, null));
    }
}
