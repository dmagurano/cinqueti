package it.polito.cinqueti.repositories;

import org.springframework.data.repository.CrudRepository;

import it.polito.cinqueti.entities.BusStop;

public interface BusStopRepository extends CrudRepository<BusStop, String> {
	
	public BusStop findById(String id);

}
