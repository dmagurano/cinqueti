package it.polito.cinqueti.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import it.polito.cinqueti.entities.User;

public interface UserRepository extends MongoRepository<User, String> {

	public User findById(String userId);
	
	public User findByEmail(String email);
	
	public User findByNickname(String nickname);
	
	public List<User> findAll();
	
	public Page<User> findAll(Pageable p);
	
}
