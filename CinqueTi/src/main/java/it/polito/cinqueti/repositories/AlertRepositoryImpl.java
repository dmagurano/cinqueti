package it.polito.cinqueti.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import it.polito.cinqueti.entities.Alert;
import it.polito.cinqueti.entities.Rate;

public class AlertRepositoryImpl implements AlertRepositoryCustom {
	//@Autowired
	//MongoOperations mongoOperations;
	
	@Autowired
	AlertRepository alertRepository;

	@Transactional
	@Override
	public void updateUserRate(String alertId, Rate rate) {
		// two phase commit
		// https://docs.mongodb.com/manual/tutorial/perform-two-phase-commits/
		
		Alert alert = alertRepository.findById(alertId);
		
		alert.setRate(rate.getEmail(), rate.getValue());
		
		alertRepository.save(alert);
	}

}
