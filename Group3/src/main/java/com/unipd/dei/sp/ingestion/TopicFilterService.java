package com.unipd.dei.sp.ingestion;

import com.unipd.dei.sp.mallet.MalletTopicModelingService;
import com.unipd.dei.sp.model.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Service for filtering documents based on topic distribution.
 * Uses Mallet's TopicInferencer to determine which topics a document contains.
 * 
 * Filters documents to retain only those containing selected target topics.
 */
@Service
public class TopicFilterService {

    @Autowired
    private MalletTopicModelingService malletService;

    // Minimum weight threshold for a topic to be considered present
    private static final double TOPIC_WEIGHT_THRESHOLD = 0.10; // 10%

    /**
     * Check if document contains any of the specified target topics.
     * This matches the requirement: "retain only documents where a selected set of topics occur"
     * 
     * @param doc The document to check
     * @param targetTopics Set of topic IDs to filter for (e.g., {2, 5, 7})
     * @return true if document contains at least one target topic with sufficient weight
     * @throws IOException if topic inference fails
     */
    public boolean hasTargetTopics(Document doc, Set<Integer> targetTopics) throws IOException {
        // If topic model is not ready, allow all documents through
        if (!malletService.isModelTrained()) {
            return true;
        }

        // If no target topics specified, allow all documents through
        if (targetTopics == null || targetTopics.isEmpty()) {
            return true;
        }

        // Get topic distribution using TopicInferencer
        Map<Integer, Double> topicDistribution = malletService.getDocumentTopicDistribution(doc);

        // Check if document has any of the target topics with sufficient weight
        for (Integer targetTopic : targetTopics) {
            Double weight = topicDistribution.get(targetTopic);
            if (weight != null && weight >= TOPIC_WEIGHT_THRESHOLD) {
                System.out.println("Document " + doc.id() + " contains target topic " + 
                                 targetTopic + " (weight: " + String.format("%.2f", weight) + ") - PASSED");
                return true;
            }
        }

        System.out.println("Document " + doc.id() + " does not contain any target topics - FILTERED");
        return false;
    }

    /**
     * Check if topic filtering is enabled
     * @return true if the topic model is trained
     */
    public boolean isEnabled() {
        return malletService.isModelTrained();
    }
}