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
import java.util.*;
import java.util.regex.Pattern;

/**
 * Service for Mallet topic modeling operations
 */
@Service
public class MalletTopicModelingService {

    @Autowired
    private TopicRepository topicRepository;

    private ParallelTopicModel topicModel;
    private static final int NUM_TOPICS = 10;
    private static final int NUM_ITERATIONS = 1000;
    private static final int NUM_TOP_WORDS = 25;

    /**
     * Train topic model on a collection of documents
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

        // Save topics to MongoDB
        saveTopicsToMongo();

        System.out.println("Topic modeling training completed!");
    }

    /**
     * Check if the topic model has been trained
     * @return true if model is trained and ready for inference
     */
    public boolean isModelTrained() {
        return topicModel != null;
    }

    /**
     * Create Mallet InstanceList from documents
     */
    private InstanceList createInstanceList(List<Document> documents) throws IOException {
        ArrayList<Pipe> pipeList = new ArrayList<>();

        // Preprocessing pipeline
        pipeList.add(new CharSequenceLowercase());
        pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
        
        // Load stopwords - Mallet 2.0.8 uses File, not InputStream
        File stopwordsFile = new ClassPathResource("stoplist_en.txt").getFile();
        pipeList.add(new TokenSequenceRemoveStopwords(stopwordsFile, "UTF-8", false, false, false));
        
        pipeList.add(new TokenSequence2FeatureSequence());

        InstanceList instances = new InstanceList(new SerialPipes(pipeList));

        // Add documents to instance list
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

    /**
     * Save extracted topics to MongoDB
     */
    private void saveTopicsToMongo() {
        topicRepository.deleteAll(); // Clear old topics

        for (int t = 0; t < NUM_TOPICS; t++) {
            List<String> topWords = new ArrayList<>();
            
            // Get top words for this topic
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

    /**
     * Get topic distribution for a document using TopicInferencer
     * This is the key method for filtering documents by topic
     */
    public Map<Integer, Double> getDocumentTopicDistribution(Document doc) throws IOException {
        if (topicModel == null) {
            throw new IllegalStateException("Topic model not trained yet");
        }

        // Create instance for this document
        ArrayList<Pipe> pipeList = new ArrayList<>();
        pipeList.add(new CharSequenceLowercase());
        pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
        
        // Load stopwords 
        File stopwordsFile = new ClassPathResource("stoplist_en.txt").getFile();
        pipeList.add(new TokenSequenceRemoveStopwords(stopwordsFile, "UTF-8", false, false, false));
        
        pipeList.add(new TokenSequence2FeatureSequence());

        InstanceList instances = new InstanceList(new SerialPipes(pipeList));
        instances.addThruPipe(new Instance(doc.content(), doc.id(), doc.id(), null));

        // Get topic distribution using TopicInferencer
        double[] topicDistribution = topicModel.getInferencer().getSampledDistribution(
            instances.get(0), 10, 1, 5
        );

        Map<Integer, Double> distribution = new HashMap<>();
        for (int i = 0; i < topicDistribution.length; i++) {
            if (topicDistribution[i] > 0.01) { // Only include topics with >1% weight
                distribution.put(i, topicDistribution[i]);
            }
        }

        return distribution;
    }

    /**
     * Get all topics from MongoDB
     */
    public List<Topic> getAllTopics() {
        return topicRepository.findAll();
    }

    /**
     * Get topic by ID
     */
    public Topic getTopicById(Integer topicId) {
        return topicRepository.findById(topicId).orElse(null);
    }
}