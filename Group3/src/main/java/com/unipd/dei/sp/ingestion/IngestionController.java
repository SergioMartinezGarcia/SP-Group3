package com.unipd.dei.sp.ingestion;

import com.unipd.dei.sp.model.Document;
import com.unipd.dei.sp.repository.DocumentRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * @class IngestionController
 * @brief Exposes endpoints to trigger crawling, filtering, and forwarding of documents.
 *
 * This controller retrieves raw data using {@link CrawlerService}, applies relevance filtering, and sends the data to(Change later).
 */
@RestController

public class IngestionController {

    @Autowired
    private CrawlerService crawlerService;

    @Autowired
    private RelevanceFilterService filterService;
   
    @Autowired
    private DocumentRepository documentRepository;

    private String coreUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // Trigger via Postman or UI
    @PostMapping("/api/trigger")
    public String manualTrigger(@RequestParam(name = "topic", defaultValue = "science") String topic) {
        runPipeline(topic);
        return "Crawling started for topic: " + topic;
    }
    /**
     * @brief Executes the ingestion pipeline: crawl → filter → forward.
     *
     * Steps performed:
     * 1. Calls {@link CrawlerService#crawl(String)} to gather articles.
     * 2. Applies {@link RelevanceFilterService#isRelevant(Document, String)}.
     * 3. Sends relevant {@link Document} objects to the MongoDB repository.
     *
     * Errors during forwarding are logged but do not halt the pipeline.
     *
     * @param topic The topic used for crawling and filtering documents.
     */
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
                    documentRepository.save(doc);
                    System.out.println("Sent doc: " + documentRepository.findById(doc.id()).get().id());
                }
            } catch (Exception e) {
                System.err.println("Could not store in database: " + e.getMessage());
            }
        }
    }

}
