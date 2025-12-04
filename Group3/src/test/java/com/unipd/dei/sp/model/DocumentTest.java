package com.unipd.dei.sp.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DocumentTest {

    @Test
    void testDocumentCreation() {
        String id = "123";
        String url = "http://example.com";
        String content = "Test content";
        String source = "Test Source";
        Instant timestamp = Instant.now();
        Map<String, String> metadata = Map.of("key", "value");

        Document doc = new Document(id, url, content, source, timestamp, metadata);

        assertEquals(id, doc.id());
        assertEquals(url, doc.url());
        assertEquals(content, doc.content());
        assertEquals(source, doc.source());
        assertEquals(timestamp, doc.timestamp());
        assertEquals(metadata, doc.metadata());
    }

    @Test
    void testDocumentEquality() {
        Instant timestamp = Instant.now();
        Document doc1 = new Document("1", "url", "content", "source", timestamp, Map.of());
        Document doc2 = new Document("1", "url", "content", "source", timestamp, Map.of());

        assertEquals(doc1, doc2);
    }
}
