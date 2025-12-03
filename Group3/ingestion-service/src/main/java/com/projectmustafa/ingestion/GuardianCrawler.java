package com.projectmustafa.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectmustafa.model.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GuardianCrawler implements CrawlerService {

    @Value("${guardian.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<Document> crawl(String topic) {
        List<Document> documents = new ArrayList<>();
        // URL asks for bodyText so we have content to analyze
        String url = "https://content.guardianapis.com/search?show-fields=bodyText&page-size=10&api-key=" + apiKey
                + "&q=" + topic;

        try {
            System.out.println("Crawling: " + url);
            // 1. Get raw JSON string
            String jsonResponse = restTemplate.getForObject(url, String.class);

            // 2. Parse JSON Tree
            JsonNode root = mapper.readTree(jsonResponse);
            JsonNode results = root.path("response").path("results");

            // 3. Loop through results and create Document objects
            if (results.isArray()) {
                for (JsonNode node : results) {
                    String bodyText = node.path("fields").path("bodyText").asText();

                    // Skip empty articles
                    if (bodyText == null || bodyText.isEmpty())
                        continue;

                    Document doc = new Document(
                            node.path("id").asText(), // ID
                            node.path("webUrl").asText(), // URL
                            bodyText, // Content
                            "The Guardian", // Source
                            Instant.now(), // Timestamp
                            Map.of("section", node.path("sectionName").asText()) // Metadata
                    );
                    documents.add(doc);
                }
            }
        } catch (Exception e) {
            System.err.println("Error crawling Guardian: " + e.getMessage());
        }
        return documents;
    }
}
