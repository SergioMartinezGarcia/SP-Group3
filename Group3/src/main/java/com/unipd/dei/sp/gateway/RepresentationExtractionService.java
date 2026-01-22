package com.unipd.dei.sp.gateway;

import com.unipd.dei.sp.elasticsearch.ElasticSearchService;
import com.unipd.dei.sp.mallet.MalletTopicModelingService;
import com.unipd.dei.sp.model.Document;
import com.unipd.dei.sp.model.Topic;
import com.unipd.dei.sp.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/*
 * Service for enriching search results with topic information.
 * Combines document data with topic modeling results.
 */
@Service
public class RepresentationExtractionService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private MalletTopicModelingService malletService;
    
    @Autowired
    private ElasticSearchService elasticSearchService;

    /*
     * Performs a search and enriches results with topic data.
     * Supports wildcard queries to retrieve all documents.
     * Can filter results by a specific topic if provided.
     */
    public GatewayController.SearchResponse searchWithTopics(String query, Integer topicFilter) {
        List<Document> documents;
        
        // Wildcard or empty query returns all documents
        if (query == null || query.trim().isEmpty() || query.equals("*")) {
            documents = documentRepository.findAll();
        } else {
            // Future enhancement: use Elasticsearch for text search
        	documents = elasticSearchService.searchByContent(query);
        }

        Map<Integer, Topic> topicsMap = malletService.getAllTopics().stream()
                .collect(Collectors.toMap(Topic::id, t -> t));

        List<GatewayController.EnrichedDocument> enrichedDocs = documents.stream()
                .map(doc -> enrichDocument(doc, topicsMap))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Keep only documents containing the specified topic
        if (topicFilter != null) {
            enrichedDocs = enrichedDocs.stream()
                    .filter(doc -> doc.topics() != null && doc.topics().containsKey(topicFilter))
                    .collect(Collectors.toList());
        }

        return new GatewayController.SearchResponse(query, enrichedDocs);
    }

    /*
     * Adds topic information to a document.
     * Uses the topics already embedded in the document during indexing.
     */
    private GatewayController.EnrichedDocument enrichDocument(Document doc, Map<Integer, Topic> topicsMap) {
        try {
            Map<Integer, GatewayController.TopicInfo> topicInfoMap = new HashMap<>();
            
            // Documents already contain topic data from the ingestion phase
            if (doc.topics() != null && !doc.topics().isEmpty()) {
                for (Map.Entry<Integer, Document.TopicInfo> entry : doc.topics().entrySet()) {
                    Integer topicId = entry.getKey();
                    Document.TopicInfo docTopicInfo = entry.getValue();
                    
                    topicInfoMap.put(topicId, new GatewayController.TopicInfo(
                        topicId,
                        docTopicInfo.weight(),
                        docTopicInfo.topWords()
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
        } catch (Exception e) {
            System.err.println("Error enriching document: " + e.getMessage());
            return null;
        }
    }

    // Extracts a display title from document metadata or uses the ID as fallback
    private String extractTitle(Document doc) {
        if (doc.metadata() != null && doc.metadata().containsKey("webTitle")) {
            return doc.metadata().get("webTitle");
        }
        return doc.id();
    }

    // Shortens long content for display purposes
    private String truncateContent(String content) {
        if (content == null) return "";
        return content.length() > 500 ? content.substring(0, 500) + "..." : content;
    }

    // Returns all topics from the topic model
    public List<Topic> getAllTopics() {
        return malletService.getAllTopics();
    }

    // Retrieves a specific topic by its identifier
    public Topic getTopicById(Integer topicId) {
        return malletService.getTopicById(topicId);
    }
}