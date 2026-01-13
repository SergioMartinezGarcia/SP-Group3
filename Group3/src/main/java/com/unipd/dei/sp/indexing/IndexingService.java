package com.unipd.dei.sp.indexing;

import com.unipd.dei.sp.elasticsearch.ElasticSearchService;
import com.unipd.dei.sp.mallet.MalletTopicModelingService;
import com.unipd.dei.sp.model.Document;
import com.unipd.dei.sp.model.Topic;
import com.unipd.dei.sp.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Service that orchestrates indexing documents into Elasticsearch
 * and training the Mallet topic model.
 * 
 * After training, documents are enriched with topic information
 * and re-saved to MongoDB before indexing to Elasticsearch.
 */
@Service
public class IndexingService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ElasticSearchService elasticsearchService;

    @Autowired
    private MalletTopicModelingService malletService;

    /**
     * Index all documents from MongoDB into Elasticsearch and train topic model.
     * 
     * Process:
     * 1. Read all documents from MongoDB
     * 2. Train topic model on these documents
     * 3. Enrich documents with topic information
     * 4. Re-save enriched documents to MongoDB
     * 5. Index enriched documents to Elasticsearch
     */
    public void indexAllDocuments() {
        System.out.println("Starting full indexing pipeline...");

        // Get all documents from MongoDB
        List<Document> documents = documentRepository.findAll();
        System.out.println("Found " + documents.size() + " documents in MongoDB");

        if (documents.isEmpty()) {
            System.out.println("No documents to index");
            return;
        }

        // Train topic model on existing documents
        try {
            System.out.println("Training Mallet topic model...");
            malletService.trainTopicModel(documents);
            System.out.println("Topic model training complete");
        } catch (IOException e) {
            System.err.println("Error training topic model: " + e.getMessage());
            e.printStackTrace();
            return; // Cannot proceed without trained model
        }

        // Enrich documents with topic information
        System.out.println("Enriching documents with topic information...");
        List<Document> enrichedDocuments = new ArrayList<>();
        
        for (Document doc : documents) {
            try {
                Document enriched = enrichDocumentWithTopics(doc);
                enrichedDocuments.add(enriched);
                
                // Update MongoDB with enriched version
                documentRepository.save(enriched);
                
            } catch (Exception e) {
                System.err.println("Error enriching document " + doc.id() + ": " + e.getMessage());
                enrichedDocuments.add(doc); // Use original if enrichment fails
            }
        }
        
        System.out.println("Document enrichment complete");

        // Index enriched documents into Elasticsearch
        System.out.println("Indexing documents into Elasticsearch...");
        elasticsearchService.bulkIndexDocuments(enrichedDocuments);
        System.out.println("Elasticsearch indexing complete");

        System.out.println("Full indexing pipeline completed!");
    }

    /**
     * Index a single document
     */
    public void indexDocument(Document doc) {
        // Enrich before indexing if model is trained
        Document enriched = malletService.isModelTrained() ? 
            enrichDocumentWithTopics(doc) : doc;
        
        elasticsearchService.indexDocument(enriched);
    }

    /**
     * Enrich document with topic information.
     * 
     * @param doc The document to enrich
     * @return Enriched document with topic information
     */
    private Document enrichDocumentWithTopics(Document doc) {
        if (!malletService.isModelTrained()) {
            return doc;
        }
        
        try {
            // Get topic distribution
            Map<Integer, Double> topicDist = malletService.getDocumentTopicDistribution(doc);
            
            // Build topic info map
            Map<Integer, Document.TopicInfo> topicInfoMap = new HashMap<>();
            
            for (Map.Entry<Integer, Double> entry : topicDist.entrySet()) {
                int topicId = entry.getKey();
                double weight = entry.getValue();
                
                // Get top words for this topic
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
                topicInfoMap
            );
            
        } catch (IOException e) {
            System.err.println("Failed to enrich document: " + e.getMessage());
            return doc;
        }
    }
}