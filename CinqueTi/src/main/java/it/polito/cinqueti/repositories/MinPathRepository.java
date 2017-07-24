package it.polito.cinqueti.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import it.polito.cinqueti.entities.MinPath;


public interface MinPathRepository extends MongoRepository<MinPath, String>, MinPathRepositoryCustom {

}

