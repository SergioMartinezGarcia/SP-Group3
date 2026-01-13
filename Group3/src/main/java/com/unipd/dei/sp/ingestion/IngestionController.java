package com.unipd.dei.sp.ingestion;

import com.unipd.dei.sp.mallet.MalletTopicModelingService;
import com.unipd.dei.sp.model.Document;
import com.unipd.dei.sp.model.Topic;
import com.unipd.dei.sp.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;

/**
 * Exposes endpoints to trigger crawling and filtering of documents.
 * 
 * Pipeline: crawl → keyword filter → topic filter → enrich → store
 */
@RestController
public class IngestionController {

    @Autowired
    private CrawlerService crawlerService;

    @Autowired
    private RelevanceFilterService filterService;
   
    @Autowired
    private TopicFilterService topicFilterService;
   
    @Autowired
    private MalletTopicModelingService malletService;
   
    @Autowired
    private DocumentRepository documentRepository;

    /**
     * Trigger manual crawling and ingestion with keyword and optional topic filtering.
     * Documents are enriched with topic information before storing.
     * 
     * @param topic The topic keyword used for crawling and keyword filtering
     * @param targetTopicsParam Comma-separated topic IDs to filter by (e.g., "2,5,7")
     * @return Status message
     */
    @PostMapping("/api/trigger")
    public String manualTrigger(
            @RequestParam(name = "topic", defaultValue = "science") String topic,
            @RequestParam(name = "targetTopics", required = false) String targetTopicsParam) {
        
        Set<Integer> targetTopics = parseTargetTopics(targetTopicsParam);
        runPipeline(topic, targetTopics);
        return "Crawling and filtering completed for topic: " + topic;
    }

    /**
     * Parse comma-separated topic IDs into a Set
     */
    private Set<Integer> parseTargetTopics(String targetTopicsParam) {
        if (targetTopicsParam == null || targetTopicsParam.trim().isEmpty()) {
            return null;
        }
        
        Set<Integer> topics = new HashSet<>();
        for (String topicId : targetTopicsParam.split(",")) {
            try {
                topics.add(Integer.parseInt(topicId.trim()));
            } catch (NumberFormatException e) {
                System.err.println("Invalid topic ID: " + topicId);
            }
        }
        return topics.isEmpty() ? null : topics;
    }

    /**
     * Executes the ingestion pipeline: crawl → keyword filter → topic filter → enrich → store.
     * 
     * Documents are filtered BEFORE storing to MongoDB.
     * This ensures only filtered documents are used for training and indexing.
     *
     * @param topic The topic used for crawling and keyword filtering
     * @param targetTopics Set of topic IDs to filter for (null = no topic filter)
     */
    private void runPipeline(String topic, Set<Integer> targetTopics) {
        System.out.println("--- Starting Pipeline for: " + topic + " ---");

        // Step 1: Gather documents from The Guardian API
        List<Document> docs = crawlerService.crawl(topic);
        System.out.println("Found " + docs.size() + " documents from crawler.");

        // Check if topic filtering is enabled
        boolean topicFilterEnabled = malletService.isModelTrained();
        if (topicFilterEnabled && targetTopics != null && !targetTopics.isEmpty()) {
            System.out.println("Topic filtering: ENABLED (filtering for topics: " + targetTopics + ")");
        } else if (topicFilterEnabled) {
            System.out.println("Topic filtering: AVAILABLE but no target topics specified");
        } else {
            System.out.println("Topic filtering: DISABLED (model not trained yet)");
        }

        int keywordFiltered = 0;
        int topicFiltered = 0;
        int stored = 0;

        // Step 2: Apply filters, enrich, and store
        for (Document doc : docs) {
            try {
                // FILTER 1: Keyword-based relevance filter
                boolean isRelevant = filterService.isRelevant(doc, topic);
                
                if (!isRelevant) {
                    keywordFiltered++;
                    System.out.println("Document " + doc.id() + " filtered by keyword filter");
                    continue;
                }

                // FILTER 2: Topic-based filter (only if model trained AND target topics specified)
                if (topicFilterEnabled && targetTopics != null && !targetTopics.isEmpty()) {
                    boolean hasTargetTopics = topicFilterService.hasTargetTopics(doc, targetTopics);
                    
                    if (!hasTargetTopics) {
                        topicFiltered++;
                        System.out.println("Document " + doc.id() + " filtered by topic filter");
                        continue;
                    }
                }

                // ENRICH: Add topic information to document (if model is trained)
                Document enrichedDoc = enrichDocumentWithTopics(doc);

                // Document passed all filters - store enriched version
                documentRepository.save(enrichedDoc);
                stored++;
                System.out.println("✓ Stored document: " + enrichedDoc.id());

            } catch (Exception e) {
                System.err.println("Error processing document: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Print summary
        System.out.println("\n--- Pipeline Summary ---");
        System.out.println("Total crawled: " + docs.size());
        System.out.println("Filtered by keyword: " + keywordFiltered);
        System.out.println("Filtered by topic: " + topicFiltered);
        System.out.println("Successfully stored: " + stored);
        System.out.println("------------------------\n");
    }

    /**
     * Enrich document with topic information.
     * For each topic in the document's distribution, includes:
     * - Topic ID
     * - Weight (probability)
     * - Top words
     * 
     * @param doc The document to enrich
     * @return Enriched document with topic information
     */
    private Document enrichDocumentWithTopics(Document doc) {
        if (!malletService.isModelTrained()) {
            return doc; // No enrichment if model not trained
        }
        
        try {
            // Get topic distribution using TopicInferencer
            Map<Integer, Double> topicDist = malletService.getDocumentTopicDistribution(doc);
            
            // Build topic info map
            Map<Integer, Document.TopicInfo> topicInfoMap = new HashMap<>();
            
            for (Map.Entry<Integer, Double> entry : topicDist.entrySet()) {
                int topicId = entry.getKey();
                double weight = entry.getValue();
                
                // Get top words for this topic from MongoDB
                Topic topic = malletService.getTopicById(topicId);
                List<String> topWords = topic != null ? topic.topWords() : List.of();
                
                topicInfoMap.put(topicId, new Document.TopicInfo(
                    topicId,
                    weight,
                    topWords
                ));
            }
            
            // Return enriched document
            return new Document(
                doc.id(),
                doc.url(),
                doc.content(),
                doc.source(),
                doc.timestamp(),
                doc.metadata(),
                topicInfoMap  // ← Enriched with topics
            );
            
        } catch (IOException e) {
            System.err.println("Failed to enrich document with topics: " + e.getMessage());
            return doc; // Return original document on error
        }
    }
}