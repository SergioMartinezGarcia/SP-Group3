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

/*
 * Service for interacting with Elasticsearch.
 * Handles document indexing and searching operations.
 */
@Service
public class ElasticSearchService {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    // Adds a single document to the search index
    public void indexDocument(Document doc) {
        elasticsearchOperations.save(doc);
    }

    // Adds multiple documents to the search index at once
    public void bulkIndexDocuments(List<Document> docs) {
        docs.forEach(this::indexDocument);
    }

    // Searches for documents matching the query text in their content
    public List<Document> searchByContent(String queryText) {
        Criteria criteria = new Criteria("content").matches(queryText);
        Query query = new CriteriaQuery(criteria);
        
        SearchHits<Document> searchHits = elasticsearchOperations.search(query, Document.class);
        
        return searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    /*
     * Searches documents with an optional topic filter.
     * Currently simplified to just search by content.
     */
    public List<Document> searchWithTopicFilter(String queryText, Integer topicId) {
        return searchByContent(queryText);
    }

    // Retrieves a specific document by its unique identifier
    public Document getDocumentById(String id) {
        return elasticsearchOperations.get(id, Document.class);
    }
}