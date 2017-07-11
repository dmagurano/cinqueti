package it.polito.cinqueti.repositories;



import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import it.polito.cinqueti.entities.Message;

public interface MessageRepository extends MongoRepository<Message, String> {
	
	public Page<Message> findByTopic(String topic, Pageable pageable);
	public List<Message> findTop10ByTopicOrderByTimestampDesc(String topic);

}
