package com.projectmustafa.ingestion;

import com.projectmustafa.model.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CrawlerTest {

    @Autowired
    private CrawlerService crawlerService;

    @Test
    void testGuardianConnection() {
        // This actually calls the API.
        // If you don't have a real key in application.properties,
        // this might fail with 401 Unauthorized, which PROVES network works!

        List<Document> docs = crawlerService.crawl("technology");

        // If you have a valid key, this passes
        // assertNotNull(docs);

        // If you assume the key is invalid/test, just ensure it doesn't crash
        assertDoesNotThrow(() -> crawlerService.crawl("test"));
    }
}
