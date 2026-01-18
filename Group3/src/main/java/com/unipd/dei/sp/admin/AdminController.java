package com.unipd.dei.sp.admin;

import com.unipd.dei.sp.mallet.MalletTopicModelingService;
import com.unipd.dei.sp.repository.DocumentRepository;
import com.unipd.dei.sp.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/*
 * Controller for administrative operations.
 * Handles system maintenance tasks like checking status and clearing data.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private MalletTopicModelingService malletService;

    /*
     * Returns the current status of the topic model.
     * Indicates whether the model has been trained and is ready to use.
     */
    @GetMapping("/topic-filter/status")
    public ResponseEntity<String> getTopicFilterStatus() {
        boolean enabled = malletService.isModelTrained();
        
        if (enabled) {
            return ResponseEntity.ok("Topic model is TRAINED and ready");
        } else {
            return ResponseEntity.ok("Topic model is NOT TRAINED - use 'Crawl & Train Topics' to begin");
        }
    }

    /*
     * Deletes all documents and topics from the database.
     * This is a destructive operation that clears all stored data.
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