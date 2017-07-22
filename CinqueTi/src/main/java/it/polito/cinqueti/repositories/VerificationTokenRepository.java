package it.polito.cinqueti.repositories;


import org.springframework.data.mongodb.repository.MongoRepository;

import it.polito.cinqueti.entities.User;
import it.polito.cinqueti.entities.VerificationToken;

public interface VerificationTokenRepository 
	extends MongoRepository<VerificationToken, Long>{

	VerificationToken findByToken(String token);
	
	VerificationToken findByUser(User user);
	
	void deleteByToken(String token);
	
}
