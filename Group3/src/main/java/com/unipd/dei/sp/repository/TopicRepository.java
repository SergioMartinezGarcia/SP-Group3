package com.unipd.dei.sp.repository;

import com.unipd.dei.sp.model.Topic;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/*
 * Repository for accessing Topic entities in MongoDB.
 * Handles storage and retrieval of discovered topics.
 */
@Repository
public interface TopicRepository extends MongoRepository<Topic, Integer> {
}