package com.unipd.dei.sp.ingestion;

import com.unipd.dei.sp.model.Document;
import java.util.List;

public interface CrawlerService {
    List<Document> crawl(String topic);
}
