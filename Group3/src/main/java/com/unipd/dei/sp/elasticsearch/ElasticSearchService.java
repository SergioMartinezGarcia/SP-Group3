package com.unipd.dei.sp.elasticsearch;

import com.unipd.dei.sp.model.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Elasticsearch operations
 */
@Service
public class ElasticSearchService {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    /**
     * Index a document in Elasticsearch
     */
    public void indexDocument(Document doc) {
        elasticsearchOperations.save(doc);
    }

    /**
     * Bulk index multiple documents
     */
    public void bulkIndexDocuments(List<Document> docs) {
        docs.forEach(this::indexDocument);
    }

    /**
     * Search documents by query text in content field
     */
    public List<Document> searchByContent(String queryText) {
        Criteria criteria = new Criteria("content").matches(queryText);
        Query query = new CriteriaQuery(criteria);
        
        SearchHits<Document> searchHits = elasticsearchOperations.search(query, Document.class);
        
        return searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    /**
     * Search documents with topic filter
     */
    public List<Document> searchWithTopicFilter(String queryText, Integer topicId) {
        // This would require nested query - simplified for now
        return searchByContent(queryText);
    }

    /**
     * Get document by ID
     */
    public Document getDocumentById(String id) {
        return elasticsearchOperations.get(id, Document.class);
    }
}