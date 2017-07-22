package it.polito.cinqueti.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import it.polito.cinqueti.entities.MinPath;

public interface PathRepository extends MongoRepository<MinPath, String> {

}
