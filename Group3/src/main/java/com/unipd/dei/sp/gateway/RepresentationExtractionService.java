package com.unipd.dei.sp.gateway;

import com.unipd.dei.sp.elasticsearch.ElasticSearchService;
import com.unipd.dei.sp.mallet.MalletTopicModelingService;
import com.unipd.dei.sp.model.Document;
import com.unipd.dei.sp.model.Topic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Representation Extraction Service
 * Bridges Elasticsearch and Mallet to provide topic-enriched search results
 */
@Service
public class RepresentationExtractionService {

    @Autowired
    private ElasticSearchService elasticsearchService;

    @Autowired
    private MalletTopicModelingService malletService;

    /**
     * Search documents and enrich with topic information
     */
    public GatewayController.SearchResponse searchWithTopics(String query, Integer topicFilter) {
        // Step 1: Query Elasticsearch
        List<Document> documents = elasticsearchService.searchByContent(query);

        // Step 2: Get all topics from MongoDB
        Map<Integer, Topic> topicsMap = malletService.getAllTopics().stream()
                .collect(Collectors.toMap(Topic::id, t -> t));

        // Step 3: Enrich documents with topic information
        List<GatewayController.EnrichedDocument> enrichedDocs = documents.stream()
                .map(doc -> enrichDocument(doc, topicsMap))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Step 4: Apply topic filter if specified
        if (topicFilter != null) {
            enrichedDocs = enrichedDocs.stream()
                    .filter(doc -> doc.topics().containsKey(topicFilter))
                    .collect(Collectors.toList());
        }

        return new GatewayController.SearchResponse(query, enrichedDocs);
    }

    /**
     * Enrich a document with topic distribution
     */
    private GatewayController.EnrichedDocument enrichDocument(Document doc, Map<Integer, Topic> topicsMap) {
        try {
            // Get topic distribution for this document
            Map<Integer, Double> topicDistribution = malletService.getDocumentTopicDistribution(doc);

            // Create topic info map
            Map<Integer, GatewayController.TopicInfo> topicInfoMap = new HashMap<>();
            for (Map.Entry<Integer, Double> entry : topicDistribution.entrySet()) {
                Integer topicId = entry.getKey();
                Double weight = entry.getValue();
                Topic topic = topicsMap.get(topicId);
                
                if (topic != null) {
                    topicInfoMap.put(topicId, new GatewayController.TopicInfo(
                        topicId,
                        weight,
                        topic.topWords()
                    ));
                }
            }

            return new GatewayController.EnrichedDocument(
                doc.id(),
                extractTitle(doc),
                doc.url(),
                truncateContent(doc.content()),
                topicInfoMap
            );
        } catch (IOException e) {
            System.err.println("Error enriching document: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extract title from document metadata
     */
    private String extractTitle(Document doc) {
        if (doc.metadata() != null && doc.metadata().containsKey("webTitle")) {
            return doc.metadata().get("webTitle");
        }
        return doc.id();
    }

    /**
     * Truncate content for display
     */
    private String truncateContent(String content) {
        if (content == null) return "";
        return content.length() > 500 ? content.substring(0, 500) + "..." : content;
    }

    /**
     * Get all topics
     */
    public List<Topic> getAllTopics() {
        return malletService.getAllTopics();
    }

    /**
     * Get topic by ID
     */
    public Topic getTopicById(Integer topicId) {
        return malletService.getTopicById(topicId);
    }
}