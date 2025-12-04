package com.unipd.dei.sp.ingestion;

import com.unipd.dei.sp.model.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class IngestionControllerTest {

    @Mock
    private CrawlerService crawlerService;

    @Mock
    private RelevanceFilterService filterService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private IngestionController ingestionController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(ingestionController, "coreUrl", "http://core-service/api/documents");
        // Inject the mocked RestTemplate into the controller manually since it's
        // instantiated in the field
        ReflectionTestUtils.setField(ingestionController, "restTemplate", restTemplate);
    }

    @Test
    void testManualTrigger() {
        String topic = "science";
        Document doc = new Document("1", "url", "content", "source", Instant.now(), Map.of());

        when(crawlerService.crawl(topic)).thenReturn(List.of(doc));
        when(filterService.isRelevant(doc, topic)).thenReturn(true);

        String response = ingestionController.manualTrigger(topic);

        assertEquals("Crawling started for topic: " + topic, response);
        verify(crawlerService, times(1)).crawl(topic);
        verify(filterService, times(1)).isRelevant(doc, topic);
        verify(restTemplate, times(1)).postForObject(anyString(), eq(doc), eq(Void.class));
    }
}
