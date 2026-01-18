package com.unipd.dei.sp.ingestion;

import com.unipd.dei.sp.model.Document;
import java.util.List;

/*
 * Interface for document crawling services.
 * Defines the contract for retrieving documents from external sources.
 */
public interface CrawlerService {
    // Fetches documents related to the specified topic
    List<Document> crawl(String topic);
}