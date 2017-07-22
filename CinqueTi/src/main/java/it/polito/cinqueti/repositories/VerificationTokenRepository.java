package it.polito.cinqueti.repositories;


import org.springframework.data.mongodb.repository.MongoRepository;

import it.polito.cinqueti.entities.VerificationToken;

public interface VerificationTokenRepository 
	extends MongoRepository<VerificationToken, Long>{

	VerificationToken findByToken(String token);
	
	//VerificationToken findByUserId(String UserId);
	
	void deleteByToken(String token);
	
}
