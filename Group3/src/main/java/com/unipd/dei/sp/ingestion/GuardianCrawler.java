package com.unipd.dei.sp.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unipd.dei.sp.model.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @class GuardianCrawler
 * @brief Crawler implementation that fetches articles from The Guardian API.
 *
 * This class implements the {@link CrawlerService} interface
 * and provides functionality to query The Guardian's content API using
 * a provided API key. Results are parsed from JSON into {@link Document}
 * objects containing text content, metadata, and source information.
 */
@Service
public class GuardianCrawler implements CrawlerService {

	@Value("${guardian.api.key}")
	private String apiKey;

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper mapper = new ObjectMapper();

    /**
     * @brief Crawls The Guardian API for articles related to a given topic.
     *
     * This method:
     * - Builds a query URL based on the given topic.
     * - Sends an HTTP GET request to The Guardian API.
     * - Parses the JSON response.
     * - Extracts article fields such as body text and metadata.
     * - Wraps each article in a {@link Document} instance.
     *
     * Articles that do not contain textual body content are skipped.
     *
     * @param topic The search term (keyword) used to filter articles.
     * @return A list of {@link Document} objects representing retrieved articles.
     *
     * @throws RuntimeException If an unexpected error occurs while processing
     * or parsing the API response.
     */
	@Override
	public List<Document> crawl(String topic) {
		List<Document> documents = new ArrayList<>();
		String url = "https://content.guardianapis.com/search?show-fields=bodyText&page-size=10&api-key=" + apiKey
				+ "&q=" + topic;

		try {
			System.out.println("Crawling: " + url);
			// Get raw JSON string
			String jsonResponse = restTemplate.getForObject(url, String.class);

			// Parse JSON Tree
			JsonNode root = mapper.readTree(jsonResponse);
			JsonNode results = root.path("response").path("results");

			// Loop through results and create Document objects
			if (results.isArray()) {
				for (JsonNode node : results) {
					String bodyText = node.path("fields").path("bodyText").asText();

					// Skip empty articles
					if (bodyText == null || bodyText.isEmpty())
						continue;

					Document doc = new Document(node.path("id").asText(), node.path("webUrl").asText(), bodyText,
							"The Guardian", Instant.now(), Map.of("section", node.path("sectionName").asText()));
					documents.add(doc);
				}
			}
		} catch (Exception e) {
			System.err.println("Error crawling Guardian: " + e.getMessage());
		}
		return documents;
	}
}
