package com.unipd.dei.sp.ingestion;

import com.unipd.dei.sp.model.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class GuardianCrawlerTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GuardianCrawler guardianCrawler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(guardianCrawler, "apiKey", "test-key");
        ReflectionTestUtils.setField(guardianCrawler, "restTemplate", restTemplate);
    }

    @Test
    void testCrawl_Success() {
        String mockResponse = "{\"response\":{\"results\":[{\"id\":\"1\",\"webUrl\":\"http://example.com\",\"fields\":{\"bodyText\":\"Test content\"},\"sectionName\":\"Test Section\"}]}}";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);

        List<Document> docs = guardianCrawler.crawl("test");

        assertNotNull(docs);
        assertEquals(1, docs.size());
        assertEquals("1", docs.get(0).id());
        assertEquals("Test content", docs.get(0).content());
    }

    @Test
    void testCrawl_EmptyResponse() {
        String mockResponse = "{\"response\":{\"results\":[]}}";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);

        List<Document> docs = guardianCrawler.crawl("test");

        assertNotNull(docs);
        assertTrue(docs.isEmpty());
    }
}
