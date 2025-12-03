package com.projectmustafa.ingestion;

import com.projectmustafa.model.Document;
import java.util.List;

public interface CrawlerService {
    // Downloads data based on a topic keyword
    List<Document> crawl(String topic);
}
