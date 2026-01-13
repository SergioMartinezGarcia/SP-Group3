package com.unipd.dei.sp.repository;

import com.unipd.dei.sp.model.Topic;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Topic entities in MongoDB
 */
@Repository
public interface TopicRepository extends MongoRepository<Topic, Integer> {
}