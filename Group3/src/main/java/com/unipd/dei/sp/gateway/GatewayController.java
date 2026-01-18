package com.unipd.dei.sp.gateway;

import com.unipd.dei.sp.model.Document;
import com.unipd.dei.sp.model.Topic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/*
 * Main API controller for client requests.
 * Handles search queries and topic retrieval with data enrichment.
 */
@RestController
@RequestMapping("/api/gateway")
public class GatewayController {

    @Autowired
    private RepresentationExtractionService representationService;

    /*
     * Searches for documents and returns results with topic information.
     * Can optionally filter results by a specific topic.
     */
    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @RequestParam String query,
            @RequestParam(required = false) Integer topicFilter) {
        
        SearchResponse response = representationService.searchWithTopics(query, topicFilter);
        return ResponseEntity.ok(response);
    }

    // Returns the complete list of available topics
    @GetMapping("/topics")
    public ResponseEntity<List<Topic>> getAllTopics() {
        List<Topic> topics = representationService.getAllTopics();
        return ResponseEntity.ok(topics);
    }

    // Retrieves detailed information about a specific topic
    @GetMapping("/topics/{topicId}")
    public ResponseEntity<Topic> getTopicById(@PathVariable Integer topicId) {
        Topic topic = representationService.getTopicById(topicId);
        if (topic != null) {
            return ResponseEntity.ok(topic);
        }
        return ResponseEntity.notFound().build();
    }

    // Contains the search query and matching documents
    public static record SearchResponse(
        String query,
        List<EnrichedDocument> hits
    ) {}

    // Document with additional topic information attached
    public static record EnrichedDocument(
        String id,
        String title,
        String url,
        String content,
        Map<Integer, TopicInfo> topics
    ) {}

    // Information about a topic within a document
    public static record TopicInfo(
        Integer id,
        Double weight,
        List<String> topWords
    ) {}
}