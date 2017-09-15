package it.polito.cinqueti.repositories;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import it.polito.cinqueti.entities.VerificationToken;

public interface VerificationTokenRepository 
	extends MongoRepository<VerificationToken, String>{

	VerificationToken findByToken(String token);
	
	@Query("{'user.email': ?0}")
	VerificationToken findByEmail(String email);
	
	void deleteByToken(String token);
	
}
