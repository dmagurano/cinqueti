package it.polito.cinqueti.repositories;



import org.springframework.data.mongodb.repository.MongoRepository;
import it.polito.cinqueti.entities.Alert;

public interface AlertRepository extends MongoRepository<Alert, String>, AlertRepositoryCustom {

	public Alert findById(String alertId);
	public Long deleteAlertById(String alertId);

}
