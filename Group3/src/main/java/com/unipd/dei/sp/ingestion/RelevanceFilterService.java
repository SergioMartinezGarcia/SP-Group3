package com.unipd.dei.sp.ingestion;

import com.unipd.dei.sp.model.Document;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * @class RelevanceFilterService
 * @brief Provides logic to assess the relevance of documents to a specific topic.
 *
 * Relevance scoring is based on:
 * - URL containing the topic keyword
 * - Metadata fields containing the topic keyword
 * - Occurrences of the topic keyword in the document content
 *
 * The service defines fixed weights for each type of match and a
 * threshold above which a document is considered relevant.
 */
@Service
public class RelevanceFilterService {

    private static final int THRESHOLD = 15;
    private static final int URL_MATCH_SCORE = 20;
    private static final int METADATA_MATCH_SCORE = 10;
    private static final int CONTENT_MATCH_SCORE = 1;

    /**
     * @brief Determines if a document is relevant to the given topic.
     *
     * Calls {@link #calculateScore(Document, String)} to compute the relevance score
     * and compares it to the threshold.
     *
     * @param doc The {@link Document} to evaluate.
     * @param topic The topic keyword used to assess relevance.
     * @return true if the document score is greater than or equal to {@link #THRESHOLD}, false otherwise.
     */
    public boolean isRelevant(Document doc, String topic) {
        int score = calculateScore(doc, topic);
        System.out.println("Relevance Score for doc " + doc.id() + ": " + score);
        return score >= THRESHOLD;
    }

    /**
     * @brief Calculates the relevance score of a document based on URL, metadata, and content.
     *
     * Scoring logic:
     * - URL contains topic → +{@link #URL_MATCH_SCORE}
     * - Any metadata field contains topic → +{@link #METADATA_MATCH_SCORE} (only once)
     * - Each occurrence of topic in content → +{@link #CONTENT_MATCH_SCORE}
     *
     * @param doc The {@link Document} to score.
     * @param topic The topic keyword.
     * @return The computed relevance score as an integer.
     */
    private int calculateScore(Document doc, String topic) {
        if (topic == null || topic.isEmpty()) {
            return 0;
        }

        int score = 0;
        String lowerTopic = topic.toLowerCase();

        // URL Match
        if (doc.url() != null && doc.url().toLowerCase().contains(lowerTopic)) {
            score += URL_MATCH_SCORE;
        }

        // Metadata Match
        if (doc.metadata() != null) {
            for (String value : doc.metadata().values()) {
                if (value != null && value.toLowerCase().contains(lowerTopic)) {
                    score += METADATA_MATCH_SCORE;
                    // Count only once per metadata map to avoid inflation
                    break;
                }
            }
        }

        // Content Match
        if (doc.content() != null) {
            score += countOccurrences(doc.content(), lowerTopic) * CONTENT_MATCH_SCORE;
        }

        return score;
    }

    /**
     * @brief Counts the occurrences of a term in the content using whole-word matching.
     *
     * Uses a regular expression with word boundaries (\b) to match only full words.
     *
     * @param content The text content to search in.
     * @param term The term to count.
     * @return The number of occurrences of the term in the content.
     */
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
