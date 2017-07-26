package it.polito.cinqueti.services;

import java.util.List;

import it.polito.cinqueti.entities.BusStop;

public interface BusStopService {

	public BusStop findById(String id);
	List<BusStop> findAll();
}
