package com.unipd.dei.sp.ingestion;

import com.unipd.dei.sp.model.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController

public class IngestionController {

    @Autowired
    private CrawlerService crawlerService;

    @Autowired
    private RelevanceFilterService filterService;

    @Value("${core.service.url}")
    private String coreUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // Trigger via Postman or UI
    @PostMapping("/api/trigger")
    public String manualTrigger(@RequestParam(name = "topic", defaultValue = "science") String topic) {
        runPipeline(topic);
        return "Crawling started for topic: " + topic;
    }

    private void runPipeline(String topic) {
        System.out.println("--- Starting Pipeline for: " + topic + " ---");

        // Gather
        List<Document> docs = crawlerService.crawl(topic);
        System.out.println("Found " + docs.size() + " documents.");

        // Send to DB
        for (Document doc : docs) {
            try {
                // Apply Filter Logic
                boolean isRelevant = filterService.isRelevant(doc, topic);

                if (isRelevant) {
                    restTemplate.postForObject(coreUrl, doc, Void.class);
                    System.out.println("Sent doc: " + doc.id());
                }
            } catch (Exception e) {
                System.err.println("Could not send to Core Service: " + e.getMessage());
            }
        }
    }

}
