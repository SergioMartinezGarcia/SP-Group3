package com.unipd.dei.sp.ingestion;

import com.unipd.dei.sp.elasticsearch.ElasticSearchService;
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

/*
 * Controller for the complete document ingestion and processing pipeline.
 * 
 * The pipeline works in two main steps:
 * 1. Extract topics from documents (trainTopicModel phase)
 * 2. Filter documents based on those topics (TopicInferencer phase)
 * 
 * Flow: crawl -> keyword filter -> store -> train topics -> topic filter -> enrich -> update -> index
 */
@RestController
public class IngestionController {

    @Autowired
    private CrawlerService crawlerService;

    @Autowired
    private RelevanceFilterService filterService;
   
    @Autowired
    private MalletTopicModelingService malletService;
   
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private ElasticSearchService elasticsearchService;

    // Topics below this weight threshold are ignored
    private static final double TOPIC_WEIGHT_THRESHOLD = 0.10;

    /*
     * Main endpoint that runs the complete document processing pipeline.
     * 
     * STEP 1: Build the topic model
     * - Crawl documents from The Guardian
     * - Filter out irrelevant documents using keyword matching
     * - Store relevant documents in MongoDB
     * - Train the topic model on these documents
     * 
     * STEP 2: Filter by topics and finalize
     * - Use the trained model to get topic distributions
     * - Keep only documents containing specified topics
     * - Enrich documents with their topic information
     * - Remove filtered documents from the database
     * - Index the final set to Elasticsearch
     */
    @PostMapping("/api/trigger/crawl-and-index")
    public String crawlAndIndex(
            @RequestParam(name = "topic", defaultValue = "science") String topic,
            @RequestParam(name = "targetTopics", required = false) String targetTopicsParam) {
        
        System.out.println("=== UNIFIED PIPELINE STARTED ===");
        System.out.println("Keyword: " + topic);
        
        Set<Integer> targetTopics = parseTargetTopics(targetTopicsParam);
        if (targetTopics != null && !targetTopics.isEmpty()) {
            System.out.println("Target topics for filtering: " + targetTopics);
        }
        
        try {
            // Step 1: Build the topic model from crawled documents
            System.out.println("\n=== STEP 1: EXTRACT TOPICS ===");
            
            // Crawl articles from The Guardian API
            System.out.println("\n[1.1] Crawling documents from The Guardian API...");
            List<Document> crawledDocs = crawlerService.crawl(topic);
            System.out.println("Crawled " + crawledDocs.size() + " documents");
            
            // Apply keyword relevance filtering and store
            System.out.println("\n[1.2] Filtering by keyword relevance and storing...");
            int stored = 0;
            int keywordFiltered = 0;
            
            for (Document doc : crawledDocs) {
                boolean isRelevant = filterService.isRelevant(doc, topic);
                
                if (isRelevant) {
                    documentRepository.save(doc);
                    stored++;
                } else {
                    keywordFiltered++;
                }
            }
            
            System.out.println("Stored: " + stored + " documents");
            System.out.println("Keyword filtered out: " + keywordFiltered + " documents");
            
            // Get complete document set from database
            List<Document> allDocuments = documentRepository.findAll();
            
            if (allDocuments.isEmpty()) {
                return "No documents to train on. Please crawl some documents first.";
            }
            
            // Train the topic model
            System.out.println("\n[1.3] Training topic model on " + allDocuments.size() + " total documents...");
            malletService.trainTopicModel(allDocuments);
            System.out.println("Topic model trained successfully");
            
            // Step 2: Filter by topics and finalize dataset
            if (targetTopics != null && !targetTopics.isEmpty()) {
                System.out.println("\n=== STEP 2: FILTER BY TOPICS (using TopicInferencer) ===");
                
                List<Document> documentsToKeep = new ArrayList<>();
                List<Document> documentsToDelete = new ArrayList<>();
                int topicFilteredCount = 0;
                int errorCount = 0;
                
                // Check each document against target topics
                System.out.println("\n[2.1] Using TopicInferencer to filter documents by topics: " + targetTopics);
                
                for (Document doc : allDocuments) {
                    try {
                        // Get topic distribution from the trained model
                        Map<Integer, Double> topicDistribution = malletService.getDocumentTopicDistribution(doc);
                        
                        // Check if document has any target topic
                        boolean hasTargetTopic = false;
                        for (Integer targetTopic : targetTopics) {
                            Double weight = topicDistribution.get(targetTopic);
                            if (weight != null && weight >= TOPIC_WEIGHT_THRESHOLD) {
                                hasTargetTopic = true;
                                System.out.println("  Document " + doc.id() + " contains topic " + 
                                                 targetTopic + " (weight: " + String.format("%.2f", weight) + ") - KEEPING");
                                break;
                            }
                        }
                        
                        if (hasTargetTopic) {
                            Document enriched = enrichDocumentWithTopics(doc, topicDistribution);
                            documentsToKeep.add(enriched);
                        } else {
                            documentsToDelete.add(doc);
                            topicFilteredCount++;
                            System.out.println("  Document " + doc.id() + " does not contain target topics - FILTERING OUT");
                        }
                        
                    } catch (Exception e) {
                        System.err.println("Error processing document " + doc.id() + ": " + e.getMessage());
                        // On error, filter out the document since we cannot determine its topics
                        documentsToDelete.add(doc);
                        errorCount++;
                    }
                }
                
                System.out.println("\nDocuments to keep: " + documentsToKeep.size());
                System.out.println("Documents to filter out: " + topicFilteredCount);
                if (errorCount > 0) {
                    System.out.println("Documents with processing errors (filtered out): " + errorCount);
                }
                
                // Remove filtered documents from database
                System.out.println("\n[2.2] Removing filtered documents from MongoDB...");
                for (Document doc : documentsToDelete) {
                    documentRepository.delete(doc);
                }
                System.out.println("Removed " + documentsToDelete.size() + " documents");
                
                // Update kept documents with topic information
                System.out.println("\n[2.3] Updating kept documents in MongoDB...");
                documentRepository.saveAll(documentsToKeep);
                System.out.println("Updated " + documentsToKeep.size() + " documents");
                
                // Index to Elasticsearch
                System.out.println("\n[2.4] Indexing filtered documents to Elasticsearch...");
                elasticsearchService.bulkIndexDocuments(documentsToKeep);
                System.out.println("Indexed " + documentsToKeep.size() + " documents");
                
                // Print summary
                System.out.println("\n=== SUMMARY (with topic filtering) ===");
                System.out.println("Crawled: " + crawledDocs.size());
                System.out.println("Keyword filtered: " + keywordFiltered);
                System.out.println("Topic filtered: " + topicFilteredCount);
                if (errorCount > 0) {
                    System.out.println("Errors (filtered out): " + errorCount);
                }
                System.out.println("Final documents stored: " + documentsToKeep.size());
                System.out.println("Target topics: " + targetTopics);
                System.out.println("======================================\n");
                
                return String.format(
                    "Pipeline completed! Crawled %d documents, filtered by keyword (%d removed), " +
                    "trained topic model on %d documents, filtered by topics %s (%d removed). " +
                    "Final stored documents: %d",
                    crawledDocs.size(), keywordFiltered, allDocuments.size(), 
                    targetTopics, documentsToDelete.size(), documentsToKeep.size()
                );
                
            } else {
                // No topic filtering - enrich all documents
                System.out.println("\n=== NO TOPIC FILTERING - Enriching all documents ===");
                
                List<Document> enrichedDocuments = new ArrayList<>();
                
                for (Document doc : allDocuments) {
                    try {
                        Map<Integer, Double> topicDistribution = malletService.getDocumentTopicDistribution(doc);
                        Document enriched = enrichDocumentWithTopics(doc, topicDistribution);
                        enrichedDocuments.add(enriched);
                    } catch (Exception e) {
                        System.err.println("Error enriching document " + doc.id() + ": " + e.getMessage());
                        enrichedDocuments.add(doc);
                    }
                }
                
                System.out.println("\n[3] Updating MongoDB with enriched documents...");
                documentRepository.saveAll(enrichedDocuments);
                System.out.println("Updated " + enrichedDocuments.size() + " documents");
                
                System.out.println("\n[4] Indexing to Elasticsearch...");
                elasticsearchService.bulkIndexDocuments(enrichedDocuments);
                System.out.println("Indexed " + enrichedDocuments.size() + " documents");
                
                System.out.println("\n=== SUMMARY (no topic filtering) ===");
                System.out.println("Crawled: " + crawledDocs.size());
                System.out.println("Keyword filtered: " + keywordFiltered);
                System.out.println("Total documents: " + allDocuments.size());
                System.out.println("All documents enriched with topics");
                System.out.println("====================================\n");
                
                return String.format(
                    "Pipeline completed! Crawled %d new documents for '%s'. " +
                    "Total documents: %d. All documents enriched with topic information.",
                    stored, topic, allDocuments.size()
                );
            }
            
        } catch (Exception e) {
            System.err.println("ERROR in pipeline: " + e.getMessage());
            e.printStackTrace();
            return "Error in pipeline: " + e.getMessage();
        }
    }

    // Parses comma-separated topic IDs from request parameter
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

    /*
     * Adds topic information to a document using the topic distribution.
     * The distribution comes from the TopicInferencer after model training.
     */
    private Document enrichDocumentWithTopics(Document doc, Map<Integer, Double> topicDistribution) {
        try {
            Map<Integer, Document.TopicInfo> topicInfoMap = new HashMap<>();
            
            for (Map.Entry<Integer, Double> entry : topicDistribution.entrySet()) {
                int topicId = entry.getKey();
                double weight = entry.getValue();
                
                // Get the top words for this topic
                Topic topic = malletService.getTopicById(topicId);
                List<String> topWords = topic != null ? topic.topWords() : List.of();
                
                topicInfoMap.put(topicId, new Document.TopicInfo(
                    topicId,
                    weight,
                    topWords
                ));
            }
            
            // Create new document with topic information
            return new Document(
                doc.id(),
                doc.url(),
                doc.content(),
                doc.source(),
                doc.timestamp(),
                doc.metadata(),
                topicInfoMap
            );
            
        } catch (Exception e) {
            System.err.println("Failed to enrich document with topics: " + e.getMessage());
            return doc;
        }
    }
}