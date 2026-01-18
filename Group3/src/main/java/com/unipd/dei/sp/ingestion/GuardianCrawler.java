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

@Service
public class GuardianCrawler implements CrawlerService {

	@Value("${guardian.api.key}")
	private String apiKey;

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper mapper = new ObjectMapper();

	@Override
	public List<Document> crawl(String topic) {
		List<Document> documents = new ArrayList<>();
		String url = "https://content.guardianapis.com/search?show-fields=bodyText&page-size=50&api-key=" + apiKey
				+ "&q=" + topic;

		try {
			String jsonResponse = restTemplate.getForObject(url, String.class);
			JsonNode root = mapper.readTree(jsonResponse);
			JsonNode results = root.path("response").path("results");

			if (results.isArray()) {
				for (JsonNode node : results) {
					String bodyText = node.path("fields").path("bodyText").asText();

					if (bodyText == null || bodyText.isEmpty()) {
						continue;
					}

					Document doc = new Document(
							node.path("id").asText(), 
							node.path("webUrl").asText(), 
							bodyText,
							"The Guardian", 
							String.valueOf(Instant.now().toEpochMilli()),
							Map.of("section", node.path("sectionName").asText(),
							       "webTitle", node.path("webTitle").asText())
					);
					documents.add(doc);
				}
			}
		} catch (Exception e) {
			System.err.println("Error crawling Guardian API: " + e.getMessage());
		}
		return documents;
	}
}