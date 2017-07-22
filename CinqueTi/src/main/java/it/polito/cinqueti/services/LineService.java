package it.polito.cinqueti.services;

import java.util.List;

import it.polito.cinqueti.entities.BusLine;

public interface LineService {

	List<BusLine> findAll();
	BusLine findOne(String id);
}
