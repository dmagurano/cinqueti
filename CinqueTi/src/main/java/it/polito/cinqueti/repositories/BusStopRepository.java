package it.polito.cinqueti.repositories;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import it.polito.cinqueti.entities.BusStop;

@Repository
public interface BusStopRepository extends CrudRepository<BusStop, String> {
	
	public BusStop findById(String id);
	public List<BusStop> findAll();

}
