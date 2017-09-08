package it.polito.cinqueti.repositories;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import it.polito.cinqueti.entities.Alert;
import it.polito.cinqueti.entities.Rate;

public class AlertRepositoryImpl implements AlertRepositoryCustom {
	//@Autowired
	//MongoOperations mongoOperations;
	
	@Autowired
	AlertRepository alertRepository;

	// two phase commit
	// https://docs.mongodb.com/manual/tutorial/perform-two-phase-commits/

	@Transactional
	@Override
	public void updateUserRate(String alertId, Rate rate) {
		Alert alert = alertRepository.findById(alertId);
		alert.setRate(rate.getEmail(), rate.getValue());
		alertRepository.save(alert);
	}
	
	@Transactional
	@Override
	public void updateReferenceTs(String alertId, Date ts) {
		Alert alert = alertRepository.findById(alertId);
		if(alert == null)
			return;
		alert.setLastAccessTimestamp(ts);
		alertRepository.save(alert);
	}

}
