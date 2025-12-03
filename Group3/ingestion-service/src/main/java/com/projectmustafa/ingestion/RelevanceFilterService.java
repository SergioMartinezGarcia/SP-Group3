package com.projectmustafa.ingestion;

import com.projectmustafa.model.Document;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RelevanceFilterService {

    private static final int THRESHOLD = 15;
    private static final int URL_MATCH_SCORE = 20;
    private static final int METADATA_MATCH_SCORE = 10;
    private static final int CONTENT_MATCH_SCORE = 1;

    public boolean isRelevant(Document doc, String topic) {
        int score = calculateScore(doc, topic);
        System.out.println("Relevance Score for doc " + doc.id() + ": " + score);
        return score >= THRESHOLD;
    }

    private int calculateScore(Document doc, String topic) {
        if (topic == null || topic.isEmpty()) {
            return 0;
        }

        int score = 0;
        String lowerTopic = topic.toLowerCase();

        // 1. URL Match
        if (doc.url() != null && doc.url().toLowerCase().contains(lowerTopic)) {
            score += URL_MATCH_SCORE;
        }

        // 2. Metadata Match
        if (doc.metadata() != null) {
            for (String value : doc.metadata().values()) {
                if (value != null && value.toLowerCase().contains(lowerTopic)) {
                    score += METADATA_MATCH_SCORE;
                    // Count only once per metadata map to avoid inflation
                    break;
                }
            }
        }

        // 3. Content Match (Whole Word Frequency)
        if (doc.content() != null) {
            score += countOccurrences(doc.content(), lowerTopic) * CONTENT_MATCH_SCORE;
        }

        return score;
    }

    private int countOccurrences(String content, String term) {
        // Use regex to match whole words only
        // \b represents a word boundary
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
