package com.unipd.dei.sp.admin;

import com.unipd.dei.sp.indexing.IndexingService;
import com.unipd.dei.sp.ingestion.TopicFilterService;
import com.unipd.dei.sp.repository.DocumentRepository;
import com.unipd.dei.sp.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin Controller for maintenance operations
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private IndexingService indexingService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private TopicFilterService topicFilterService;

    /**
     * Trigger full re-indexing and topic model training
     * Should be called after initial documents are ingested into MongoDB
     * 
     * After this, topic filtering will be automatically enabled for future ingestion
     */
    @PostMapping("/reindex")
    public ResponseEntity<String> triggerReindexing() {
        try {
            indexingService.indexAllDocuments();
            
            String message = "Reindexing and topic modeling completed successfully. ";
            if (topicFilterService.isEnabled()) {
                message += "Topic filtering is now ENABLED for future document ingestion.";
            }
            
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error during reindexing: " + e.getMessage());
        }
    }

    /**
     * Check if topic filtering is currently enabled
     */
    @GetMapping("/topic-filter/status")
    public ResponseEntity<String> getTopicFilterStatus() {
        boolean enabled = topicFilterService.isEnabled();
        
        if (enabled) {
            return ResponseEntity.ok("Topic filtering is ENABLED (model trained)");
        } else {
            return ResponseEntity.ok("Topic filtering is DISABLED (model not trained - run /api/admin/reindex first)");
        }
    }

    /**
     * Delete all documents from MongoDB
     */
    @DeleteMapping("/documents")
    public ResponseEntity<String> deleteAllDocuments() {
        try {
            long count = documentRepository.count();
            documentRepository.deleteAll();
            return ResponseEntity.ok("Deleted " + count + " documents from MongoDB");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error deleting documents: " + e.getMessage());
        }
    }

    /**
     * Delete all topics from MongoDB
     */
    @DeleteMapping("/topics")
    public ResponseEntity<String> deleteAllTopics() {
        try {
            long count = topicRepository.count();
            topicRepository.deleteAll();
            return ResponseEntity.ok("Deleted " + count + " topics from MongoDB");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error deleting topics: " + e.getMessage());
        }
    }

    /**
     * Delete all data (documents and topics) from MongoDB
     */
    @DeleteMapping("/all")
    public ResponseEntity<String> deleteAllData() {
        try {
            long docCount = documentRepository.count();
            long topicCount = topicRepository.count();
            
            documentRepository.deleteAll();
            topicRepository.deleteAll();
            
            return ResponseEntity.ok("Deleted " + docCount + " documents and " + topicCount + " topics from MongoDB");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error deleting all data: " + e.getMessage());
        }
    }
}