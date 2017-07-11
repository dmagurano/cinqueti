package it.polito.cinqueti.repositories;



import org.springframework.data.mongodb.repository.MongoRepository;
import it.polito.cinqueti.entities.Alert;

public interface AlertRepository extends MongoRepository<Alert, String>, AlertRepositoryCustom {

	public Alert findById(String alertId);
	//public void findAndModify(Query query, Update args, FindAndModifyOptions opts);
	//public Page<Message> findByTopic(String topic, Pageable pageable);
	//public List<Message> findTop10ByTopicOrderByTimestampDesc(String topic);

}
