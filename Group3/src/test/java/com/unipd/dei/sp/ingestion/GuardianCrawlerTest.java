package com.unipd.dei.sp.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unipd.dei.sp.model.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GuardianCrawler using Mockito.
 * Tests API interaction and response parsing without making real HTTP calls.
 */
@ExtendWith(MockitoExtension.class)
class GuardianCrawlerTest {

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private GuardianCrawler guardianCrawler;

    private static final String API_KEY = "test-api-key";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(guardianCrawler, "apiKey", API_KEY);
        ReflectionTestUtils.setField(guardianCrawler, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(guardianCrawler, "mapper", objectMapper);
    }

    @Test
    @DisplayName("Successfully crawl documents from Guardian API")
    void testSuccessfulCrawl() {
        String mockResponse = """
            {
              "response": {
                "results": [
                  {
                    "id": "science/2025/jan/19/article-1",
                    "webUrl": "https://www.theguardian.com/science/2025/jan/19/article-1",
                    "webTitle": "Science Article 1",
                    "sectionName": "Science",
                    "fields": {
                      "bodyText": "This is the body text of the first article about science."
                    }
                  },
                  {
                    "id": "science/2025/jan/19/article-2",
                    "webUrl": "https://www.theguardian.com/science/2025/jan/19/article-2",
                    "webTitle": "Science Article 2",
                    "sectionName": "Science",
                    "fields": {
                      "bodyText": "This is the body text of the second article about research."
                    }
                  }
                ]
              }
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(mockResponse);

        List<Document> documents = guardianCrawler.crawl("science");

        assertNotNull(documents);
        assertEquals(2, documents.size());

        Document doc1 = documents.get(0);
        assertEquals("science/2025/jan/19/article-1", doc1.id());
        assertEquals("https://www.theguardian.com/science/2025/jan/19/article-1", doc1.url());
        assertEquals("This is the body text of the first article about science.", doc1.content());
        assertEquals("The Guardian", doc1.source());
        assertEquals("Science", doc1.metadata().get("section"));
        assertEquals("Science Article 1", doc1.metadata().get("webTitle"));

        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }

    @Test
    @DisplayName("Skip documents with empty body text")
    void testSkipEmptyBodyText() {
        String mockResponse = """
            {
              "response": {
                "results": [
                  {
                    "id": "article-1",
                    "webUrl": "https://example.com/article-1",
                    "webTitle": "Article 1",
                    "sectionName": "News",
                    "fields": {
                      "bodyText": "Valid content"
                    }
                  },
                  {
                    "id": "article-2",
                    "webUrl": "https://example.com/article-2",
                    "webTitle": "Article 2",
                    "sectionName": "News",
                    "fields": {
                      "bodyText": ""
                    }
                  },
                  {
                    "id": "article-3",
                    "webUrl": "https://example.com/article-3",
                    "webTitle": "Article 3",
                    "sectionName": "News",
                    "fields": {}
                  }
                ]
              }
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(mockResponse);

        List<Document> documents = guardianCrawler.crawl("test");

        assertEquals(1, documents.size());
        assertEquals("Valid content", documents.get(0).content());
    }

    @Test
    @DisplayName("Handle empty API response")
    void testEmptyApiResponse() {
        String mockResponse = """
            {
              "response": {
                "results": []
              }
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(mockResponse);

        List<Document> documents = guardianCrawler.crawl("nonexistent");

        assertNotNull(documents);
        assertTrue(documents.isEmpty());
    }

    @Test
    @DisplayName("Handle API errors gracefully")
    void testApiErrorHandling() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenThrow(new RestClientException("API Error"));

        List<Document> documents = guardianCrawler.crawl("test");

        assertNotNull(documents);
        assertTrue(documents.isEmpty());
    }

    @Test
    @DisplayName("Handle malformed JSON response")
    void testMalformedJsonHandling() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn("invalid json {{{");

        List<Document> documents = guardianCrawler.crawl("test");

        assertNotNull(documents);
        assertTrue(documents.isEmpty());
    }

    @Test
    @DisplayName("Handle missing fields in response")
    void testMissingFieldsInResponse() {
        String mockResponse = """
            {
              "response": {
                "results": [
                  {
                    "id": "article-1",
                    "webUrl": "https://example.com/article-1",
                    "fields": {
                      "bodyText": "Content here"
                    }
                  }
                ]
              }
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(mockResponse);

        List<Document> documents = guardianCrawler.crawl("test");

        assertEquals(1, documents.size());
        assertEquals("", documents.get(0).metadata().get("section"));
        assertNotNull(documents.get(0).metadata().get("webTitle"));
    }

    @Test
    @DisplayName("Verify API URL construction")
    void testApiUrlConstruction() {
        String mockResponse = """
            {
              "response": {
                "results": []
              }
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(mockResponse);

        guardianCrawler.crawl("climate change");

        String expectedUrl = "https://content.guardianapis.com/search?show-fields=bodyText&page-size=50&api-key=" 
                           + API_KEY + "&q=climate change";
        
        verify(restTemplate).getForObject(eq(expectedUrl), eq(String.class));
    }

    @Test
    @DisplayName("Handle null response from API")
    void testNullApiResponse() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(null);

        List<Document> documents = guardianCrawler.crawl("test");

        assertNotNull(documents);
        assertTrue(documents.isEmpty());
    }

    @Test
    @DisplayName("Verify document metadata is populated correctly")
    void testMetadataPopulation() {
        String mockResponse = """
            {
              "response": {
                "results": [
                  {
                    "id": "tech/2025/jan/19/ai-breakthrough",
                    "webUrl": "https://www.theguardian.com/tech/2025/jan/19/ai-breakthrough",
                    "webTitle": "Major AI Breakthrough Announced",
                    "sectionName": "Technology",
                    "fields": {
                      "bodyText": "Scientists announce breakthrough in artificial intelligence research."
                    }
                  }
                ]
              }
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(mockResponse);

        List<Document> documents = guardianCrawler.crawl("AI");

        assertEquals(1, documents.size());
        Document doc = documents.get(0);
        
        assertNotNull(doc.metadata());
        assertEquals("Technology", doc.metadata().get("section"));
        assertEquals("Major AI Breakthrough Announced", doc.metadata().get("webTitle"));
    }

    @Test
    @DisplayName("Verify timestamp is set")
    void testTimestampIsSet() {
        String mockResponse = """
            {
              "response": {
                "results": [
                  {
                    "id": "article-1",
                    "webUrl": "https://example.com/article",
                    "webTitle": "Test",
                    "sectionName": "News",
                    "fields": {
                      "bodyText": "Content"
                    }
                  }
                ]
              }
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(mockResponse);

        List<Document> documents = guardianCrawler.crawl("test");

        assertNotNull(documents.get(0).timestamp());
        assertFalse(documents.get(0).timestamp().isEmpty());
    }
}