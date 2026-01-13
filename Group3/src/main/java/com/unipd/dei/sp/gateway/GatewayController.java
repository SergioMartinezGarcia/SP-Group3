package com.unipd.dei.sp.gateway;

import com.unipd.dei.sp.model.Document;
import com.unipd.dei.sp.model.Topic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Gateway Controller - Main entry point for client requests
 * Coordinates between Elasticsearch and Mallet services
 */
@RestController
@RequestMapping("/api/gateway")
public class GatewayController {

    @Autowired
    private RepresentationExtractionService representationService;

    /**
     * Search endpoint - returns documents with topic enrichment
     */
    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @RequestParam String query,
            @RequestParam(required = false) Integer topicFilter) {
        
        SearchResponse response = representationService.searchWithTopics(query, topicFilter);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all topics
     */
    @GetMapping("/topics")
    public ResponseEntity<List<Topic>> getAllTopics() {
        List<Topic> topics = representationService.getAllTopics();
        return ResponseEntity.ok(topics);
    }

    /**
     * Get specific topic details
     */
    @GetMapping("/topics/{topicId}")
    public ResponseEntity<Topic> getTopicById(@PathVariable Integer topicId) {
        Topic topic = representationService.getTopicById(topicId);
        if (topic != null) {
            return ResponseEntity.ok(topic);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Response model for search results
     */
    public static record SearchResponse(
        String query,
        List<EnrichedDocument> hits
    ) {}

    /**
     * Document enriched with topic information
     */
    public static record EnrichedDocument(
        String id,
        String title,
        String url,
        String content,
        Map<Integer, TopicInfo> topics
    ) {}

    /**
     * Topic information for a document
     */
    public static record TopicInfo(
        Integer id,
        Double weight,
        List<String> topWords
    ) {}
}
