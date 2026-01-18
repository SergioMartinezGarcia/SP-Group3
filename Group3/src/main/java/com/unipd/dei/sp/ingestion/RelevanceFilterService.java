package com.unipd.dei.sp.ingestion;

import com.unipd.dei.sp.model.Document;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Service for filtering documents by keyword relevance.
 * Uses a scoring system based on where and how often keywords appear.
 */
@Service
public class RelevanceFilterService {

    private static final int THRESHOLD = 15;
    private static final int URL_MATCH_SCORE = 20;
    private static final int METADATA_MATCH_SCORE = 10;
    private static final int CONTENT_MATCH_SCORE = 1;

    /*
     * Determines if a document is relevant to the given topic.
     * Returns true if the relevance score meets or exceeds the threshold.
     */
    public boolean isRelevant(Document doc, String topic) {
        int score = calculateScore(doc, topic);
        return score >= THRESHOLD;
    }

    /*
     * Calculates relevance score based on keyword presence in different fields.
     * URLs have highest weight, then metadata, then content occurrences.
     */
    private int calculateScore(Document doc, String topic) {
        if (topic == null || topic.isEmpty()) {
            return 0;
        }

        int score = 0;
        String lowerTopic = topic.toLowerCase();

        // Check if keyword appears in URL
        if (doc.url() != null && doc.url().toLowerCase().contains(lowerTopic)) {
            score += URL_MATCH_SCORE;
        }

        // Check if keyword appears in metadata fields
        if (doc.metadata() != null) {
            for (String value : doc.metadata().values()) {
                if (value != null && value.toLowerCase().contains(lowerTopic)) {
                    score += METADATA_MATCH_SCORE;
                    break;
                }
            }
        }

        // Count keyword occurrences in content
        if (doc.content() != null) {
            score += countOccurrences(doc.content(), lowerTopic) * CONTENT_MATCH_SCORE;
        }

        return score;
    }

    // Counts how many times a term appears as a complete word in the content
    private int countOccurrences(String content, String term) {
        String regex = "\\b" + Pattern.quote(term) + "\\b";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);

        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}