package com.unipd.dei.sp.mallet;

import cc.mallet.pipe.*;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.Alphabet;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import com.unipd.dei.sp.model.Document;
import com.unipd.dei.sp.model.Topic;
import com.unipd.dei.sp.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Pattern;

/*
 * Service for topic modeling using the Mallet library.
 * Trains models to discover topics in document collections and infer topics in new documents.
 */
@Service
public class MalletTopicModelingService {

    @Autowired
    private TopicRepository topicRepository;

    private ParallelTopicModel topicModel;
    private static final int NUM_TOPICS = 10;
    private static final int NUM_ITERATIONS = 1000;
    private static final int NUM_TOP_WORDS = 25;

    /*
     * Trains the topic model on a collection of documents.
     * Extracts the most important topics and saves them to the database.
     */
    public void trainTopicModel(List<Document> documents) throws IOException {
        System.out.println("Starting Mallet topic modeling training...");

        InstanceList instances = createInstanceList(documents);
        
        topicModel = new ParallelTopicModel(NUM_TOPICS);
        topicModel.addInstances(instances);
        topicModel.setNumThreads(2);
        topicModel.setNumIterations(NUM_ITERATIONS);
        topicModel.setTopicDisplay(100, NUM_TOP_WORDS);

        topicModel.estimate();

        saveTopicsToMongo();

        System.out.println("Topic modeling training completed!");
    }

    // Checks whether the model has been trained and is ready for use
    public boolean isModelTrained() {
        return topicModel != null;
    }

    /*
     * Loads the stopwords file from classpath.
     * Handles both file system and JAR-based resources.
     */
    private File getStopwordsFile() throws IOException {
        ClassPathResource resource = new ClassPathResource("stoplist_en.txt");
        
        // Try to get as File first (works when running from IDE)
        if (resource.exists() && resource.isFile()) {
            return resource.getFile();
        }
        
        // If in JAR, extract to temporary file
        File tempFile = File.createTempFile("stoplist_en", ".txt");
        tempFile.deleteOnExit();
        
        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        
        return tempFile;
    }

    /*
     * Converts documents into Mallet's internal format.
     * Applies preprocessing like lowercasing, tokenization, and stopword removal.
     */
    private InstanceList createInstanceList(List<Document> documents) throws IOException {
        ArrayList<Pipe> pipeList = new ArrayList<>();

        // Text preprocessing pipeline
        pipeList.add(new CharSequenceLowercase());
        pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
        
        // Remove common words that don't help with topic discovery
        File stopwordsFile = getStopwordsFile();
        pipeList.add(new TokenSequenceRemoveStopwords(stopwordsFile, "UTF-8", false, false, false));
        
        pipeList.add(new TokenSequence2FeatureSequence());

        InstanceList instances = new InstanceList(new SerialPipes(pipeList));

        // Convert each document to a Mallet instance
        for (Document doc : documents) {
            instances.addThruPipe(new Instance(
                doc.content() != null ? doc.content() : "",
                doc.id(),
                doc.id(),
                null
            ));
        }

        return instances;
    }

    // Stores the discovered topics in MongoDB for later retrieval
    private void saveTopicsToMongo() {
        topicRepository.deleteAll();

        for (int t = 0; t < NUM_TOPICS; t++) {
            List<String> topWords = new ArrayList<>();
            
            // Extract the most representative words for this topic
            TreeSet<IDSorter> sortedWords = topicModel.getSortedWords().get(t);
            Alphabet alphabet = topicModel.getAlphabet();
            
            int count = 0;
            for (IDSorter idCountPair : sortedWords) {
                if (count >= NUM_TOP_WORDS) break;
                topWords.add((String) alphabet.lookupObject(idCountPair.getID()));
                count++;
            }

            Topic topic = new Topic(t, topWords, 0.0);
            topicRepository.save(topic);
        }

        System.out.println("Saved " + NUM_TOPICS + " topics to MongoDB");
    }

    /*
     * Determines the topic distribution for a single document.
     * This uses the TopicInferencer to analyze documents that weren't in the training set.
     * Returns a map of topic IDs to their weights in the document.
     */
    public Map<Integer, Double> getDocumentTopicDistribution(Document doc) throws IOException {
        if (topicModel == null) {
            throw new IllegalStateException("Topic model not trained yet");
        }

        // Prepare the document for analysis
        ArrayList<Pipe> pipeList = new ArrayList<>();
        pipeList.add(new CharSequenceLowercase());
        pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
        
        File stopwordsFile = getStopwordsFile();
        pipeList.add(new TokenSequenceRemoveStopwords(stopwordsFile, "UTF-8", false, false, false));
        
        pipeList.add(new TokenSequence2FeatureSequence());

        InstanceList instances = new InstanceList(new SerialPipes(pipeList));
        instances.addThruPipe(new Instance(doc.content(), doc.id(), doc.id(), null));

        // Infer topic proportions for this document
        double[] topicDistribution = topicModel.getInferencer().getSampledDistribution(
            instances.get(0), 10, 1, 5
        );

        Map<Integer, Double> distribution = new HashMap<>();
        for (int i = 0; i < topicDistribution.length; i++) {
            // Only include topics with meaningful presence
            if (topicDistribution[i] > 0.01) {
                distribution.put(i, topicDistribution[i]);
            }
        }

        return distribution;
    }

    // Retrieves all topics from the database
    public List<Topic> getAllTopics() {
        return topicRepository.findAll();
    }

    // Retrieves a specific topic by its identifier
    public Topic getTopicById(Integer topicId) {
        return topicRepository.findById(topicId).orElse(null);
    }
}